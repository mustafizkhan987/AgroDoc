AgroDoc — Complete Build Prompt v2
All features requested so far, one gated build plan, and an analysis of the conditions that can make it fail. Extends docs/ARCHITECTURE.md (v1). Where the two conflict, this document wins.
0. How to run this in Antigravity
1. Save this file as docs/MASTER_PROMPT_V2.md. Update .agent/rules/agrodoc.md to say: follow both documents; the offline flavor must never contain the INTERNET permission; never invent agronomy numbers; every agronomy fact carries source and verified fields.
2. Open the existing repo (Phase 0 is built). Use Planning mode and paste this kickoff:
Read docs/ARCHITECTURE.md and docs/MASTER_PROMPT_V2.md, then inspect the repo. Do not rewrite working code. Execute Phases 1 to 10 in order. At every HUMAN GATE stop, print exactly what I must do, and wait. After each phase build both flavors, run tests, update docs/BACKLOG.md, summarise the changes and list anything you could not verify.
3. “One go” means one prompt executed as gated phases. An agent cannot train on your GPU, create accounts, hold API keys, or certify agronomy facts, so Section 7 lists the steps only you can do.
1. Current state and corrections
• Done (Phase 0): compiles with Gradle 8.14.6 / SDK 35, runs on an emulator; Compose + Material 3, Navigation, Hilt, Room, DataStore, CameraX, LiteRT dependency, 4 locales (84 keys each), bottom navigation, 7 screens, 23-class scope (Tomato, Potato, Corn, Grape, Pepper).
• Not done: the model files do not exist yet (ml/train_export.py has not been run), so there is no real inference; trust thresholds are uncalibrated placeholders; Analyzer.kt is monolithic.
• Fix in README and project summary: (a) the trust rating is not calibrated until Phase 2 finishes; (b) train_export.py is TensorFlow/Keras only; (c) replace “100% offline” with: the offline build declares no INTERNET permission; the connected build adds optional weather and advisor features.
2. Decisions that override v1
• D1 Two product flavors. offline (review/approval build, no INTERNET permission, no network code paths) and connected (adds <uses-permission INTERNET> and coarse location only through src/connected/AndroidManifest.xml). Shared code talks to interfaces (WeatherProvider, AdvisorEngine); the offline flavor binds manual/offline implementations, the connected flavor binds network ones. CI must run aapt dump permissions on the offline APK and fail if INTERNET appears.
• D2 Scope expansion is deliberate. v1 excluded treatment advice and Review-0 excluded treatment recommendations. Fertilizer guidance and prevention advice are now in scope. Pesticide/fungicide names, brands and doses stay out of scope. The app tells the farmer to consult the local agriculture officer or Krishi Vigyan Kendra (KVK) for chemical control.
• D3 Evidence over claims. No accuracy, risk or fertilizer number may be shown unless it is computed by a script in the repo or carries a cited source. The app and docs must say “unverified” where applicable.
• D4 Two kinds of crops. image_model: true crops get photo diagnosis. knowledge_only crops (for which no suitable image dataset has been verified, e.g. ragi, arecanut, coffee) get the symptom guide, weather risk, crop requirements, fertilizer and advisor, but no photo diagnosis.
3. Feature specifications
3.1 Disease model v2 (photo → plant condition → disease)
Goal: the best honest accuracy on real field photos, not the highest number on lab images.
• Datasets (all free; record licence, URL, class mapping and download date in ml/DATASETS.md; check each licence before use):
    ◦ PlantVillage (lab, 23-class v1 scope) — Hugging Face mohanty/PlantVillage or Kaggle.
    ◦ PlantDoc (field photos, CC BY 4.0, ships with a train/test split) — GitHub pratikkayal/PlantDoc-Dataset.
    ◦ Rice: “Rice Leaf Disease Image Samples” (Mendeley, 5,932 images; bacterial blight, blast, brown spot, tungro) and RiceLeafBD (Mendeley, 2,545 field images; blast, narrow brown spot, sheath blight, tungro, normal).
    ◦ Sugarcane: “Sugarcane Leaf Dataset” (6,748 images, nine diseases + healthy + dried).
    ◦ Cotton: the 2,137-image field-survey cotton leaf disease dataset (Data in Brief, 2024).
    ◦ Mendeley downloads need a manual browser download (HUMAN GATE); the agent writes the loader and the folder layout, not the download.
