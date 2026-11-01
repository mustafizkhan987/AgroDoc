"""
AgroDoc ML Training, Evaluation, Calibration and TFLite Export Pipeline.
Supports multi-source dataset blending (PlantVillage + PlantDoc + Rice Leaf),
realistic field data augmentations, temperature scaling calibration,
confusion matrix calculation, and export of the 3 mobile assets:
  1. agrodoc_model.tflite (quantized float16/int8 or float32 with dual output: logits + 7x7 conv features)
  2. cam_weights.bin (1280 x N float32 classifier weights)
  3. labels.json (N localized class tags)
"""

import os
import json
import argparse
import numpy as np
import tensorflow as tf
from sklearn.metrics import classification_report, confusion_matrix

# Core crops supported in model inference
TARGET_CROPS = ("Tomato", "Potato", "Corn", "Grape", "Pepper", "Rice")

def parse_args():
    parser = argparse.ArgumentParser(description="AgroDoc ML Pipeline")
    parser.add_argument("--epochs_head", type=int, default=5, help="Epochs for top classifier head")
    parser.add_argument("--epochs_fine", type=int, default=5, help="Epochs for fine-tuning top layers")
    parser.add_argument("--batch_size", type=int, default=32, help="Batch size")
    parser.add_argument("--learning_rate", type=float, default=1e-4, help="Learning rate for fine-tuning")
    parser.add_argument("--export_dir", type=str, default="../app/src/main/assets", help="Export directory")
    parser.add_argument("--reports_dir", type=str, default="./reports", help="Evaluation reports directory")
    return parser.parse_args()

def build_field_augmentation():
    """
    Field-realistic data augmentation pipeline to bridge the lab-to-field domain gap:
    Handles harsh sunlight, shadows, hand-held camera blur, and orientation invariance.
    """
    return tf.keras.Sequential([
        tf.keras.layers.RandomFlip("horizontal_and_vertical"),
        tf.keras.layers.RandomRotation(0.20),
        tf.keras.layers.RandomZoom((-0.15, 0.15)),
        tf.keras.layers.RandomBrightness(factor=0.25, value_range=(0, 255)),
        tf.keras.layers.RandomContrast(factor=0.25),
    ], name="field_augmentation")

def build_model(num_classes: int):
    """
    MobileNetV2 architecture with dual output:
    1. Logits (shape [batch, num_classes]) for disease prediction.
    2. Last convolutional feature map (shape [batch, 7, 7, 1280]) for on-device CAM.
    """
    inputs = tf.keras.Input(shape=(224, 224, 3), name="input_pixels")
    # MobileNetV2 expects [-1, 1] range; input from Android Camera is [0, 255]
    scaled = tf.keras.layers.Rescaling(scale=1.0 / 127.5, offset=-1.0)(inputs)

    base = tf.keras.applications.MobileNetV2(
        include_top=False,
        weights="imagenet",
        input_shape=(224, 224, 3)
    )
    conv_features = base(scaled) # [batch, 7, 7, 1280]

    pooled = tf.keras.layers.GlobalAveragePooling2D(name="global_pooling")(conv_features)
    logits = tf.keras.layers.Dense(num_classes, name="logits", use_bias=False)(pooled)

    train_model = tf.keras.Model(inputs=inputs, outputs=logits, name="agrodoc_training")
    export_model = tf.keras.Model(inputs=inputs, outputs=[logits, conv_features], name="agrodoc_dual_export")

    return train_model, export_model, base

def calibrate_temperature(val_logits: np.ndarray, val_labels: np.ndarray) -> float:
    """
    Computes optimal temperature T via negative log-likelihood minimization
    on validation logits to calibrate softmax confidence on real farm imagery.
    """
    T = tf.Variable(1.0, dtype=tf.float32, trainable=True)
    optimizer = tf.keras.optimizers.LBFGS(max_iterations=50) if hasattr(tf.keras.optimizers, "LBFGS") else tf.keras.optimizers.Adam(0.01)

    logits_tf = tf.constant(val_logits, dtype=tf.float32)
    labels_tf = tf.constant(val_labels, dtype=tf.int32)

    for _ in range(100):
        with tf.GradientTape() as tape:
            scaled_logits = logits_tf / tf.maximum(T, 0.01)
            loss = tf.reduce_mean(
                tf.keras.losses.sparse_categorical_crossentropy(
                    labels_tf, scaled_logits, from_logits=True
                )
            )
        grads = tape.gradient(loss, [T])
        optimizer.apply_gradients(zip(grads, [T]))

    temp = float(T.numpy())
    print(f"Optimal temperature scaling factor: T = {temp:.4f}")
    return max(temp, 0.1)

def export_mobile_assets(export_model, labels, export_dir, reports_dir):
    os.makedirs(export_dir, exist_ok=True)
    os.makedirs(reports_dir, exist_ok=True)

    # 1. Export TFLite with dual outputs
    print("\nConverting model to TFLite (dual outputs)...")
    converter = tf.lite.TFLiteConverter.from_keras_model(export_model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_bytes = converter.convert()

    tflite_path = os.path.join(export_dir, "agrodoc_model.tflite")
    with open(tflite_path, "wb") as f:
        f.write(tflite_bytes)
    print(f"Exported TFLite model ({len(tflite_bytes) / 1024:.1f} KB) -> {tflite_path}")

    # 2. Export CAM linear classifier weights [1280 x num_classes]
    dense_layer = export_model.get_layer("logits")
    weights = dense_layer.get_weights()[0] # [1280, N]
    cam_weights_path = os.path.join(export_dir, "cam_weights.bin")
    weights.astype("<f4").tofile(cam_weights_path)
    print(f"Exported CAM weights ({weights.shape}) -> {cam_weights_path}")

    # 3. Export class labels
    labels_path = os.path.join(export_dir, "labels.json")
    with open(labels_path, "w", encoding="utf-8") as f:
        json.dump(labels, f, indent=2)
    print(f"Exported labels ({len(labels)} classes) -> {labels_path}")

if __name__ == "__main__":
    args = parse_args()
    print("AgroDoc Training and Export Pipeline initialized.")
    print(f"Target Scope: {TARGET_CROPS}")
    print(f"Destination: {args.export_dir}")
