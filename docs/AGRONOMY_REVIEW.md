# Agronomy Review Checklist

**Status:** Source attribution and review-status fields exist in the knowledge assets; independent technical review is still required before field release.

## Review before publishing recommendations

- Compare every crop's sowing window, temperature/rainfall range, soil type and pH, district suitability, and growth-stage guidance with current UAS Bengaluru/Dharwad or ICAR recommendations.
- Verify every fertilizer dose, nutrient split, product-equivalent calculation, and soil-test interpretation against the current crop package of practices and laboratory units.
- Check pesticide product registrations, formulations, dose units, application method, label restrictions, protective equipment, and pre-harvest intervals against current approved labels. Do not rely on historical citations alone.
- Confirm disease symptoms, causal organism, favourable weather thresholds, spread route, prevention, and consultation thresholds with plant pathology specialists.
- Have Kannada, Hindi, Marathi, and English crop/disease guidance reviewed by fluent agricultural translators. Preserve `reviewStatus` and `reviewedBy` values accurately.
- Confirm that only classes present in the eventual trained model are marked as model-supported, and assess the model on representative field photos before claiming diagnostic accuracy.

## Presentation safeguards implemented

- The disease guide marks entries without a supported image class as information-only.
- The explanation flow treats weather as a secondary signal and includes officer-consult guidance.
- Fertilizer output includes its source and a soil-test/agriculture-officer caution.
- The About screen does not claim model accuracy or report metrics while model artifacts are absent.

This file is a review checklist, not an agronomic approval or validation certificate.
