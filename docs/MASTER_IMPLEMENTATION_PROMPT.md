# AgroDoc — End-to-End Completion Prompt

Use this prompt with a coding agent that has the AgroDoc repository open. Treat the repository as the source of truth and work in place.

## Objective

Finish AgroDoc as a usable Android agronomy assistant for farmers in Karnataka, with multilingual, offline-first plant disease guidance, crop growing information, fertilizer planning, and optional weather forecasts for locations across India. Complete the existing implementation; do not replace working architecture with a new project.

## Start with an audit

Read `docs/ANALYSIS.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/DATA_SOURCES.md`, `docs/DATASETS.md`, `docs/BACKLOG.md`, the Android source, assets, Gradle configuration, and manifests. Compare each report claim with files actually present. Record the real state in `docs/ANALYSIS.md` before coding. Preserve existing user changes and do not assume the progress report is current.

## Product requirements

1. **India weather**: support manual state/district selection using the bundled district coordinates and Open-Meteo forecast in the connected flavor. Keep the offline flavor free of internet permissions and network calls. Cache forecasts, show stale/cache/empty/error states, and use weather only as a secondary disease-risk signal. Do not require or expose a secret API key for Open-Meteo; if another provider is introduced, keep credentials out of source control and document setup.
2. **Crop guide**: let farmers search and select crops, save My Crops, and see localized growth stages, sowing windows, temperature/rainfall needs, soil type/pH, district fit, current suitability, common diseases, and links to disease and fertilizer guidance.
3. **Karnataka disease library**: cover common diseases for the supported crops. Every record must state crop, symptoms, cause, spread, prevention, treatment, weather conditions where known, model-support status, source, and review status. Never claim a model can diagnose a disease unless that class exists in the bundled model labels. Mark translated or unverified material as draft.
4. **Diagnosis and explanation**: camera and gallery photos should run local inference when a model is installed. Handle missing model, bad image, low confidence, and unsupported crop honestly. Show top predictions, confidence/trust and limitations; provide a heatmap only when available. Explain likely causes from grounded crop/disease/weather data. Weather cannot override image evidence.
5. **Multilingual assistant**: provide grounded local Q&A in Kannada, Hindi, Marathi, and English, using the bundled knowledge base and the selected language. Clearly say when the answer is unavailable and refer the farmer to a local Raitha Samparka Kendra/agriculture officer. Add on-device speech output with a clear unavailable-voice fallback; do not send photos or location to a server.
6. **Fertilizer planner**: select crop, cultivated area/unit, optional soil test values, and growth stage. Use the existing fertilizer rules and advisor, show per-area product amounts, stage schedule, organic options, micronutrient notes, source, and safety disclaimer. Persist generated plans in Room.
7. **Navigation and settings**: wire all destinations and deep links between home, crops, crop detail, library, disease detail, assistant, fertilizer, camera, result, history, settings, and about. Settings should persist language, state/district, voice, text size, and heatmap preference. Provide attribution, privacy and model limitations.
8. **Quality and docs**: keep Android UI strings localized across all four locales, ensure accessible labels and loading/empty/error states, retain Room migrations, and update README plus final implementation and agronomy review reports. Validate JSON structure/source fields and verify offline/connected manifests and build variants. Do not fabricate training metrics or agronomic validation.

## Implementation rules

- Follow `docs/DECISIONS.md` and `docs/ARCHITECTURE.md`; pure domain logic stays independent of Android.
- Inspect and reuse existing code before adding a duplicate model, repository, or engine.
- Preserve the offline/connected flavor boundary. Only the connected weather implementation may perform network I/O.
- Every agronomic dose, threshold, or recommendation must retain traceable source attribution and suitable review status. Do not present a computed general recommendation as a substitute for soil testing or professional advice.
- Never silently substitute placeholder UI for an unfinished feature. Where a resource (especially trained model assets) is unavailable, show a useful, honest fallback.
- Keep scope focused on requirements above. Do not add analytics, ads, remote inference, or cloud storage.

## Completion checklist

- All required user flows are reachable and functional, including crop detail, disease detail, assistant, fertilizer plan, image selection/diagnosis, history, settings, and weather refresh/cache behavior.
- All visible copy is localized in the four supported languages with matching string keys.
- Knowledge assets and their provenance/review status are checked; unsupported inference classes are clearly distinguished from informational disease entries.
- Offline and connected manifests honor the network boundary, and Room migrations preserve existing user data.
- Automated checks and both build variants are run; failures and unavailable external prerequisites (such as missing trained TFLite assets) are reported accurately.
- `docs/ANALYSIS.md`, `docs/AGRONOMY_REVIEW.md`, `docs/FINAL_REPORT.md`, `docs/README.md`/root README, and backlog reflect actual outcomes rather than intended work.
- Finish with a concise summary of changed files, completed flows, checks performed, known limitations, and anything requiring agronomist/native-speaker/model review.
