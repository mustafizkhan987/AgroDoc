"""Train, calibrate, evaluate, and export AgroDoc's dual-output TFLite model.

Requires a reviewed CSV manifest from prepare_data.py and TensorFlow/Keras in
an approved GPU environment. This module does not download datasets.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

import numpy as np

from evaluation import (
    calibrate_reject_rules,
    calibrate_temperature,
    evaluate_logits,
    evaluate_open_set,
    evaluate_reject_thresholds,
    restrict_to_crop,
)
from prepare_data import CLASSES_PATH, load_classes, validate_groups

ROOT = Path(__file__).resolve().parents[1]
MANIFEST_FIELDS = {
    "path", "dataset_id", "source_label", "class_id", "crop", "disease_id",
    "split", "group_id", "sha256",
}
IMAGE_SIZE = 224
FEATURE_CHANNELS = 1280
OPEN_SET_ID = 23


def load_manifest(path: Path, classes: list[dict[str, Any]]) -> list[dict[str, Any]]:
    if not path.is_file():
        raise FileNotFoundError(f"manifest not found: {path}")
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        missing = MANIFEST_FIELDS - set(reader.fieldnames or [])
        if missing:
            raise ValueError(f"manifest is missing columns: {', '.join(sorted(missing))}")
        rows = list(reader)
    if not rows:
        raise ValueError("manifest contains no image rows")
    by_id = {int(row["class_id"]): row for row in classes}
    for row in rows:
        row["class_id"] = int(row["class_id"])
        row["path"] = str(Path(row["path"]).resolve())
        if row["class_id"] not in by_id:
            raise ValueError(f"manifest contains unconfigured class_id {row['class_id']}")
        if not Path(row["path"]).is_file():
            raise FileNotFoundError(f"manifest image missing: {row['path']}")
        class_row = by_id[row["class_id"]]
        if row["disease_id"] != class_row["disease_id"] or row["crop"] != class_row["crop"]:
            raise ValueError(f"manifest metadata does not match classes.yaml for {row['path']}")
        if row["split"] not in {"train", "validation", "test"}:
            raise ValueError(f"invalid split {row['split']!r} for {row['path']}")
    validate_groups(rows)
    present = {row["class_id"] for row in rows if row["split"] == "train"}
    expected = set(by_id)
    missing_train = sorted(expected - present)
    if missing_train:
        raise ValueError(
            "training split lacks configured classes " + str(missing_train)
            + "; include a reviewed not-a-leaf dataset and every selected class"
        )
    for split in ("validation", "test"):
        if not any(row["split"] == split for row in rows):
            raise ValueError(f"manifest has no {split} examples")
    return rows


def require_tensorflow():
    try:
        import tensorflow as tf
    except ImportError as error:
        raise RuntimeError(
            "TensorFlow is not installed. Run this training script in the approved GPU environment; no model was trained."
        ) from error
    return tf


def _gaussian_blur(tf, image):
    kernel = tf.constant([[1, 2, 1], [2, 4, 2], [1, 2, 1]], dtype=tf.float32) / 16.0
    kernel = tf.reshape(kernel, [3, 3, 1, 1])
    kernel = tf.tile(kernel, [1, 1, 3, 1])
    expanded = tf.expand_dims(image, 0)
    blurred = tf.nn.depthwise_conv2d(expanded, kernel, strides=[1, 1, 1, 1], padding="SAME")[0]
    return tf.cond(tf.random.uniform([]) < 0.15, lambda: blurred, lambda: image)


def make_dataset(tf, rows: list[dict[str, Any]], batch_size: int, training: bool):
    paths = [row["path"] for row in rows]
    labels = np.asarray([row["class_id"] for row in rows], dtype=np.int32)
    dataset = tf.data.Dataset.from_tensor_slices((paths, labels))
    random_zoom = tf.keras.layers.RandomZoom((-0.15, 0.15)) if training else None
    if training:
        dataset = dataset.shuffle(len(rows), reshuffle_each_iteration=True)

    def decode(path, label):
        raw = tf.io.read_file(path)
        image = tf.io.decode_image(raw, channels=3, expand_animations=False)
        image.set_shape([None, None, 3])
        image = tf.image.resize(image, [IMAGE_SIZE, IMAGE_SIZE], antialias=True)
        image = tf.cast(image, tf.float32)
        if training:
            image = tf.image.random_flip_left_right(image)
            image = tf.image.random_flip_up_down(image)
            image = tf.image.random_brightness(image, max_delta=36.0)
            image = tf.image.random_contrast(image, lower=0.75, upper=1.25)
            image = _gaussian_blur(tf, image)
            # Crop/zoom augmentation is applied only after train groups are fixed.
            image = random_zoom(image, training=True)
        image = tf.clip_by_value(image, 0.0, 255.0)
        return image, label

    return dataset.map(decode, num_parallel_calls=tf.data.AUTOTUNE).batch(batch_size).prefetch(tf.data.AUTOTUNE)


def build_model(tf, backbone_name: str, number_of_classes: int):
    inputs = tf.keras.Input(shape=(IMAGE_SIZE, IMAGE_SIZE, 3), name="input_pixels")
    if backbone_name == "mobilenetv2":
        base = tf.keras.applications.MobileNetV2(
            include_top=False, weights="imagenet", input_shape=(IMAGE_SIZE, IMAGE_SIZE, 3)
        )
        prepared = tf.keras.applications.mobilenet_v2.preprocess_input(inputs)
        raw_features = base(prepared)
    elif backbone_name == "mobilenetv3large":
        base = tf.keras.applications.MobileNetV3Large(
            include_top=False, weights="imagenet", input_shape=(IMAGE_SIZE, IMAGE_SIZE, 3),
            include_preprocessing=True,
        )
        raw_features = base(inputs)
    elif backbone_name == "efficientnet_lite0":
        raise NotImplementedError(
            "EfficientNet-Lite0's Keras training and 7x7x1280 feature-map adapter is not implemented. "
            "Do not substitute EfficientNet-B0; the Phase 1 bake-off is incomplete."
        )
    else:
        raise ValueError(f"unsupported backbone: {backbone_name}")

    # Project candidate backbone features to the Android v1 CAM shape.
    # MobileNetV2 is already 1280 channels; MobileNetV3-Large is projected.
    channels = raw_features.shape[-1]
    if channels != FEATURE_CHANNELS:
        features = tf.keras.layers.Conv2D(
            FEATURE_CHANNELS, 1, padding="same", use_bias=False, name="cam_projection"
        )(raw_features)
    else:
        features = tf.keras.layers.Activation("linear", name="cam_projection")(raw_features)
    pooled = tf.keras.layers.GlobalAveragePooling2D(name="global_pooling")(features)
    logits = tf.keras.layers.Dense(number_of_classes, name="logits", use_bias=False)(pooled)
    training_model = tf.keras.Model(inputs, logits, name=f"agrodoc_{backbone_name}_train")
    export_model = tf.keras.Model(inputs, [logits, features], name=f"agrodoc_{backbone_name}_export")
    return training_model, export_model, base


def compile_model(tf, model, learning_rate: float):
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate),
        loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
        metrics=[tf.keras.metrics.SparseCategoricalAccuracy(name="accuracy")],
    )


def _rows_for(rows, split, dataset_id=None):
    return [row for row in rows if row["split"] == split and (dataset_id is None or row["dataset_id"] == dataset_id)]


def _crop_filter(logits: np.ndarray, rows: list[dict[str, Any]], classes: list[dict[str, Any]]) -> np.ndarray:
    restricted = np.empty_like(logits)
    for index, row in enumerate(rows):
        restricted[index:index + 1] = restrict_to_crop(
            logits[index:index + 1], classes, row["crop"], open_set_id=OPEN_SET_ID
        )
    return restricted


def predict_rows(tf, model, rows, batch_size, tta_flip=False):
    dataset = make_dataset(tf, rows, batch_size, training=False).map(lambda image, label: image)
    logits_parts = []
    feature_parts = []
    for images in dataset:
        outputs = model(images, training=False)
        logits, features = outputs
        if tta_flip:
            flipped = model(tf.image.flip_left_right(images), training=False)[0]
            logits = (logits + flipped) / 2.0
        logits_parts.append(logits.numpy())
        feature_parts.append(features.numpy())
    return np.concatenate(logits_parts), np.concatenate(feature_parts)


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def dataset_fingerprints(rows):
    grouped = defaultdict(list)
    for row in rows:
        grouped[row["dataset_id"]].append(row["sha256"])
    return {
        name: hashlib.sha256("\n".join(sorted(set(hashes))).encode("utf-8")).hexdigest()
        for name, hashes in sorted(grouped.items())
    }


def verify_tflite_parity(tf, model, image_path: str, tflite_path: Path) -> dict[str, float]:
    image_bytes = tf.io.read_file(image_path)
    image = tf.image.resize(tf.io.decode_image(image_bytes, channels=3, expand_animations=False), [IMAGE_SIZE, IMAGE_SIZE])
    sample = tf.expand_dims(tf.cast(image, tf.float32), 0)
    keras_logits, keras_features = model(sample, training=False)

    interpreter = tf.lite.Interpreter(model_path=str(tflite_path))
    interpreter.allocate_tensors()
    input_detail = interpreter.get_input_details()[0]
    outputs = interpreter.get_output_details()
    interpreter.set_tensor(input_detail["index"], sample.numpy().astype(input_detail["dtype"]))
    interpreter.invoke()
    output_tensors = [(detail, interpreter.get_tensor(detail["index"])) for detail in outputs]
    logits_tensor = next(value for detail, value in output_tensors if len(detail["shape"]) == 2)
    features_tensor = next(value for detail, value in output_tensors if len(detail["shape"]) == 4)
    logits_error = float(np.max(np.abs(keras_logits.numpy() - logits_tensor)))
    features_error = float(np.max(np.abs(keras_features.numpy() - features_tensor)))
    if not np.allclose(keras_logits.numpy(), logits_tensor, rtol=1e-4, atol=1e-4):
        raise RuntimeError(f"TFLite/Keras logits differ (max abs error {logits_error})")
    if not np.allclose(keras_features.numpy(), features_tensor, rtol=1e-4, atol=1e-4):
        raise RuntimeError(f"TFLite/Keras feature maps differ (max abs error {features_error})")
    return {"logits_max_abs_error": logits_error, "features_max_abs_error": features_error}


def train_one_backbone(args):
    tf = require_tensorflow()
    classes, _ = load_classes(args.classes)
    classes = sorted(classes, key=lambda row: int(row["class_id"]))
    rows = load_manifest(args.manifest, classes)
    output_root = args.reports_dir
    output_root.mkdir(parents=True, exist_ok=True)
    train_rows = _rows_for(rows, "train")
    validation_rows = _rows_for(rows, "validation")
    train_ds = make_dataset(tf, train_rows, args.batch_size, training=True)
    validation_ds = make_dataset(tf, validation_rows, args.batch_size, training=False)

    train_model, export_model, base = build_model(tf, args.backbone, len(classes))
    compile_model(tf, train_model, args.learning_rate)
    train_model.fit(train_ds, validation_data=validation_ds, epochs=args.epochs_head)

    base.trainable = True
    cutoff = max(0, len(base.layers) - args.finetune_layers)
    for layer in base.layers[:cutoff]:
        layer.trainable = False
    # Keep batch-normalization statistics stable during small-dataset fine tuning.
    for layer in base.layers:
        if isinstance(layer, tf.keras.layers.BatchNormalization):
            layer.trainable = False
    compile_model(tf, train_model, args.fine_learning_rate)
    train_model.fit(train_ds, validation_data=validation_ds, epochs=args.epochs_fine)

    val_raw_logits, _ = predict_rows(tf, export_model, validation_rows, args.batch_size, args.tta_flip)
    val_y = np.asarray([row["class_id"] for row in validation_rows], dtype=np.int64)
    known_mask = val_y != OPEN_SET_ID
    unknown_mask = val_y == OPEN_SET_ID
    if not known_mask.any() or not unknown_mask.any():
        raise ValueError("validation split must contain both known leaves and reviewed not-a-leaf examples")
    temperature = calibrate_temperature(val_raw_logits[known_mask, :OPEN_SET_ID], val_y[known_mask])
    open_set_thresholds = calibrate_reject_rules(
        val_raw_logits[known_mask, :OPEN_SET_ID] / temperature,
        val_raw_logits[unknown_mask, :OPEN_SET_ID] / temperature,
    )

    class_names = [row["source_label"] for row in classes]
    all_ids = [int(row["class_id"]) for row in classes]
    dataset_metrics: dict[str, Any] = {}
    test_outputs: dict[str, tuple[list[dict[str, Any]], np.ndarray]] = {}
    test_datasets = sorted({row["dataset_id"] for row in rows if row["split"] == "test"})
    for dataset_id in test_datasets:
        subset = _rows_for(rows, "test", dataset_id)
        raw_logits, _ = predict_rows(tf, export_model, subset, args.batch_size, args.tta_flip)
        test_outputs[dataset_id] = (subset, raw_logits)
        logits = _crop_filter(raw_logits / temperature, subset, classes)
        y = np.asarray([row["class_id"] for row in subset], dtype=np.int64)
        dataset_metrics[dataset_id] = evaluate_logits(y, logits, all_ids, class_names)
        test_known = y != OPEN_SET_ID
        test_unknown = y == OPEN_SET_ID
        if test_known.any() and test_unknown.any():
            dataset_metrics[dataset_id]["open_set"] = evaluate_open_set(
                raw_logits[test_known, :OPEN_SET_ID] / temperature,
                raw_logits[test_unknown, :OPEN_SET_ID] / temperature,
            )
            dataset_metrics[dataset_id]["calibrated_reject_rule"] = evaluate_reject_thresholds(
                raw_logits[test_known, :OPEN_SET_ID] / temperature,
                raw_logits[test_unknown, :OPEN_SET_ID] / temperature,
                open_set_thresholds,
            )
        else:
            dataset_metrics[dataset_id]["open_set"] = None
            dataset_metrics[dataset_id]["calibrated_reject_rule"] = None

    all_test_rows = [row for dataset_id in test_datasets for row in test_outputs[dataset_id][0]]
    all_test_raw_logits = np.concatenate([test_outputs[dataset_id][1] for dataset_id in test_datasets])
    all_test_y = np.asarray([row["class_id"] for row in all_test_rows], dtype=np.int64)
    all_test_known = all_test_y != OPEN_SET_ID
    all_test_unknown = all_test_y == OPEN_SET_ID
    if all_test_known.any() and all_test_unknown.any():
        open_set_test = {
            **evaluate_open_set(
                all_test_raw_logits[all_test_known, :OPEN_SET_ID] / temperature,
                all_test_raw_logits[all_test_unknown, :OPEN_SET_ID] / temperature,
            ),
            "calibrated_reject_rule": evaluate_reject_thresholds(
                all_test_raw_logits[all_test_known, :OPEN_SET_ID] / temperature,
                all_test_raw_logits[all_test_unknown, :OPEN_SET_ID] / temperature,
                open_set_thresholds,
            ),
        }
    else:
        open_set_test = None

    if "plantvillage" in dataset_metrics:
        lab = dataset_metrics["plantvillage"]["macro_f1"]
        for dataset_id, metrics in dataset_metrics.items():
            if dataset_id != "plantvillage":
                metrics["lab_minus_field_macro_f1"] = float(lab - metrics["macro_f1"])

    model_path = output_root / f"{args.backbone}.keras"
    train_model.save(model_path)
    args.export_dir.mkdir(parents=True, exist_ok=True)
    converter = tf.lite.TFLiteConverter.from_keras_model(export_model)
    tflite_bytes = converter.convert()
    tflite_path = args.export_dir / "agrodoc_model.tflite"
    tflite_path.write_bytes(tflite_bytes)
    kernel = train_model.get_layer("logits").get_weights()[0]
    if kernel.shape != (FEATURE_CHANNELS, len(classes)):
        raise RuntimeError(f"unexpected CAM kernel shape {kernel.shape}")
    (args.export_dir / "cam_weights.bin").write_bytes(kernel.astype("<f4").tobytes(order="C"))
    (args.export_dir / "labels.json").write_text(
        json.dumps(class_names, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    parity = verify_tflite_parity(tf, export_model, _rows_for(rows, "test")[0]["path"], tflite_path)
    report = {
        "status": "incomplete_until_human_gate_and_device_review",
        "backbone": args.backbone,
        "temperature": temperature,
        "open_set_thresholds": open_set_thresholds,
        "open_set_test": open_set_test,
        "datasets": dataset_metrics,
        "tflite_size_bytes": len(tflite_bytes),
        "keras_tflite_parity": parity,
        "trust_score_auroc": None,
        "trust_score_auroc_note": "requires calibrated on-device trust scorer after Phase 2",
        "deletion_insertion_faithfulness": None,
        "deletion_insertion_note": "not yet implemented",
        "on_device_latency_ms": None,
        "on_device_latency_note": "requires target-phone benchmark",
        "dataset_fingerprints": dataset_fingerprints(rows),
        "manifest_sha256": _sha256_file(args.manifest),
        "model_keras_path": str(model_path),
    }
    (output_root / "metrics.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    meta = {
        "schema_version": 1,
        "model_version": args.model_version,
        "backbone": args.backbone,
        "input_size": [IMAGE_SIZE, IMAGE_SIZE, 3],
        "logit_count": len(classes),
        "feature_map_shape": [7, 7, FEATURE_CHANNELS],
        "crops": sorted({str(row["crop"]) for row in classes if row["crop"] != "unknown"}),
        "temperature": temperature,
        "open_set_thresholds": open_set_thresholds,
        "metrics_path": str((output_root / "metrics.json").resolve()),
        "dataset_fingerprints": report["dataset_fingerprints"],
        "verified": False,
    }
    (args.export_dir / "model_meta.json").write_text(json.dumps(meta, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Exported model assets to {args.export_dir}")
    print(f"Wrote metrics to {output_root / 'metrics.json'}")


def train_and_export(args):
    if args.backbone != "all":
        return train_one_backbone(args)

    import copy

    candidate_names = ["mobilenetv2", "mobilenetv3large", "efficientnet_lite0"]
    results: dict[str, Any] = {}
    for name in candidate_names:
        candidate_args = copy.copy(args)
        candidate_args.backbone = name
        candidate_args.reports_dir = args.reports_dir / "candidates" / name
        candidate_args.export_dir = args.export_dir / "model_candidates" / name
        try:
            train_one_backbone(candidate_args)
            candidate_metrics = json.loads((candidate_args.reports_dir / "metrics.json").read_text(encoding="utf-8"))
            results[name] = {"status": "trained", "metrics": candidate_metrics}
        except NotImplementedError as error:
            results[name] = {"status": "blocked", "reason": str(error)}

    report = {
        "status": "incomplete",
        "candidates": results,
        "selection": None,
        "selection_note": "A winner requires all three candidates plus a target-phone latency benchmark. No candidate is promoted automatically.",
    }
    args.reports_dir.mkdir(parents=True, exist_ok=True)
    report_path = args.reports_dir / "candidate_bakeoff.json"
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote candidate bake-off status to {report_path}")


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--classes", type=Path, default=CLASSES_PATH)
    parser.add_argument("--backbone", choices=["mobilenetv2", "mobilenetv3large", "efficientnet_lite0", "all"], default="mobilenetv2")
    parser.add_argument("--epochs-head", type=int, default=5)
    parser.add_argument("--epochs-fine", type=int, default=5)
    parser.add_argument("--finetune-layers", type=int, default=30)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--learning-rate", type=float, default=1e-3)
    parser.add_argument("--fine-learning-rate", type=float, default=1e-5)
    parser.add_argument("--model-version", default="unverified-v2")
    parser.add_argument("--tta-flip", action="store_true")
    parser.add_argument("--reports-dir", type=Path, default=ROOT / "reports")
    parser.add_argument("--export-dir", type=Path, default=ROOT / "ml" / "exports")
    args = parser.parse_args()
    if args.epochs_head < 1 or args.epochs_fine < 1 or args.batch_size < 1 or args.finetune_layers < 1:
        parser.error("epoch counts, batch size, and finetune layers must be positive")
    return args


if __name__ == "__main__":
    try:
        train_and_export(parse_args())
    except Exception as error:
        print(f"Training stopped: {error}", file=sys.stderr)
        raise
