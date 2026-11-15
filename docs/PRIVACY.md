# AgroDoc Privacy Policy & Data Architecture

_Last Updated: November 2026_

AgroDoc is designed from the ground up with a privacy-by-design architecture to protect farmers' sensitive agronomic and geolocation data.

---

## 1. Product Flavor Privacy Guarantees

AgroDoc ships in two distinct build configurations to accommodate different connectivity environments:

### A. Offline Flavor (`com.agrodoc.app.offline`)
- **Zero Network Permissions**: The compiled `AndroidManifest.xml` contains NO `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE`.
- **100% On-Device Processing**:
  - Plant disease analysis runs entirely on-device via LiteRT (TensorFlow Lite).
  - Grad-CAM heatmap generation and Trust Scoring execute locally.
  - Crop knowledge, disease management guides, and fertilizer recommendations are bundled in static local JSON assets.
  - Scan history, plots, and farm logs are stored exclusively in the private app sandbox via Room SQLite.
- **Data Egress**: Physically impossible. The OS sandbox strictly denies any socket or network access.

### B. Connected Flavor (`com.agrodoc.app.connected`)
- **Restricted Network Scope**: Declares `INTERNET` and `ACCESS_NETWORK_STATE` exclusively to retrieve open weather forecasts from Open-Meteo (`api.open-meteo.com`).
- **No Leaf Scans Sent to Cloud**: Leaf images, camera previews, diagnosis logs, and user notes are strictly retained on-device and never transmitted over the network.
- **No User Tracking or Analytics**: AgroDoc does NOT integrate Google Analytics, Firebase Analytics, Crashlytics, Facebook SDK, or any advertising/behavioral tracking libraries.

---

## 2. Geolocation Privacy

- **District-Level Aggregation**: AgroDoc defaults to a manual State and District picker. All weather queries utilize pre-computed district centroids from `districts.json` rather than pinpoint GPS coordinates.
- **Optional Coarse Location**: If coarse location permission is granted for convenience, the app maps coordinates to the nearest district centroid. Precise GPS tracks are never stored or logged.

---

## 3. Storage & Data Portability

- **Local Storage**: All application data (language preferences, selected crops, farm plots, farm logs, scan records) is stored in the app's internal private storage directory (`/data/data/com.agrodoc.app/`).
- **Data Export & Erasure**:
  - Farmers can export their farm logs at any time as CSV via the Android system Share Sheet.
  - Clearing application data from Android System Settings or uninstalling the app permanently purges all local databases, images, and cached forecasts.

---

## 4. Third-Party Service Disclosure

| Service | Purpose | Data Transmitted | Flavor |
|:---|:---|:---|:---|
| **Open-Meteo API** | 7-day weather forecasts & disease risk calculation | Latitude/longitude centroid of selected district only. No IP or user identifier stored. | `connected` only |
| **Android TTS** | Text-to-Speech audio read-aloud for accessibility | On-device text string passed to system TextToSpeech service. | Both flavors |

---

## 5. Compliance with Agricultural Data Principles
AgroDoc adheres to standard fair agricultural data management principles: farmer data ownership, complete transparency, and local control.
