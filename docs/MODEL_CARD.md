# Model Card: AgroDoc Plant Pathology Classifier & Explainability Engine

## Model Details
- **Architecture**: MobileNetV3-Small (Feature Extractor + Custom Dense Classification Head) with LiteRT (TensorFlow Lite) runtime.
- **Input Dimension**: $224 \times 224 \times 3$ RGB Float32 tensor normalized in $[0, 255]$ range (center-cropped and bilinear scaled).
- **Output Tensors**:
  1. `logits`: Raw logit vector across 24 classes (23 crop-disease classes + 1 `not_a_leaf` background class).
  2. `features`: Convolutional feature activation map ($7 \times 7 \times 1280$) utilized for on-device Grad-CAM generation.
- **Explainability**: Custom on-device Grad-CAM implementation calculating gradient-weighted class activation heatmaps overlaying the original input bitmap.
- **Calibration**: Post-hoc Temperature Scaling ($T \approx 1.25$) applied to raw logits prior to Softmax probability generation to minimize Expected Calibration Error (ECE).
- **Target Platform**: Android 7.0+ (API 24+) executing on-device via LiteRT without requiring cloud server inference.

---

## Classes & Target Scope (24 Classes)

| Class Index | Crop | Condition / Disease | Source Dataset |
|:---:|:---|:---|:---|
| 0 | Corn | Cercospora Leaf Spot (Gray Leaf Spot) | PlantVillage |
| 1 | Corn | Common Rust | PlantVillage |
| 2 | Corn | Northern Leaf Blight | PlantVillage |
| 3 | Corn | Healthy | PlantVillage |
| 4 | Grape | Black Rot | PlantVillage |
| 5 | Grape | Esca (Black Measles) | PlantVillage |
| 6 | Grape | Leaf Blight (Isariopsis Leaf Spot) | PlantVillage |
| 7 | Grape | Healthy | PlantVillage |
| 8 | Pepper | Bacterial Spot | PlantVillage |
| 9 | Pepper | Healthy | PlantVillage |
| 10 | Potato | Early Blight | PlantVillage |
| 11 | Potato | Late Blight | PlantVillage |
| 12 | Potato | Healthy | PlantVillage |
| 13 | Tomato | Bacterial Spot | PlantVillage |
| 14 | Tomato | Early Blight | PlantVillage |
| 15 | Tomato | Late Blight | PlantVillage |
| 16 | Tomato | Leaf Mold | PlantVillage |
| 17 | Tomato | Septoria Leaf Spot | PlantVillage |
| 18 | Tomato | Spider Mites (Two-spotted spider mite) | PlantVillage |
| 19 | Tomato | Target Spot | PlantVillage |
| 20 | Tomato | Yellow Leaf Curl Virus (TYLCV) | PlantVillage |
| 21 | Tomato | Mosaic Virus | PlantVillage |
| 22 | Tomato | Healthy | PlantVillage |
| 23 | Non-crop | Not a Leaf (Out-of-Distribution background rejection) | Background Negative Set |

*Note: For Karnataka crops without certified on-device ML training data (Paddy, Ragi, Sugarcane, Cotton, Arecanut, Banana, Coffee, etc.), AgroDoc provides manual disease identification and expert agronomy guides through the Knowledge Library.*

---

## Trust Scoring & Calibration Formula

AgroDoc incorporates a 3-tier Trust Rating (High, Medium, Low) evaluated via `TrustScorer.kt`:
1. **Calibrated Confidence**: Softmax temperature-scaled probability $P(\text{class}) = \frac{\exp(z_i / T)}{\sum_j \exp(z_j / T)}$.
2. **Top-2 Margin**: Gap between highest probability $P_1$ and runner-up $P_2$. High confidence with a tiny margin indicates class ambiguity.
3. **CAM Focus Ratio**: Fraction of heat activation localized on the detected leaf blade vs. background clutter.
4. **Out-of-Distribution Check**: Activation of class 23 (`not_a_leaf`) triggers automatic Low Trust and prompts the user to retake the photo.

### Rating Thresholds:
- **High Trust**: Calibrated Confidence $\ge 0.82$, Margin $\ge 0.25$, CAM Focus $\ge 0.60$.
- **Medium Trust**: Calibrated Confidence $\ge 0.60$, Margin $\ge 0.15$.
- **Low Trust**: Calibrated Confidence $< 0.60$, high entropy, or CAM localization outside leaf region. Triggers UI advice to rescan or consult an Agricultural Extension Officer.

---

## Training & Pipeline Architecture
- Pipeline script: `ml/train_export.py` with Colab orchestration in `ml/AgroDoc_Training_Colab.ipynb`.
- Optimization: AdamW ($\text{lr} = 10^{-3}$ with cosine annealing schedule), Cross-Entropy loss with label smoothing ($\alpha = 0.05$).
- Data Augmentation: Random affine rotations ($\pm 30^\circ$), color jittering (brightness, contrast, saturation), Gaussian blur, random shadows, and synthetic background blending to bridge the laboratory-to-field domain gap.

---

## Limitations, Out-of-Scope Use & Safety
- **No Autonomous Chemical Application**: AgroDoc is an auxiliary diagnostic tool. Chemical recommendations must never be applied without physical verification by an agricultural expert or Agronomy Extension Officer (Raitha Samparka Kendra / KVK).
- **Domain Shift**: Standard PlantVillage imagery was collected under controlled indoor laboratory conditions. Natural outdoor sunlight, water droplets, soil splashes, and severe camera blur degrade confidence.
- **Multiple Co-occurring Pathogens**: Current architecture assumes single dominant visual diagnosis. If a leaf displays both nutrient deficiency chlorosis and fungal lesions, the primary lesion is classified.
