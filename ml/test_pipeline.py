import tempfile
import unittest
from pathlib import Path

import numpy as np

from evaluation import (
    calibrate_reject_rules,
    calibrate_temperature,
    energy_score,
    evaluate_logits,
    evaluate_open_set,
    evaluate_reject_thresholds,
    lab_field_macro_f1_drop,
    restrict_to_crop,
    softmax,
)
from prepare_data import folder_rows, load_classes, prepare_plantvillage, split_training_groups


class ClassConfigTests(unittest.TestCase):
    def test_class_ids_are_stable_and_include_open_set_class(self):
        rows, _ = load_classes()
        self.assertEqual([row["class_id"] for row in rows], list(range(24)))
        self.assertEqual(rows[-1]["disease_id"], "not_a_leaf")
        self.assertEqual(sum(row["image_model"] for row in rows), 24)


class ManifestTests(unittest.TestCase):
    def test_folder_indexer_records_classes_splits_and_hashes(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            samples = {
                "train/Tomato___Early_blight/one.jpg": b"training image one",
                "train/Tomato___Early_blight/two.jpg": b"training image two",
                "test/Tomato___Early_blight/three.jpg": b"test image three",
            }
            for relative, content in samples.items():
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(content)
            rows = folder_rows(root, "fixture")
            self.assertEqual(len(rows), 3)
            self.assertEqual({row["split"] for row in rows}, {"train", "test"})
            self.assertTrue(all(row["disease_id"] == "tomato_early_blight" for row in rows))
            self.assertTrue(all(len(row["sha256"]) == 64 for row in rows))

    def test_group_split_moves_whole_groups_to_validation(self):
        rows = [
            {"group_id": "g1", "split": "train", "sample": 1},
            {"group_id": "g1", "split": "train", "sample": 2},
            {"group_id": "g2", "split": "train", "sample": 3},
            {"group_id": "g3", "split": "train", "sample": 4},
            {"group_id": "g4", "split": "train", "sample": 5},
        ]
        result = split_training_groups(rows, validation_fraction=0.25, seed=7)
        by_group = {}
        for row in result:
            by_group.setdefault(row["group_id"], set()).add(row["split"])
        self.assertTrue(all(len(splits) == 1 for splits in by_group.values()))
        self.assertIn("validation", {row["split"] for row in result})

    def test_unknown_label_fails_closed(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            image = root / "train/Unreviewed_class/image.jpg"
            image.parent.mkdir(parents=True)
            image.write_bytes(b"image")
            with self.assertRaisesRegex(ValueError, "unmapped class"):
                folder_rows(root, "fixture")

    def test_reviewed_source_aliases_map_plantdoc_overlaps(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            samples = {
                "train/Corn Gray leaf spot/one.jpg": b"corn gray spot",
                "test/Corn rust leaf/two.jpg": b"corn rust",
                "train/Apple Scab Leaf/unmapped.jpg": b"excluded class",
            }
            for relative, content in samples.items():
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(content)
            rows = folder_rows(root, "plantdoc", skip_unmapped=True)
            self.assertEqual({row["class_id"] for row in rows}, {0, 1})
            self.assertEqual({row["source_label"] for row in rows}, {"Corn Gray leaf spot", "Corn rust leaf"})

    def test_plantvillage_loader_requires_an_immutable_revision(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaisesRegex(ValueError, "immutable"):
                prepare_plantvillage("main", Path(temp))


class EvaluationTests(unittest.TestCase):
    def test_crop_filter_keeps_only_crop_and_open_set(self):
        classes = [
            {"class_id": 0, "crop": "tomato"},
            {"class_id": 1, "crop": "potato"},
            {"class_id": 2, "crop": "unknown"},
        ]
        values = restrict_to_crop(np.array([[1.0, 4.0, 2.0]]), classes, "tomato", open_set_id=2)
        self.assertTrue(np.isneginf(values[0, 1]))
        self.assertEqual(values[0, 0], 1.0)
        self.assertEqual(values[0, 2], 2.0)

    def test_temperature_calibration_and_metrics_are_finite(self):
        logits = np.array([[4.0, 0.0], [0.0, 3.0], [2.5, 0.0], [0.0, 2.5]])
        labels = np.array([0, 1, 0, 1])
        temperature = calibrate_temperature(logits, labels)
        result = evaluate_logits(labels, logits / temperature, [0, 1], ["a", "b"])
        self.assertGreater(temperature, 0.0)
        self.assertEqual(result["accuracy"], 1.0)
        self.assertEqual(len(result["confusion_matrix"]), 2)
        self.assertGreaterEqual(result["ece"], 0.0)

    def test_open_set_calibration_uses_known_and_unknown_validation(self):
        known = np.array([[6.0, 0.0], [0.0, 6.0], [5.0, 0.0]])
        unknown = np.array([[0.2, 0.1], [0.0, 0.3], [0.1, 0.0]])
        thresholds = calibrate_reject_rules(known, unknown)
        self.assertIn("max_softmax_minimum", thresholds)
        self.assertIn("energy_maximum", thresholds)
        self.assertGreater(energy_score(unknown).mean(), energy_score(known).mean())
        self.assertTrue(np.allclose(softmax(known).sum(axis=1), 1.0))

    def test_open_set_auroc_and_field_drop(self):
        known = np.array([[6.0, 0.0], [0.0, 6.0], [5.0, 0.0]])
        unknown = np.array([[0.2, 0.1], [0.0, 0.3], [0.1, 0.0]])
        metrics = evaluate_open_set(known, unknown)
        self.assertEqual(metrics["max_softmax_reject_auroc"], 1.0)
        self.assertEqual(metrics["energy_reject_auroc"], 1.0)
        self.assertAlmostEqual(lab_field_macro_f1_drop(0.91, 0.64), 0.27)

    def test_validation_calibrated_reject_rule_is_applied_to_test_scores(self):
        known = np.array([[6.0, 0.0], [0.0, 6.0]])
        unknown = np.array([[0.2, 0.1], [0.0, 0.3]])
        thresholds = calibrate_reject_rules(known, unknown)
        metrics = evaluate_reject_thresholds(known, unknown, thresholds)
        self.assertEqual(metrics["known_reject_rate"], 0.0)
        self.assertEqual(metrics["unknown_reject_rate"], 1.0)
        self.assertEqual(metrics["balanced_accuracy"], 1.0)


if __name__ == "__main__":
    unittest.main()
