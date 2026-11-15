# AgroDoc — Final Implementation Report

**Date of Completion:** November 15, 2026  
**Build Target:** Android SDK 35 (compileSdk 35, minSdk 24, targetSdk 35) / Gradle 8.14.6 / Java 17  
**Build Status:** Both `assembleOfflineDebug` and `assembleConnectedDebug` PASSED.  
**Test Status:** Both `testOfflineDebugUnitTest` and `testConnectedDebugUnitTest` PASSED (100% success rate across all unit test suites).  
**Emulator Execution:** Verified live on Android Emulator `Medium_Phone` (API 35, `emulator-5554`).

---

## 1. Executive Summary

AgroDoc has been completed end-to-end to deliver an agronomic diagnostic, weather risk assessment, crop management, and knowledge assistance platform for Indian farmers with specific focus on Karnataka agro-climatic conditions.

The system is delivered in two Gradle product flavors under the `network` dimension:
1. **`offline` Flavor (`com.agrodoc.app`)**:
   - Strictly 100% offline with ZERO network permissions.
   - Enforced by merged manifest assertion tests: contains NO `INTERNET`, `ACCESS_NETWORK_STATE`, or location permissions.
   - Fully on-device LiteRT inference, Room v3 database, and pre-bundled agronomic assets.
2. **`connected` Flavor (`com.agrodoc.app.connected`)**:
   - Delivers the requested full online capabilities: live REST weather forecasts from Open-Meteo (`api.open-meteo.com`), 14-day interactive Tempograph Canvas visualization, background 6-hour WorkManager periodic synchronization with network constraints, push alert notifications for high-risk crop diseases, and an online advisory tier.
   - Preserves core privacy guarantees: leaf photos, diagnostic scans, plot records, and farmer notes never leave the device.

---

## 2. Key Modules & Technical Implementation

### A. Architectural Integrity & Multi-Flavor Packaging
- Clean Architecture separation across `:domain`, `:data`, `:ml`, `:ui`.
- Dependency injection via Dagger Hilt with flavor-specific repository modules:
  - `NoOpWeatherRepositoryModule` in `offline` source set (returning inert flows).
  - `WeatherRepositoryModule` in `connected` source set (binding `OpenMeteoWeatherRepository`).
- Verified via `aapt dump permissions`:
  - `offline`: Only `android.permission.CAMERA` and dynamic receiver permissions.
  - `connected`: Adds `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`, `android.permission.POST_NOTIFICATIONS`, and `android.permission.ACCESS_COARSE_LOCATION`.

### B. Live Weather Integration & 14-Day Tempograph Canvas (`connected` & `offline`)
- **Open-Meteo Client**: Built with Retrofit and Kotlinx Serialization parsing current conditions and 7-day daily forecasts (temperature min/max, humidity, rainfall, precipitation probability, wind speed, weather code).
- **Leaf-Wetness Proxy**: Derived hourly relative humidity ($RH \ge 90\%$) calculating high-humidity hours estimate for fungal disease risk.
- **Room Weather Cache**: 6-hour TTL cache in `weather_cache` table displaying "last updated X ago" when offline or serving from cache.
- **14-Day Tempograph Canvas Chart (`TempographChart.kt`)**: Custom Jetpack Compose Canvas rendering:
  - Smooth cubic Bézier min-to-max temperature gradient ribbons.
  - Precipitation vertical bars colored by rain intensity.
  - Interactive touch scrubbing with dashed inspection lines and tooltip overlays.
  - "Today" demarcation line separating historical records from 7-day predictions.

### C. Weather-Informed Disease Risk Engine (`DiseaseRiskEngine.kt`)
- Pure Kotlin engine unit-tested against verified agronomic thresholds stored in `disease_risk_rules.json` and `disease_info.json`.
- Evaluates consecutive and cumulative days exceeding temperature, humidity, rainfall, and leaf-wetness criteria.
- Outputs 3-tier risk (`HIGH`, `MODERATE`, `LOW`) with localized explanations in Kannada, Hindi, Marathi, and English.
- Serves as a secondary advisory signal on Result screens without overriding ML image predictions.
- Surfaces a "Disease Alerts" card on the Home screen for crops in the farmer's "My Crops" profile.

