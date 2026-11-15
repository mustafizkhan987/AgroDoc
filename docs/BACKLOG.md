# AgroDoc Backlog & Release Status

---

## 1. Feature Completion Status (November 2026)

| Module / Feature | Status | Implementation Details |
|:---|:---:|:---|
| **Multi-Flavor Architecture (`offline` vs `connected`)** | ✅ DONE | Enforced in Gradle and `AndroidManifest.xml`. `offline` contains 0 network permissions. |
| **Open-Meteo Weather Integration** | ✅ DONE | `OpenMeteoWeatherRepository` in `connected` flavor; `NoOpWeatherRepository` in `offline`. 6-hour Room cache with stale indicators. |
| **Karnataka Crop Directory (26 Crops)** | ✅ DONE | `crops.json` with 4 languages (KN, HI, MR, EN), agro-climatic zones, soils, sowing windows, water needs, and growth stages. |
| **Expanded Disease Library (36 Pathogens)** | ✅ DONE | `disease_info.json` with symptoms, causes, biological/chemical treatments, pre-harvest intervals, and favorable weather parameters. |
| **14-Day Tempograph Canvas Chart** | ✅ DONE | Custom Jetpack Compose Canvas component on Home screen: min/max temperature ribbon, rainfall bars, today reference line, and interactive touch scrubbing. |
| **Weather-Informed Disease Risk Engine** | ✅ DONE | Pure Kotlin `DiseaseRiskEngine.kt` evaluating 7-day meteorological forecasts against verified agronomic risk thresholds. |
| **Crop Suitability & Fertilizer Advisor** | ✅ DONE | `CropSuitabilityEngine.kt` and `FertilizerAdvisor.kt` calculating exact NPK splits, fertilizer products (Urea, DAP, MOP), and soil-test adjustments for custom farm plots. |
| **My Farm Land Parcels & FarmLog Timeline** | ✅ DONE | Room v3 migration with `PlotEntity` and `FarmLogEntity`. Days After Sowing (DAS) ring gauge, activity logging (sowing, spraying, irrigation, harvest), and CSV export via Share Sheet. |
| **Multilingual Knowledge Assistant** | ✅ DONE | Hybrid architecture: Tier A offline BM25 indexed search + Tier B connected advisory + Android `TextToSpeech` audio read-aloud. |
| **Background Weather Sync & Push Alerts** | ✅ DONE | AndroidX `WorkManager` (`WeatherSyncWorker`) running 6-hour periodic sync with network constraints, dispatching notifications for high-risk diseases in "My Crops". |
| **Model Fallback & Safety Flow** | ✅ DONE | Graceful missing model handling with UI guidance directing the user to the manual crop pathology library. |

---

## 2. Future Improvements (Post-MVP)

| # | Item | Priority | Notes |
|:---:|:---|:---:|:---|
| 1 | **Bundled Noto Font TTFs** | P2 | Currently utilizing downloadable font provider XMLs. Bundling offline TTFs in `res/font/` will guarantee full typography rendering in air-gapped environments without Google Play Services. |
| 2 | **Full GPU Farm-Photo Retraining** | P1 | Execute `ml/train_export.py` on a distributed GPU cluster once multi-source field datasets (PlantVillage + Field Annotated Sets) are curated and licensed. |
| 3 | **Dark Theme Support** | P3 | Add Material 3 Dark ColorScheme tokens to `AgroDocTheme`. |
| 4 | **Pest Trapping & Visual Damage Guide** | P2 | Expand knowledge base with Fall Armyworm (FAW) pheromone trap counts and stem borer scouting checklists. |
