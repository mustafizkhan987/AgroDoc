# ML dataset ledger

This ledger separates facts shown on a dataset's source page from permission to train or distribute with it. The source pages below were checked on 2026-11-02. No dataset was downloaded into this repository. Human review of the exact downloaded version and license terms is still required before training or release.

| Dataset | Candidate / version | Source-reported license and contents | Intended role | Status |
|---|---|---|---|---|
| PlantVillage color | `mohanty/PlantVillage`; pin an immutable revision when downloading | Dataset card lists CC BY-SA 3.0, 38 class labels, `train`/`test` splits, and a `leaf_id` field. The current card exposes classes used by the 23-class AgroDoc subset. | Lab training; preserve provided train/test and leaf grouping. | Source card checked; license compatibility and revision still need human sign-off. |
| PlantDoc | `pratikkayal/PlantDoc-Dataset` | Repository lists CC BY 4.0 and provides `train` and `test` directories. Its README describes 2,598 images across 13 species and up to 17 disease classes. | Field evaluation / mapped field training. | Source checked; exact overlap mapping and image reuse license need human sign-off. |
| Rice Leaf Disease Image Samples | Mendeley V2, DOI `10.17632/fwcj7stb8r.2` | Dataset page lists CC BY 4.0 and 5,932 images covering bacterial blight, blast, brown spot, and tungro. | Rice field-domain candidate. | Manual download; inspect source files and grouping metadata before use. |
| RiceLeafDiseaseBD | Mendeley V2, DOI `10.17632/86s4jzj2m4.2` | Current V2 page lists CC BY 4.0, 9,769 smartphone field images, six classes, and image metadata. This differs from the older 2,545-image description in the build prompt. | Rice field evaluation candidate. | Ask the human to choose and pin a version; inspect annotations and duplicates. |
| Sugarcane Leaf Dataset | Mendeley V1, DOI `10.17632/355y629ynj.1` | Dataset page lists CC BY 4.0, 6,748 images, nine disease categories, plus healthy and dried leaves. | Sugarcane field-domain candidate. | Manual download; inspect class folders and exact duplicates before mapping. |
| SAR-CLD-2024 cotton | Mendeley V2, DOI `10.17632/b3jy2p6k8w.2` | Page lists CC BY 4.0 and 2,137 original images plus 7,000 augmented images, but its text conflicts on seven versus eight classes. | Cotton field-domain candidate. | Use originals only; human must inspect class folders and exclude pre-augmented copies from validation/test. |
| Not-a-leaf backgrounds | No source selected yet. | No license or class list has been verified. | Open-set negative examples. | Select and approve a source before adding examples. |

## Source links

- [PlantVillage dataset card](https://huggingface.co/datasets/mohanty/PlantVillage)
- [PlantDoc dataset repository and license](https://github.com/pratikkayal/PlantDoc-Dataset)
- [Rice Leaf Disease Image Samples V2](https://data.mendeley.com/datasets/fwcj7stb8r/2)
- [RiceLeafDiseaseBD V2](https://data.mendeley.com/datasets/86s4jzj2m4/2)
- [Sugarcane Leaf Dataset V1](https://data.mendeley.com/datasets/355y629ynj/1)
- [SAR-CLD-2024 V2](https://data.mendeley.com/datasets/b3jy2p6k8w/2)

## Folder and manifest contract

Keep downloaded data under `ml/data/` (ignored by Git). A local folder source uses `dataset/split/class_name/image.ext`; `split` is `train`, `validation`, or `test`. The indexer writes `manifest.csv` with source path, dataset ID, source label, class ID, crop, disease ID, split, group ID, group-ID source, and SHA-256. Preserve an upstream split when supplied. For custom splits, provide leaf/source group IDs and assign every member of a group to one split before augmentation. Do not use augmented copies in validation or test. PlantDoc aliases in `classes.yaml` are provisional and must be reviewed; `--skip-unmapped` explicitly excludes source classes outside the current model map.

The indexer fails on unrecognized classes and on a group crossing splits. Do not add a dataset's class mapping until a human has inspected its downloaded class folders and confirmed the license/version. The open-set background mapping remains unpopulated until an approved source is selected.
