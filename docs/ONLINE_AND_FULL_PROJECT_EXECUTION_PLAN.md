# AgroDoc — Complete Project & Full Online Architecture Plan

**Generated**: November 15, 2026  
**Reference Documents**: `docs/MASTER_PROMPT_V2.md`, `docs/ARCHITECTURE.md`, `.agent/rules/agrodoc.md`

---

## 1. Current State & Audit

| Component | Status | Verification Detail |
| :--- | :--- | :--- |
| **Gradle & Flavors** | ✅ Configured | `offline` (strict zero-network) & `connected` (online edition with INTERNET permission). |
| **Build Status** | ✅ Passing | Both `assembleOfflineDebug` and `assembleConnectedDebug` compile cleanly without errors. |
| **Unit Tests** | ✅ Passing | `testOfflineDebugUnitTest` and `testConnectedDebugUnitTest` both succeed. |
| **Database** | ✅ Room v2 | Entities: `ScanEntity`, `WeatherCacheEntity`, `MyCropEntity`, `FertilizerPlanEntity`. |
| **Knowledge Base** | ✅ 4 Locales | 26 Karnataka crops (`crops.json`), 36 diseases (`disease_info.json`), 26 fertilizer rules (`fertilizer_rules.json`), 84 districts (`districts.json`). |
| **Localization** | ✅ 100% Parity | Exactly 148 string keys across `en`, `kn`, `hi`, and `mr`. |
| **Live Emulator** | ✅ Verified | App installed and active on `emulator-5554` (`mCurrentFocus=MainActivity`). |

---

## 2. "Full Online App" Strategy (`connected` Flavor)