• Class config: ml/classes.yaml lists every class with crop, disease_id, source_dataset, image_model. Adding a crop must need no Kotlin change.
• Pipeline:
    1. Crop-aware inference: if the farmer selected a crop (Section 3.3), restrict the softmax to that crop’s classes. This is the cheapest large accuracy gain and must be on by default.
    2. A not_a_leaf class (random non-plant backgrounds with free licences) plus an open-set reject rule (max-softmax and energy score, thresholds calibrated) so unknown things return “cannot tell” instead of a wrong disease.
    3. Backbone bake-off on one script: MobileNetV2, MobileNetV3-Large, EfficientNet-Lite; choose by field-set macro-F1 and phone latency, not PlantVillage accuracy.
    4. Augmentation: flips, rotation, colour jitter, blur, random crops, cluttered/real background replacement. Split by leaf/source before augmenting (no leakage).
    5. Two-stage fine-tuning; temperature scaling on a validation split; test-time augmentation (flip) as a setting.
    6. Export contract unchanged from v1 (logits + last conv features + cam_weights.bin); add model_meta.json with crop list, thresholds, metrics, dataset hashes.
• Report (reports/metrics.json + figures): per-dataset accuracy and macro-F1, per-class recall, confusion matrix, ECE, lab-vs-field drop (train on PlantVillage, test on PlantDoc), reject-rule AUROC, trust-score AUROC, deletion/insertion faithfulness, on-device latency and size. Internal goal: macro-F1 ≥ 0.95 on held-out lab data; for field data report the real number and improve it with data, not with wording. If a goal is missed, say so in MODEL_CARD.md.
• Plant condition beyond disease: the KB (3.4) includes nutrient_deficiency, pest_damage and abiotic_stress entries. Only classes with a verified image dataset get an image head; others appear as “other possible causes” in text.
3.2 Weather module (“tempograph”) and weather-based disease risk
Interpretation used here: “tempograph” = a temperature/weather graph. Thermal-camera imaging is not supported by ordinary phones and is out of scope.
• Interface: WeatherProvider.get(lat, lon, pastDays = 7, forecastDays = 7): WeatherBundle (hourly temperature, relative humidity, precipitation, cloud cover; daily min/max/rain sum).
• Providers:
    ◦ OpenMeteoProvider (connected, default, no API key; free for non-commercial use per the provider’s terms — re-check the terms on open-meteo.com before any commercial release; show attribution).
    ◦ OpenWeatherMapProvider (connected, optional; API key read from local.properties as OWM_API_KEY into BuildConfig; the file is git-ignored; never hard-code or commit a key; the free tier has a daily call limit).
    ◦ ManualWeatherProvider (offline flavor): farmer enters today’s temperature, humidity feel (dry/normal/humid) and whether it rained in the last 3 days.
• All-India location: GPS (coarse) in the connected flavor, or a bundled districts.json picker (state, district, centroid lat/lng for all India; build it from an official source such as LGD/Census, record the source, mark unverified until spot-checked).
• Caching: Room WeatherCache with a 3-hour TTL; always show “updated at”; offline fallback shows the last cache with a stale badge.
• Tempograph UI: a Canvas-drawn 14-day chart (7 past + 7 forecast): temperature band, humidity line, rainfall bars, today marker, tap-to-inspect. No chart library needed.
• Disease risk engine: assets/disease_risk_rules.json holds, per disease, temp_c_range, min_rh_percent, min_hours_rh_per_day, min_rainy_days, window_days, source, verified. Do not invent thresholds: fill only from cited extension/peer-reviewed sources (ICAR, state agricultural universities, TNAU Agritech portal, journals) and leave the rule absent if none is found. The engine returns risk 0..1, LOW/MEDIUM/HIGH and reasons in four languages (for example “humidity stayed high on 4 of the last 7 days, which favours this disease”).
• Use in the app:
    1. Result screen: a “Weather says” strip next to the photo-based result (evidence, not a verdict).
    2. Crops tab: “Risk this week” per crop.
    3. Connected flavor: daily WorkManager check and a notification when a rule reaches HIGH.
    4. Experimental re-ranking (setting, OFF by default): p' ∝ p · (1 + β · risk) with a small configurable β; never changes top-1 when p1 ≥ 0.90; logs both rankings locally. It may be enabled by default only after My Farm data (3.7) shows it improves confirmed cases. No public dataset pairs leaf photos with weather, so this cannot be validated today.