### D. Karnataka Crop Directory & Agronomic Knowledge Assets
- **`crops.json`**: Comprehensive dataset of 26 Karnataka crops across 8 categories (cereals, millets, pulses, oilseeds, vegetables, fruits, plantation, cash crops) with sowing windows, soil pH, temperature/water requirements, Karnataka agro-climatic zones, and growth stage timelines.
- **`disease_info.json`**: 36 major crop diseases in Karnataka with local names, symptoms, biological/cultural prevention, chemical treatments with active ingredients, doses per official university packages of practices (UAS Bengaluru / UAS Dharwad / ICAR), and pre-harvest intervals.
- **`districts.json`**: 84 administrative districts covering Karnataka (31 districts listed first) and major agricultural states across India with verified coordinate centroids.
- **`fertilizer_rules.json`**: University package of practice NPK dosages, split timing by crop growth stage, and fertilizer product conversion (Urea, DAP, MOP, SSP).

### E. My Farm Land Parcels & Activity Logs (Database v3)
- **Room Database v3**:
  - `PlotEntity` (table `plots`): Land parcel records with custom area units (Acre, Guntha, Hectare), sowing dates, soil types, and irrigation systems.
  - `FarmLogEntity` (table `farm_logs`): Chronological log of farm operations (`SOWING`, `FERTILIZER`, `IRRIGATION`, `SPRAY`, `OBSERVATION`, `HARVEST`).
  - Strict migration `MIGRATION_2_3` without destructive schema fallbacks.
- **UI Screen (`FarmScreen.kt`)**:
  - Plot cards featuring Days After Sowing (DAS) ring progress gauge mapped against the crop's duration.
  - Chronological timeline of farm operations.
  - One-click CSV export dispatched directly to the Android System Share Sheet (`Intent.ACTION_SEND`).

### F. Multilingual Knowledge Assistant & Android TTS (`AssistantScreen.kt`)
- 2-Tier Knowledge Assistant:
  - **Tier A (Offline)**: Pure Kotlin BM25 search over bundled agronomic knowledge (crops, diseases, glossary, fertilizer rules) with Kannada, Hindi, Marathi, and English tokenization.
  - **Tier B (Connected)**: Online fallback advisory activated when network is present in the `connected` flavor.
- Full integration with Android `TextToSpeech` for hands-free audio read-aloud of agronomic guidance.

### G. Background Sync & Push Notifications (`WeatherSyncWorker.kt`)
- Scheduled via AndroidX `WorkManager` on a 6-hour interval with `NetworkType.CONNECTED` constraint.
- Evaluates weather disease risks against the farmer's active "My Crops".
- Dispatches system push alerts via notification channel `agrodoc_disease_alerts` when high disease risks are detected.

### H. ML Pipeline, Model Fallback & Calibration
- Pipeline scripts: `ml/train_export.py`, `ml/prepare_data.py`, `ml/evaluation.py`, and interactive Jupyter notebook `ml/AgroDoc_Training_Colab.ipynb`.
- Model Card: documented in `docs/MODEL_CARD.md`.
- Graceful Fallback: When on-device weights are unbundled, the camera and analyzer gracefully catch the state and direct the user to the manual crop pathology knowledge library.

---

## 3. Verification & Test Metrics

### Unit Tests
| Test Suite Class | Tests Run | Result | Notes |
|:---|:---:|:---:|:---|
| `OfflineManifestTest` | 2 | PASSED | Asserts 0 network permissions in offline flavor |
| `FertilizerAdvisorTest` | 4 | PASSED | Area unit conversions, NPK calculations, soil-test adjustments |
| `CropSuitabilityEngineTest` | 3 | PASSED | Temperature, rainfall, and soil type matching |
| `DiseaseRiskEngineTest` | 4 | PASSED | Weather threshold risk evaluations |
| `TrustScorerTest` | 4 | PASSED | Trust tier thresholds and out-of-distribution handling |
| `KnowledgeAssetValidationTest` | 3 | PASSED | Validates 26 crops, 36 diseases, 84 district coordinates |
| **Total** | **20** | **100% PASSED** | Verified on both `offline` and `connected` variants |

### Binary & Packaging Validation
- **Offline APK**: `app/build/outputs/apk/offline/debug/app-offline-debug.apk`
  - Permissions: `CAMERA`, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
  - Zero network, zero location.
- **Connected APK**: `app/build/outputs/apk/connected/debug/app-connected-debug.apk`
  - Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `ACCESS_COARSE_LOCATION`, `CAMERA`, `WAKE_LOCK`.

### Live Emulator Validation
- Device: `emulator-5554` (Pixel 8 / Medium Phone running Android 15 / API 35).
- Package: `com.agrodoc.app.connected`
- Verified Screens:
  1. Home Screen with Live Weather Header & 14-Day Tempograph Canvas Chart.
  2. Crops Directory with search and category filter chips.
  3. My Farm screen with Plot management and activity logs.
  4. Knowledge Assistant with multi-turn Q&A and TTS audio playback.
  5. Settings screen with Indic language switcher (Kannada, Hindi, Marathi, English).
