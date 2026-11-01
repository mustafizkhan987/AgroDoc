# AgroDoc — Architecture Reference

This document is the canonical architecture reference for the AgroDoc project.

---

## 1. Module Structure (single-module, package-based Clean Architecture)

```
com.agrodoc.app
├── AgroDocApp.kt                   Hilt Application
├── MainActivity.kt                 Single-activity host
│
├── domain/                         Pure Kotlin — no Android deps
│   ├── model/                      Data classes (Scan, Prediction, TrustLevel, DiseaseInfo, CropInfo, WeatherData, FertilizerPlan)
│   ├── repository/                 Interfaces (WeatherRepository, KnowledgeRepository, ScanRepository, FertilizerRepository)
│   └── engine/                     Pure business logic (DiseaseRiskEngine, FertilizerAdvisor, CropSuitabilityEngine, ExplanationGenerator, KnowledgeRetriever)
│
├── data/                           Android-dependent implementations
│   ├── db/                         Room database, entities, DAOs, migrations
│   ├── prefs/                      DataStore SettingsRepository
│   ├── assets/                     KnowledgeAssetLoader (reads JSON from assets/)
│   └── weather/                    WeatherRepository impl (connected flavor), NoOpWeatherRepository (offline flavor)
│
├── ml/                             On-device ML
│   ├── ModelRunner.kt              LiteRT interpreter wrapper (Phase 2)
│   ├── CamGenerator.kt             CAM computation (Phase 2)
│   └── TrustScorer.kt              Trust rating logic (Phase 2)
│
├── di/                             Hilt modules
│   ├── AppModule.kt                Room, DataStore, common
│   ├── WeatherModule.kt            Flavor-specific (offline/connected)
│   └── KnowledgeModule.kt          KnowledgeRepository binding
│
└── ui/
    ├── navigation/                 NavGraph, NavRoutes, MainScaffold
    ├── screens/                    One package per screen
    │   ├── onboarding/             LanguageScreen, LanguageViewModel
    │   ├── home/                   HomeScreen, HomeViewModel
    │   ├── scan/                   CameraScreen, CameraViewModel (Phase 3)
    │   ├── result/                 ResultScreen, ResultViewModel (Phase 4)
    │   ├── crops/                  CropsScreen, CropDetailScreen, CropsViewModel
    │   ├── library/                LibraryScreen, DiseaseDetailScreen, LibraryViewModel
    │   ├── fertilizer/             FertilizerScreen, FertilizerViewModel
    │   ├── assistant/              AssistantScreen, AssistantViewModel
    │   ├── history/                HistoryScreen, HistoryViewModel
    │   ├── settings/               SettingsScreen, SettingsViewModel
    │   └── about/                  AboutScreen
    └── theme/                      Theme, Typography, Color
```

---

## 2. Data Flow

```
Camera/Gallery ──► CameraViewModel ──► ModelRunner ──► CamGenerator ──► TrustScorer
                                              │
                                              ▼
                                        ScanResult ──► ResultViewModel ──► ResultScreen
                                              │
                                    ┌─────────┴──────────┐
                                    │                    │
                              ScanRepository        WeatherRepository
                              (Room)                (Open-Meteo / NoOp)
                                    │                    │
                                    └──────────┬─────────┘
                                               ▼
                                     DiseaseRiskEngine ──► Risk annotations on ResultScreen
```

---

## 3. Product Flavors

| Flavor | INTERNET | WeatherRepository | Default |
|--------|----------|-------------------|---------|
| `offline` | ABSENT | NoOpWeatherRepository | YES |
| `connected` | PRESENT (weather only) | OpenMeteoWeatherRepository | NO |

Source sets:
- `app/src/offline/` — NoOpWeatherRepositoryModule
- `app/src/connected/` — WeatherRepositoryModule, WorkManager setup

---

## 4. Database Schema (Room, version 2)

See `app/src/main/java/com/agrodoc/app/data/db/` for entity definitions.

Tables: `scans`, `weather_cache`, `my_crops`, `fertilizer_plans`

---

## 5. Key Architecture Rules

1. `domain/` has zero Android dependencies.
2. All networking goes through `WeatherRepository` interface — nothing else touches the network.
3. No analytics, crash reporting, or ad SDKs.
4. No photo or location data leaves the device.
5. Every agronomic claim in `domain/engine/` traces to a `source` field in the knowledge JSON.
6. All user-facing text is in string resources (no hardcoded strings except dev logs).

