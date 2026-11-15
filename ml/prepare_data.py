"""Build a leakage-aware CSV manifest from approved local image datasets.

This tool never downloads data implicitly. The PlantVillage command requires an
explicit Hugging Face revision; other sources must first be downloaded and
reviewed by a person, then passed through the folder command.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
from numbers import Integral
from pathlib import Path
from typing import Any

import yaml
from sklearn.model_selection import GroupShuffleSplit


ROOT = Path(__file__).resolve().parents[1]
CLASSES_PATH = Path(__file__).with_name("classes.yaml")
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
SPLIT_ALIASES = {
    "train": "train",
    "training": "train",
    "val": "validation",
    "valid": "validation",
    "validation": "validation",
    "test": "test",
    "testing": "test",
}
MANIFEST_FIELDS = [
    "path",
    "dataset_id",
    "source_label",
    "class_id",
    "crop",
    "disease_id",
    "image_model",
    "split",
    "group_id",
    "group_id_source",
    "sha256",
]


def load_classes(path: Path = CLASSES_PATH) -> tuple[list[dict[str, Any]], dict[str, dict[str, Any]]]:
    payload = yaml.safe_load(path.read_text(encoding="utf-8"))
    rows = payload.get("classes", [])
    ids = [int(row["class_id"]) for row in rows]
    if len(ids) != len(set(ids)):
        raise ValueError("classes.yaml contains duplicate class_id values")
    by_label: dict[str, dict[str, Any]] = {}
    for row in rows:
        labels = [str(row["source_label"]), *[str(alias) for alias in row.get("aliases", [])]]
        for label in labels:
            if label in by_label:
                raise ValueError(f"classes.yaml contains duplicate source label or alias: {label}")
            by_label[label] = row
    return rows, by_label


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _split_from_parts(parts: tuple[str, ...]) -> str | None:
    for part in parts:
        if part.casefold() in SPLIT_ALIASES:
            return SPLIT_ALIASES[part.casefold()]
    return None


def _read_metadata(path: Path | None, root: Path) -> dict[str, dict[str, str]]:
    if path is None:
        return {}
    result: dict[str, dict[str, str]] = {}
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        for row in csv.DictReader(handle):
            relative = Path(row["path"]).as_posix().lstrip("./")
            if relative in result:
                raise ValueError(f"duplicate metadata path: {relative}")
            result[relative] = {key: value for key, value in row.items() if value is not None}
    return result


def folder_rows(
    root: Path,
    dataset_id: str,
    metadata_path: Path | None = None,
    classes_path: Path = CLASSES_PATH,
    skip_unmapped: bool = False,
) -> list[dict[str, Any]]:
    """Index a class-folder dataset. A metadata CSV may supply path/group/split."""
    class_rows, by_label = load_classes(classes_path)
    del class_rows
    root = root.resolve()
    metadata = _read_metadata(metadata_path, root)
    images = sorted(path for path in root.rglob("*") if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES)
    if not images:
        raise ValueError(f"no supported images found under {root}")

    rows: list[dict[str, Any]] = []
    unknown_labels: set[str] = set()
    for image in images:
        relative = image.relative_to(root).as_posix()
        parts = image.relative_to(root).parts
        meta = metadata.get(relative, {})
        source_label = meta.get("source_label")
        class_row = None
        if meta.get("class_id"):
            class_row = next(
                (candidate for candidate in by_label.values() if int(candidate["class_id"]) == int(meta["class_id"])),
                None,
            )
            source_label = source_label or (class_row["source_label"] if class_row else None)
        if class_row is None and source_label:
            class_row = by_label.get(source_label)
        if class_row is None:
            for part in reversed(parts[:-1]):
                class_row = by_label.get(part)
                if class_row:
                    source_label = source_label or part
                    break
        if class_row is None:
            unknown_labels.add(source_label or (parts[-2] if len(parts) > 1 else "<root>"))
            if skip_unmapped:
                continue
            continue

        split = meta.get("split") or _split_from_parts(parts)
        if split:
            split = SPLIT_ALIASES.get(split.casefold(), split.casefold())
        group = meta.get("group_id")
        group_source = "provided_metadata" if group else "sha256_exact_only"
        digest = sha256(image)
        group = group or f"sha256:{digest}"
        if split not in {"train", "validation", "test"}:
            raise ValueError(
                f"{relative} has no train/validation/test split; add split metadata to {metadata_path or 'a metadata CSV'}"
            )
        rows.append(
            {
                "path": str(image.resolve()),
                "dataset_id": dataset_id,
                "source_label": str(source_label or class_row["source_label"]),
                "class_id": int(class_row["class_id"]),
                "crop": str(class_row["crop"]),
                "disease_id": str(class_row["disease_id"]),
                "image_model": str(bool(class_row["image_model"])).lower(),
                "split": split,
                "group_id": f"{dataset_id}:{group}",
                "group_id_source": group_source,
                "sha256": digest,
            }
        )

    if unknown_labels and not skip_unmapped:
        labels = ", ".join(sorted(unknown_labels))
        raise ValueError(
            "unmapped class folders or labels: " + labels + ". Review the data and add explicit class mappings before indexing."
        )
    validate_groups(rows)
    return rows


def validate_groups(rows: list[dict[str, Any]]) -> None:
    group_splits: dict[str, set[str]] = {}
    hash_splits: dict[str, set[str]] = {}
    for row in rows:
        group_splits.setdefault(row["group_id"], set()).add(row["split"])
        image_hash = row.get("sha256")
        if image_hash:
            hash_splits.setdefault(image_hash, set()).add(row["split"])
    leaking_groups = [group for group, splits in group_splits.items() if len(splits) > 1]
    duplicate_hashes = [digest for digest, splits in hash_splits.items() if len(splits) > 1]
    if leaking_groups:
        raise ValueError(f"{len(leaking_groups)} source groups cross data splits")
    if duplicate_hashes:
        raise ValueError(f"{len(duplicate_hashes)} exact duplicate images cross data splits")


def split_training_groups(
    rows: list[dict[str, Any]], validation_fraction: float = 0.15, seed: int = 42
) -> list[dict[str, Any]]:
    """Move whole train groups into validation before any augmentation occurs."""
    if not 0.0 < validation_fraction < 1.0:
        raise ValueError("validation_fraction must be between zero and one")
    train_rows = [row for row in rows if row["split"] == "train"]
    groups = sorted({row["group_id"] for row in train_rows})
    if len(groups) < 2:
        raise ValueError("at least two training groups are required to make a validation split")
    splitter = GroupShuffleSplit(n_splits=1, test_size=validation_fraction, random_state=seed)
    indices = list(range(len(train_rows)))
    group_ids = [row["group_id"] for row in train_rows]
    _, val_indices = next(splitter.split(indices, groups=group_ids))
    val_groups = {group_ids[index] for index in val_indices}
    result = []
    for row in rows:
        updated = dict(row)
        if updated["split"] == "train" and updated["group_id"] in val_groups:
            updated["split"] = "validation"
        result.append(updated)
    validate_groups(result)
    return result


def write_manifest(rows: list[dict[str, Any]], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=MANIFEST_FIELDS)
        writer.writeheader()
        writer.writerows(rows)


def prepare_plantvillage(revision: str, output_root: Path) -> Path:
    """Export the approved PlantVillage HF revision with leaf IDs and source splits."""
    if not revision or revision == "main":
        raise ValueError("pin an immutable Hugging Face revision; a moving branch is not reproducible")
    try:
        from datasets import load_dataset
    except ImportError as error:
        raise RuntimeError("install the `datasets` package in the training environment first") from error

    _, by_label = load_classes()
    source = load_dataset("mohanty/PlantVillage", "color", revision=revision)
    label_names = source["train"].features["label"].names
    raw_root = output_root / "raw" / "plantvillage"
    rows: list[dict[str, Any]] = []
    for source_split in ("train", "test"):
        if source_split not in source:
            raise ValueError(f"pinned PlantVillage revision has no {source_split!r} split")
        for index, sample in enumerate(source[source_split]):
            raw_label = sample["label"]
            source_label = label_names[int(raw_label)] if isinstance(raw_label, Integral) else str(raw_label)
            class_row = by_label.get(source_label)
            if class_row is None:
                continue
            image = sample["image"].convert("RGB")
            target = raw_root / source_split / source_label / f"{index:06d}.jpg"
            target.parent.mkdir(parents=True, exist_ok=True)
            image.save(target, format="JPEG", quality=95, subsampling=0)
            leaf_id = str(sample.get("leaf_id", "")).strip()
            if not leaf_id:
                raise ValueError("PlantVillage sample missing leaf_id; refusing a random image split")
            rows.append(
                {
                    "path": str(target.resolve()),
                    "dataset_id": "plantvillage",
                    "source_label": source_label,
                    "class_id": int(class_row["class_id"]),
                    "crop": str(class_row["crop"]),
                    "disease_id": str(class_row["disease_id"]),
                    "image_model": str(bool(class_row["image_model"])).lower(),
                    "split": source_split,
                    "group_id": f"plantvillage:leaf:{leaf_id}",
                    "group_id_source": "upstream_leaf_id",
                    "sha256": sha256(target),
                }
            )
    rows = split_training_groups(rows)
    validate_groups(rows)
    manifest = output_root / "manifests" / "plantvillage.csv"
    write_manifest(rows, manifest)
    metadata = {
        "dataset": "mohanty/PlantVillage",
        "config": "color",
        "revision": revision,
        "split_counts": {split: sum(row["split"] == split for row in rows) for split in ("train", "validation", "test")},
        "class_count": len({row["class_id"] for row in rows}),
        "manifest": str(manifest.resolve()),
    }
    (manifest.with_suffix(".metadata.json")).write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    return manifest


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    plantvillage = subparsers.add_parser("plantvillage-hf", help="export a pinned PlantVillage revision")
    plantvillage.add_argument("--revision", required=True, help="immutable Hugging Face commit SHA")
    plantvillage.add_argument("--output-root", type=Path, default=ROOT / "ml" / "data")

    folder = subparsers.add_parser("folder", help="index a manually downloaded class-folder dataset")
    folder.add_argument("--root", required=True, type=Path)
    folder.add_argument("--dataset-id", required=True)
    folder.add_argument("--metadata", type=Path, help="CSV path,group_id,split,source_label,class_id")
    folder.add_argument("--output", type=Path, required=True)
    folder.add_argument("--skip-unmapped", action="store_true", help="skip folders without a reviewed class mapping")

    args = parser.parse_args()
    if args.command == "plantvillage-hf":
        manifest = prepare_plantvillage(args.revision, args.output_root)
    else:
        rows = folder_rows(args.root, args.dataset_id, args.metadata, skip_unmapped=args.skip_unmapped)
        if args.skip_unmapped:
            print("Unmapped class folders, if any, were excluded from the manifest.")
        manifest = args.output
        write_manifest(rows, manifest)
    print(f"Wrote {manifest}")


if __name__ == "__main__":
    main()

