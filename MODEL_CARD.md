# AgroDoc image model card

## Status

**Not trained; not calibrated; no model weights are bundled.** This file is a status template until the Phase 1 human gate and acceptance criteria are complete. It must not be read as evidence that photo diagnosis works.

## Intended scope

The initial image-model class map contains 23 PlantVillage classes across corn, grape, pepper, potato, and tomato, plus a reserved `not_a_leaf` open-set class. This is a planned scope only. New rice, sugarcane, and cotton classes require reviewed class mappings before they can be trained.

## Data and permissions

See [the dataset ledger](ml/DATASETS.md). No datasets have been downloaded or approved in this checkout. Source-page license labels do not replace human review of the exact version and intended use.

## Evaluation

No accuracy, macro-F1, calibration, open-set, trust, faithfulness, model-size, or device-latency values are available. See [reports/metrics.json](reports/metrics.json), which explicitly records `not_run` until results exist.

## Known limitations

- No trained TFLite model, labels, CAM weights, or model metadata are bundled.
- The trust-scoring thresholds in the Android app are placeholders.
- The not-a-leaf data source is not selected.
- EfficientNet-Lite training and its compatible feature-map adapter are not implemented yet.
- Target-phone latency, real field performance, dataset licensing, and expert review remain pending.

## Intended-use restrictions

Until a reviewed model and domain validation are complete, the model is not ready to guide farm decisions. The app must label unsupported or unverified outputs clearly and direct farmers to a local agriculture officer or KVK for expert help.
