# AgroDoc Test Plan & Quality Assurance Strategy

---

## 1. Testing Scope & Objectives

The testing strategy validates:
1. **Manifest & Permission Enforcements**: `offline` APK must never contain `INTERNET` permission, whereas `connected` APK includes declared networking capabilities.
2. **Domain & Agronomic Engines**: Pure Kotlin unit testing of `FertilizerAdvisor`, `CropSuitabilityEngine`, `DiseaseRiskEngine`, and `TrustScorer`.
3. **Asset Integrity**: Structural and coordinate validation across `crops.json`, `disease_info.json`, `districts.json`, and `fertilizer_rules.json`.
4. **Room Database Migrations**: Seamless migration sequence $1 \to 2 \to 3$ retaining user data without destructive schema fallback.
5. **UI & End-to-End Workflows**: Navigation across 5 primary tabs (Home with Tempograph, Crops, Farm, Library, Assistant), language switching across 4 Indic locales, camera scanning fallback, and CSV farm log exports.

---

## 2. Test Suites

### A. Unit Tests (`app/src/test/`)
- `com.agrodoc.app.manifest.OfflineManifestTest`:
  - `offlineManifestDeclaresNoInternetPermission`: Asserts that `offline` source manifest contains zero network permissions.
  - `connectedManifestDeclaresInternetPermission`: Asserts that `connected` source manifest declares `android.permission.INTERNET`.
- `com.agrodoc.app.domain.engine.FertilizerAdvisorTest`:
  - Tests base dosage computation for Karnataka crops (e.g. Paddy, Ragi, Sugarcane).
  - Tests unit conversions (Acre, Guntha, Hectare).
  - Tests soil-test nitrogen/phosphorus/potassium adjustments ($\pm 25\%$).
- `com.agrodoc.app.domain.engine.CropSuitabilityEngineTest`:
  - Evaluates temperature, rainfall, and soil type matching against district conditions.
- `com.agrodoc.app.domain.engine.DiseaseRiskEngineTest`:
  - Tests temperature, humidity, and rainfall thresholds for disease favorability rules.
- `com.agrodoc.app.ml.TrustScorerTest`:
  - Validates high, medium, and low trust classification logic, top-2 probability margins, and out-of-distribution handling.
- `com.agrodoc.app.data.assets.KnowledgeAssetValidationTest`:
  - Parses and validates completeness of 26 crops, 36 diseases, 84 district coordinates, and multilingual translations.

---

## 3. Database Migration Verification ($1 \to 2 \to 3$)
- **Migration 1 to 2**:
  - Adds `weather_cache`, `my_crops`, `fertilizer_plans` tables and location fields on `scan_history`.
- **Migration 2 to 3**:
  - Adds `plots` table (plot name, crop id, area, unit, sowing date, soil type, irrigation type).
  - Adds `farm_logs` table (plot id foreign key, activity type, notes, timestamp).

---

## 4. UI Manual & Automated Testing Checklist
- [x] Language onboarding and switching across Kannada, Hindi, Marathi, English.
- [x] Home screen with 14-day Tempograph canvas chart (min/max temperature band, rainfall bars, today marker).
- [x] Crop Category directory with filtering by Season (Kharif, Rabi, Summer, Perennial), Water need, and Region.
- [x] Crop Detail view with growth stage timeline, disease links, and fertilizer calculator.
- [x] My Farm screen: Add plot, view Days After Sowing (DAS), log farming activities, export CSV to Android share sheet.
- [x] Assistant screen: Multilingual Q&A with Android Text-To-Speech audio playback.
- [x] CameraX scanner: Center-crop leaf viewfinder, missing model fallback, manual disease library reference.