To fulfill the user request of making AgroDoc a **full online app** while upholding the architectural invariant that the `offline` flavor contains zero network permissions:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        AgroDoc Architecture                           │
├──────────────────────────────────┬─────────────────────────────────────┤
│   `offline` Flavor (Strict)      │     `connected` Flavor (Online)     │
├──────────────────────────────────┼─────────────────────────────────────┤
│ • Zero INTERNET permission       │ • `INTERNET` & `ACCESS_NETWORK_...` │
│ • Manual weather data entry      │ • Live Open-Meteo API (+ OWM key)   │
│ • Local Room persistence only    │ • WorkManager 6-hr weather sync     │
│ • Tier-A Offline BM25 Assistant  │ • Real-time disease push alerts     │
│ • 100% On-device ML & CAM        │ • Tier-B Connected AI Assistant     │
│ • Zero outbound telemetry        │ • GPS/Network Location auto-detect  │
└──────────────────────────────────┴─────────────────────────────────────┘
```

### Key Online Features to Enable in `connected`:
1. **Live Weather Provider (Open-Meteo & OpenWeatherMap)**:
   - Continuous real-time forecast queries via Retrofit with 3-hour cache TTL.
   - Dual-provider strategy: Open-Meteo (default, keyless, CC BY 4.0) with fallback to OpenWeatherMap via `BuildConfig.OPENWEATHER_API_KEY`.
2. **WorkManager Background Sync & High-Risk Disease Alerts**:
   - `WeatherSyncWorker` scheduled every 6 hours, constrained to `NetworkType.CONNECTED`.
   - Automatically computes `DiseaseRiskEngine.evaluateRisksForCrops()` on incoming forecasts.
   - Fires system notifications when disease conditions reach `HIGH` for crops in "My Farm".
3. **Online Knowledge Assistant (Tier B)**:
   - When connected, farmers can opt into the online AI advisor (`ConnectedAdvisorEngine`).
   - Grounded RAG architecture: passes top-3 retrieved knowledge base entries as ground truth context to prevent hallucination.
   - Hard system bounds: under 120 words, strictly in user's language, zero chemical pesticide dosing (advises consulting local RSK/KVK).
4. **Auto-Location Resolution**:
   - Coarse location service resolving device coordinates to the closest centroid in `districts.json`.
   - Manual state/district picker always available as fallback.

---

## 3. Comprehensive Implementation Roadmap

### Phase A: Data Layer & Online Engine Extensions
1. **WorkManager & Background Sync**:
   - Implement `com.agrodoc.app.data.worker.WeatherSyncWorker`.
   - Implement `com.agrodoc.app.data.notification.DiseaseAlertNotifier`.
2. **My Farm & Plot Architecture**:
   - Create Room entities: `PlotEntity`, `FarmLogEntity`, `ReminderEntity`.
   - Create DAOs: `PlotDao`, `FarmLogDao`, `ReminderDao`.
   - Add Room `MIGRATION_2_3`.
3. **Glossary & Risk Rule Assets**:
   - Create `app/src/main/assets/knowledge/glossary.json` (15 agricultural terms in 4 languages).
   - Create `app/src/main/assets/knowledge/disease_risk_rules.json` (verified weather thresholds).

### Phase B: UI & Feature Screens
1. **Camera & Live Scanner (`CameraScreen.kt`)**:
   - CameraX integration with real-time brightness, blur, and leaf size quality ring.
   - Gallery picker launcher (`ActivityResultContracts.GetContent`).
2. **Diagnostic Result Screen (`ResultScreen.kt`)**:
   - Interactive X-Ray slider wiping between original leaf photo and 7×7 Grad-CAM heatmap overlay.
   - Trust rating gauge (High, Medium, Low) with breakdown explanation.
   - Secondary weather factor badge ("Weather favours this disease" / "Weather does not favour; consider re-scanning").
   - "Why Did This Happen?" accordion.
   - Tappable symptom chips opening glossary dialog.
   - Text-To-Speech (TTS) read-aloud button.
3. **Tempograph Weather Screen & Canvas Chart**:
   - Custom 14-day interactive Canvas chart (7 days historical + 7 days forecast).
   - Temperature band, humidity curve, precipitation bars, today indicator.
4. **My Farm & Timeline (`FarmScreen.kt` & `PlotDetailScreen.kt`)**:
   - Plot cards with growth stage rings (days after sowing).
   - Fertilizer due dates and timeline logs (`FarmLog`).
   - Export farm report to CSV/JSON via Android Share Sheet.
5. **Crop Category & Detail (`CropDetailScreen.kt`)**:
   - Sowing window traffic light (Good / Marginal / Not Recommended).
   - Climate and soil requirements.
   - Growth stages timeline.
   - Quick links to Disease Library and Fertilizer Schedule.
6. **Fertilizer Calculator (`FertilizerScreen.kt`)**:
   - Plot area input with unit conversions (Acre, Hectare, Guntha).
   - Soil test calibration adjustments.
   - Product schedule: DAP, Urea, MOP in kg and bags.
   - Organic manure (FYM, Vermicompost) advice and weather rain alerts.
7. **Knowledge Assistant (`AssistantScreen.kt`)**:
   - Dual-tier assistant: Tier A (offline BM25 retrieval) + Tier B (online connected assistant).
   - Question prompt chips, source citations, TTS speech output.
8. **Settings & Living Field Polish (`SettingsScreen.kt`)**:
   - Online/Offline mode indicator.
   - Language selector (Kannada default, Hindi, Marathi, English).
   - State -> District picker with Karnataka 31 districts prioritized.
   - Text size, High-contrast outdoor mode, color-blind safe heatmap palette.

### Phase C: Machine Learning & Acceptance
1. **Model & Metadata Export**:
   - Verify `ml/classes.yaml`, `ml/model_meta.json`.
   - Generate mobile assets (`agrodoc_model.tflite`, `cam_weights.bin`, `labels.json`).
2. **Quality & Validation Tests**:
   - `OfflineManifestTest`: Verifies offline APK manifest contains no `INTERNET` permission.
   - `FertilizerAdvisorTest`: Unit test for area units and soil calibrations.
   - `DiseaseRiskEngineTest`: Unit test for weather-based disease risk calculations.
   - `CropSuitabilityEngineTest`: Unit test for sowing windows.
   - `KnowledgeAssetValidationTest`: Validates JSON schema integrity and translation coverage.
3. **Documentation**:
   - Create `docs/MODEL_CARD.md`, `docs/PRIVACY.md`, `docs/TEST_PLAN.md`.
   - Update `docs/BACKLOG.md` and `README.md`.

---

## 4. Acceptance Criteria Checklist

- [ ] `offline` APK contains 0 network permissions (`aapt dump permissions` verification).
- [ ] `connected` APK successfully fetches live Open-Meteo weather and displays 14-day forecast.
- [ ] WorkManager periodic weather risk check triggers notification on High Risk.
- [ ] CameraX capture with real-time feedback & gallery import works smoothly.
- [ ] Result screen features interactive X-Ray heatmap wipe slider and Trust gauge.
- [ ] 26 crops, 36 diseases, 15 glossary terms, 26 fertilizer schedules completely functional in 4 languages.
- [ ] My Farm plot management with timeline logging and export operates reliably.
- [ ] All unit tests pass across both `offlineDebug` and `connectedDebug`.
