# AgroDoc — Data Sources

All external data used in the knowledge base, district list, and datasets.

---

## 1. districts.json

**Source:** Datameet India community — Geocoded India Districts  
**URL:** https://github.com/datameet/maps/tree/master/Districts  
**License:** CC BY 4.0  
**Coverage:** All Indian states and union territories; ~700 districts  
**Fields used:** state name, district name, centroid latitude, centroid longitude  
**Last accessed:** 2026-11-01  
**Notes:** District boundaries and centroids are approximate. Coordinate accuracy is sufficient for weather API queries (±50 km has negligible impact on forecast accuracy).

---

## 2. PlantVillage Dataset

**Source:** Penn State University / Hughes & Salathé (2016)  
**URL:** https://github.com/spMohanty/PlantVillage-Dataset  
**License:** Non-commercial research only (Penn State terms). **Verify before any commercial distribution.**  
**Classes used:** 23 classes across Tomato, Potato, Corn, Grape, Pepper  
**Notes:** Lab-photographed images with clean backgrounds; field generalization is limited.

---

## 3. PlantDoc Dataset

**Source:** Singh et al., 2020  
**Citation:** Singh, D., Jain, N., Jain, P. et al. PlantDoc: A Dataset for Visual Plant Disease Detection. In CODS-COMAD '20. ACM, 2020.  
**License:** CC BY 4.0  
**Use:** Field-condition evaluation benchmark (not training due to smaller size)

---

## 4. Disease Knowledge Base

**Primary sources for agronomy content:**
- University of Agricultural Sciences (UAS) Bengaluru, Dharwad, and Raichur — Package of Practices
- ICAR (Indian Council of Agricultural Research) — Crop guides and advisories
- Karnataka State Department of Agriculture — Extension circulars
- AICRP (All India Coordinated Research Projects) crop reports

**Policy:** No agronomic dose, threshold, or recommendation is included without a specific citation in the `source` field of the relevant JSON entry. Unverified values are marked `review_status: draft`.

---

## 5. Open-Meteo Weather API

**URL:** https://open-meteo.com  
**License:** CC BY 4.0 (attribution required)  
**Attribution shown:** About screen → "Weather data from Open-Meteo.com (CC BY 4.0)"  
**Coverage:** Global including all of India  
**Rate limit:** 10,000 calls/day (free tier, non-commercial)  
**API key required:** No

---

## 6. Datasets to Evaluate for Track B (Karnataka Crops)

> ⚠️ The entries below are candidates. None have been confirmed for inclusion. Verify license and coverage before use.

| Dataset | Crops | License | Status |
|---------|-------|---------|--------|
| PlantDoc | 13 crops, 27 diseases | CC BY 4.0 | Candidate — evaluate |
| AI Challenger Plant Disease | Various | Terms unclear | Pending verification |
| Mendeley Paddy Disease | Paddy | CC BY 4.0 | Candidate — evaluate |
| Kaggle Cassava | Cassava only | CC BY 4.0 | Not Karnataka-primary — skip |
| iNaturalist Agricultural | Mixed | CC BY-NC | Non-commercial only |

**Rule:** Do not add any dataset to `DATASETS.md` as confirmed until its license has been read and the exact class list has been verified.