3.3 Crops tab (crop category, weather and soil needs)
• Categories: Cereals and millets, Pulses, Oilseeds, Cash crops, Plantation, Vegetables, Fruits.
• First list (Karnataka focus, to be confirmed with an expert): Rice, Ragi, Maize, Jowar, Tur, Groundnut, Sunflower, Cotton, Sugarcane, Tomato, Potato, Chilli, Onion, Grape, Pomegranate, Banana, Mango, Coconut, Arecanut, Coffee.
• assets/crops.json per crop: names in 4 languages, category, seasons (Kharif/Rabi/Summer), duration, suitable temperature, rainfall, soil type, soil pH, drainage, sowing window by Karnataka agro-climatic zone, growth stages with days-after-sowing ranges, linked disease ids, image_model, source, verified. All numeric values must come from cited sources (e.g. UAS Bengaluru/Dharwad package of practices, ICAR crop pages); never from model memory.
• Crop page: icon, one-line summary, requirement cards (weather, soil), growth-stage timeline, common diseases, “Is it suitable now?” traffic light comparing current weather with the crop’s range, “Add to My Farm”, “Ask Advisor”. Selecting a crop sets the active crop used by the scanner.
3.4 Knowledge base (diseases, reasons, terms)
• assets/diseases.json per entry: id, crop, names (4 languages, including common local names such as koleroga for arecanut where verified), type (fungal, bacterial, viral, nutrient, pest, abiotic), symptom text and symptom terms, reason (what causes it and why it appears), favourable conditions (linked to risk rules), how it spreads, prevention through cultural practices (rotation, resistant varieties, spacing, sanitation, water management), when to call an expert, image_model, source, verified.
• Candidate disease list to research and verify per crop (do not trust it as is): Rice: blast, brown spot, bacterial leaf blight, sheath blight, tungro. Ragi: blast. Maize: northern leaf blight, common rust, gray leaf spot. Cotton: bacterial blight, leaf curl. Sugarcane: red rot, smut, rust, mosaic, yellow leaf disease, grassy shoot. Tur: wilt, sterility mosaic. Groundnut: leaf spot (tikka), rust. Arecanut: koleroga, yellow leaf disease. Coffee: leaf rust. Tomato: early blight, late blight, leaf curl virus. Potato: early and late blight. Chilli: leaf curl, anthracnose. Grape: downy mildew, powdery mildew. Pomegranate: bacterial blight. Banana: sigatoka, panama wilt. Mango: powdery mildew, anthracnose.
• assets/glossary.json: agriculture terms in 4 languages with a one-line plain explanation (chlorosis, necrosis, lesion, halo, blight, rust pustule, mildew, mosaic, leaf curl, wilting, stunting, canker, dieback, damping-off). Symptom terms in results are tappable chips that open the glossary.
• Drafts only: mark every translation needs_review: true; generate docs/TRANSLATION_REVIEW.md.
3.5 Advisor (multilingual “plant growth and disease” assistant)
Scope: crop growth stages, what a disease is, why it occurs, prevention, interpreting weather, explaining the fertilizer schedule, using the app. Out of scope: pesticide names/doses, medical or legal advice, anything not in the KB (answer “I am not sure, please ask your KVK”).
• Tier A (both flavors, default): grounded offline assistant. Normalise the question per language, detect intent by keywords, retrieve the best KB entries (BM25 over per-language text), and compose the answer from templates with the KB text. Voice input through Android SpeechRecognizer with the offline-preferred flag where installed, voice output through TTS. No generation, so no hallucination.
• Tier B (connected flavor only, explicit opt-in in Settings): LLM advisor. Interface AdvisorEngine; the provider is configurable with a key in local.properties. Retrieval-augmented: send only the top-k KB passages, the active crop and stage, the latest scan result, a weather summary and the user’s language. The system prompt must force: answer only from supplied passages, reply in the user’s language in under 120 words, cite KB ids, refuse pesticide dosing, add the disclaimer, say “not sure” when passages do not cover the question. No data is stored remotely by the app. Keys inside an APK can be extracted: for anything beyond a demo, move calls behind a small proxy you control.
• Tier C (optional research spike, not required): a small on-device LLM. Check current model size and Kannada quality on your target phones first; only continue if a 6 GB-RAM phone gives acceptable answers.
• Evaluation: a 100-question test set (25 per language), reviewed by native speakers and an agriculture expert for correctness and groundedness; record the pass rate in reports/advisor_eval.md.
3.6 Fertilizer guidance
• Rule-based engine over assets/fertilizer_plans.json: per crop and stage, the recommended nutrient doses (N, P₂O₅, K₂O per acre and per hectare), split timing in days after sowing/transplanting, organic inputs, with source and verified fields. Values come only from cited state-university or ICAR recommendations.
• Inputs: crop, variety (optional), plot area (acre/hectare/guntha), sowing date, irrigated or rain-fed, and optional soil test values (Soil Health Card: N, P, K rating, pH, organic carbon). With soil test data the engine adjusts the dose using the cited low/medium/high correction rules; without it the app shows the general dose and says a soil test gives a better recommendation.
• Conversion to products using fixed content percentages: urea 46% N; DAP 18% N and 46% P₂O₅; MOP 60% K₂O; SSP 16% P₂O₅. Show bags and kilograms for the plot area, accounting for the N and P that DAP already supplies.
• Weather-aware advice: if the forecast shows heavy rain in the next 48 hours (threshold taken from a cited source), suggest postponing top-dressing. Always show “follow your local agriculture officer’s advice”.
• Never recommend a brand, never recommend chemical pesticide doses.
3.7 My Farm (monitor and update crop condition and fertilizer)
• Plots: name, crop, variety, area, sowing/transplanting date, location (district or GPS), irrigation type, optional soil test, photo.
• Plot dashboard: growth ring showing the current stage and days after sowing; today’s weather card; next fertilizer due; open risks; last scan with trust badge; health-trend sparkline from past scans.
• Timeline log (FarmLog): scans (auto-added), observations (symptom chips plus a note plus a photo), fertilizer applications (product, quantity, date, method), irrigation, rainfall, other work, weather snapshot. Each entry editable and deletable.
• Reminders: fertilizer splits from the schedule, re-scan after 7 days if a disease was found, weekly check. Use WorkManager and local notifications; work offline.
• Condition update: the farmer can mark a scan result as “confirmed / not correct” with a corrected disease. This builds a private on-device record for future validation (3.2 item 4, model improvement). Provide Export to CSV/JSON and a shareable PDF/report through the Android share sheet; export is manual and consented.
• Privacy: all data stays on the device; nothing leaves unless the user shares an export.
3.8 Settings (ease of use)
• Language: app language, separate voice language, first-run picker re-openable.
• Simple mode: large icons, fewer options, one-tap Scan; guided tutorial that can be replayed.
• Accessibility: text size (S/M/L/XL), high-contrast theme, colour-blind-safe heatmap, reduce-motion, haptics on/off, large touch targets.
• Voice: read results aloud automatically, speech rate, mic input on/off.
• Display: light / dark / auto; “outdoor mode” (maximum contrast for sunlight).
• Units: acre / hectare / guntha, °C / °F, kg / quintal, mm.
• Location: use GPS or choose state and district.
• Weather: provider (Open-Meteo / OpenWeatherMap), update frequency, Wi-Fi-only, data saver, clear cache. Visible only in the connected flavor.
• Scanning: use selected crop to narrow results (default on), test-time augmentation, keep original photos, quality warnings on/off, experimental weather re-ranking (default off).
• Notifications: reminders, weather risk alerts, quiet hours.
• Data: backup/restore to a local file, export reports, clear history, delete all data.
• About: model card (datasets, metrics, limits), licences and credits (PlantVillage, PlantDoc CC BY 4.0, Open-Meteo attribution, fonts), version, feedback by email intent.
3.9 UI design system “Living Field” (cool but light)
• Mood: the app’s header gradient and a subtle animated sky reflect the current weather (sunny, cloudy, rain, night). Pure Compose animation, no heavy assets.
• Palette: Dawn Field (deep green #1B5E20, leaf #43A047, sun #FFB300, sky #4FC3F7, soil #6D4C41, surface #F4FBF4); Night Field dark theme (surface #0F1F14). All text must pass 4.5:1 contrast.
• Home: weather hero card with a 7-day sparkline and one-line farming tip; a “Risk radar” row of chips per crop; My Farm crop cards with growth rings; a large pulsing Scan button in the centre of the bottom bar; bottom tabs Home, Crops, Scan, My Farm, Advisor; Settings and language in the top bar.
• Scan: leaf-shaped guide overlay with a moving scan line; live quality ring (blur, light, leaf size) that turns green when ready; shutter with haptic.
• Result: an X-ray reveal: drag a handle across the leaf to wipe between the photo and the heatmap; trust shown as a gauge arc; symptom-term chips with glossary pop-ups; weather strip; “Why it happens” and “How to prevent” accordions; voice button; Save to My Farm.
• Motion: shared-element transitions between a card and its detail, skeleton loaders, spring animations; every animation respects the reduce-motion setting.
• Typography: Noto Sans Kannada and Noto Sans Devanagari with a rounded Latin face (OFL licences), line height at least 1.5× for Indic scripts.
• Performance budget: smooth on a 2 GB phone; no blur/glass effects on API < 31; APK under 40 MB for the offline flavor (check size after adding assets).
4. Data model and navigation additions
• Room entities: Plot, FarmLog, FertApplication, Reminder, WeatherCache; extend ScanEntity with plotId, userConfirmedClassId, weatherSnapshotJson, modelVersion. Provide Room migrations and migration tests.
• Read-only JSON assets (validated at build time by a Gradle/unit test that checks the schema, language coverage and that every verified:false item is flagged): crops, diseases, glossary, disease_risk_rules, fertilizer_plans, districts, trust_config.
• Routes: home, crops, crop/{id}, scan, result/{scanId}, farm, plot/{id}, plot/{id}/log, fertilizer/{plotId}, advisor, settings, settings/{section}, about.
5. Phases and acceptance criteria
Phase 1 — ML pipeline v2 (HUMAN GATE: GPU run, dataset downloads). Loaders for every dataset in 3.1, classes.yaml, backbone bake-off, calibration, reject rule, export, metrics. Accept: reports/metrics.json and MODEL_CARD.md exist; the three model assets load in Python and give the same logits as Keras; field-drop reported.
Phase 2 — Core refactor and calibration. Split Analyzer into ModelRunner, CamGenerator, LeafMask, TrustScorer, ImageQualityChecker; calibrate thresholds from Phase 1 data. Accept: golden-output tests match Python; both flavors build; INTERNET check passes.
Phase 3 — Capture. CameraX live viewfinder with quality ring, gallery picker, EXIF fix. Accept: scan works in airplane mode on a low-end device.
Phase 4 — Result v2 and knowledge base. X-ray reveal, gauge, glossary chips, disease KB screen, TTS. Accept: every image-model class has a KB entry in 4 languages with needs_review flagged.
Phase 5 — Weather and risk (connected + offline providers). Providers, cache, tempograph, district picker, risk engine, alerts. Accept: fake-provider unit tests for the risk engine; offline flavor uses manual input and has no network classes in its dependency graph.
Phase 6 — Crops tab. Catalogue, crop page, suitability check, active-crop constraint wired to the model. Accept: choosing a crop restricts predictions in a test.
Phase 7 — My Farm and fertilizer. Plots, timeline, reminders, fertilizer engine with unit-conversion tests, export. Accept: creating a plot yields a dated schedule; reminders fire on a device; export opens in a spreadsheet.
Phase 8 — Advisor. Tier A retrieval with voice; Tier B behind the connected flavor and an opt-in switch. Accept: the 100-question evaluation runs and reports; refusal tests for pesticide dosing pass.
Phase 9 — Settings and UI polish. Everything in 3.8 and 3.9, accessibility pass (TalkBack, 200% font, colour-blind check). Accept: each setting changes behavior and persists; reduce-motion disables animations.
Phase 10 — Hardening and release candidate. Tests (unit, Compose UI, Room migrations), performance on a 2 GB device, R8 shrink, signed APKs for both flavors, TEST_PLAN.md, MODEL_CARD.md, PRIVACY.md. Accept: offline APK has no INTERNET permission; APK size and cold-start time reported.
6. Review-ready minimum (if time is short)
Phases 1–4 plus the Crops tab (6) already give a convincing review demo: real model, reveal heatmap, trust, KB, crop-constrained diagnosis. Weather, My Farm, fertilizer and advisor are valuable but each needs verified data; ship them as clearly labelled “beta” if their data is not yet reviewed.
7. HUMAN GATES (the agent must stop and ask)
1. Run ml/train_export.py / v2 pipeline on a Kaggle or Colab GPU; download Mendeley/Kaggle datasets in a browser; confirm licences.
2. Provide API keys through local.properties (OpenWeatherMap, advisor provider). Never paste keys into chat or commit them.
3. Agronomy review of crops.json, diseases.json, disease_risk_rules.json, fertilizer_plans.json by someone qualified (agriculture university student/faculty, KVK, ICAR scientist) before the values are shown as verified.
4. Native-speaker review of Kannada, Hindi and Marathi texts.
5. Field photos of Karnataka crops (ragi, arecanut, coffee, etc.) with permission from farmers/KVK if you want image diagnosis for them.
6. Decide whether the project will ever be distributed commercially (changes weather-API terms and liability).
8. Analysis of conditions and risks
#
Condition
Why it matters
Resolution in this plan
1
Offline claim vs weather/LLM features
Network features need the INTERNET permission, which breaks the approval claim
Two flavors (D1); CI permission check
2
“Accurate, high result”
PlantVillage lab accuracy is inflated; field photos score lower
Field test on PlantDoc, crop-constrained inference, background augmentation, calibration, reject rule, honest reporting
3
Karnataka crops lack image data
PlantVillage has no ragi, rice, sugarcane, cotton, arecanut, coffee
Add verified rice/sugarcane/cotton datasets; knowledge_only crops; collect your own field set with KVK/UAS
4
Weather helps diagnosis?
Plausible agronomically but unproven for this app; no paired dataset
Show as evidence, re-ranking OFF by default, validate with My Farm confirmed cases
5
Agronomy accuracy and liability
Wrong fertilizer/disease advice harms farmers
Sourced values only, verified flags, expert review gate, no pesticide doses, disclaimers
6
LLM hallucination and Indic quality
Fluent but wrong answers in Kannada/Marathi
Tier A offline default; Tier B grounded by KB, refusals, native-speaker evaluation
7
API key exposure and cost
Keys in an APK can be extracted; limits apply
Keyless Open-Meteo default; local.properties; proxy for production
8
Open-Meteo terms
Free tier is for non-commercial use
Fine for student project; re-check terms and pay/switch before any commercial release; attribute
9
Translations
Machine drafts can mislead with agronomy terms
needs_review, review checklist, human gate
10
Device limits
More models, charts and animations can slow 2 GB phones
Performance budget, reduce-motion, size checks, flavor-based assets
11
Privacy
Photos, location, farm records are personal
Local-only storage, GPS optional, manual export, PRIVACY.md
12
Scope vs timeline
Ten phases exceed one review cycle
Section 6 minimum; beta labels
13
Documentation accuracy
Summary says trust is “calibrated” and model is ready
Corrections in Section 1
9. Rules for the agent
• Inspect the repo first; extend rather than rewrite; small commits; tests for every engine.
• Never fabricate metrics, thresholds, agronomy values, translations, dataset statistics or sources. Unknown means a clearly marked placeholder plus an entry in docs/BACKLOG.md.
• Never add network code to the offline flavor, never add analytics or ads, never commit keys.
• Respect licences and show credits in About.
• After each phase print: what changed, how to run it, what you could not verify, and the next human gate.