# Datasets Documentation for AgroDoc ML Pipeline

This document details all datasets integrated or evaluated for training and calibrating AgroDoc's on-device diagnostic model.

---

## 1. Primary Datasets

### A. PlantVillage Dataset
- **Description**: 54,306 images of healthy and diseased crop leaves across 38 classes collected under laboratory conditions (uniform grey/black backgrounds).
- **Source**: Mohanty et al., *Using Deep Learning for Image-Based Plant Disease Detection*, Frontiers in Plant Science (2016).
- **License**: Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0).
- **AgroDoc Scope Classes (Initial 23 classes)**:
  - **Tomato (9 classes)**: Bacterial Spot, Early Blight, Late Blight, Leaf Mold, Septoria Leaf Spot, Spider Mites (Two-spotted spider mite), Target Spot, Yellow Leaf Curl Virus, Mosaic Virus, Healthy.
  - **Potato (3 classes)**: Early Blight, Late Blight, Healthy.
  - **Corn / Maize (4 classes)**: Common Rust, Northern Leaf Blight, Cercospora Leaf Spot (Gray leaf spot), Healthy.
  - **Grape (4 classes)**: Black Rot, Esca (Black Measles), Leaf Blight (Isariopsis Leaf Spot), Healthy.
  - **Pepper / Bell Pepper (2 classes)**: Bacterial Spot, Healthy.
- **Limitation**: Pure laboratory imagery with artificial backgrounds. Models trained solely on PlantVillage suffer severe accuracy drops (often >40% drops) when evaluated on real-world farmer field photos with soil, weeds, shadows, and hands in frame.

---

### B. PlantDoc Dataset (Field Conditions)
- **Description**: 2,598 images across 13 plant species and 27 classes photographed under real agricultural field conditions across India (natural backgrounds, direct sunlight, partial occlusion, multiple leaves).
- **Source**: Singh et al., *PlantDoc: A Dataset for Collaborative Computer Vision Applications in Agriculture*, ACM CoNEXT (2019). IIT Bombay / IIIT Hyderabad.
- **License**: Creative Commons Attribution 4.0 International (CC BY 4.0).
- **Overlapping Target Classes with Karnataka Priority**:
  - Corn: Northern Leaf Blight, Common Rust
  - Potato: Early Blight, Late Blight
  - Tomato: Early Blight, Late Blight, Leaf Curl, Septoria Spot
  - Bell Pepper: Leaf Spot
  - Grape: Black Rot, Leaf Blight
- **Role in AgroDoc**: Used for field-domain fine-tuning and validation benchmark to measure genuine real-world accuracy rather than laboratory overfit.

---

### C. Rice Leaf Disease Dataset (Karnataka Priority Addition)
- **Description**: 3,355 images across 4 major paddy diseases: Bacterial Leaf Blight, Brown Spot, Leaf Blast, and Tungro.
- **Source**: UCI Machine Learning Repository / Mendeley Data (Paddy Doctor & Rice Leaf Disease Datasets).
- **License**: CC BY 4.0.
- **Role in AgroDoc**: Provides real field classes for Karnataka's primary staple crop (Paddy/Rice) in expanded Track B training.

---

## 2. Lab-to-Field Domain Adaptation Strategy

To address the documented fragility of lab-trained models in real farm environments, the training pipeline (`ml/train_export.py`) incorporates:

1. **Photometric Augmentations**:
   - Random brightness variation ($\pm 25\%$) simulating variable sunlight vs overcast skies.
   - Random contrast & saturation jitter simulating phone camera variations.
   - Gaussian blur simulating wind movement and camera handshake.
2. **Geometric Augmentations**:
   - Horizontal and vertical flips.
   - Random rotation ($\pm 20^\circ$).
   - Random zoom ($0.8\times$ to $1.2\times$).
   - Random cropping to force attention to local lesion patterns rather than leaf outline.
3. **Background Invariance**:
   - Synthetic background insertion / CutMix using natural field textures.
4. **Post-Training Calibration**:
   - Temperature scaling on validation logits to align confidence scores with empirical accuracy before CAM and Trust calculation.

---

## 3. Data Attribution and Compliance

All datasets used in AgroDoc comply with non-commercial and open academic distribution guidelines under Creative Commons licenses. Dataset citations and credits are surfaced directly within the app's **About & Transparency** screen.

