"""Evaluation and calibration helpers that do not require TensorFlow."""

from __future__ import annotations

from typing import Any, Iterable

import numpy as np
from sklearn.metrics import (
    accuracy_score,
    confusion_matrix,
    f1_score,
    recall_score,
    roc_auc_score,
    roc_curve,
)
from scipy.optimize import minimize_scalar


def softmax(logits: np.ndarray) -> np.ndarray:
    values = np.asarray(logits, dtype=np.float64)
    if values.ndim != 2 or np.isnan(values).any() or np.isposinf(values).any():
        raise ValueError("logits must be a [samples, classes] array without NaN or positive infinity")
    if not np.isfinite(values).any(axis=1).all():
        raise ValueError("every row must retain at least one finite logit")
    shifted = values - values.max(axis=1, keepdims=True)
    exp_values = np.exp(shifted)
    return exp_values / exp_values.sum(axis=1, keepdims=True)


def restrict_to_crop(
    logits: np.ndarray,
    classes: Iterable[dict[str, Any]],
    crop: str | None,
    open_set_id: int = 23,
) -> np.ndarray:
    """Mask logits to one crop while leaving the open-set class available."""
    values = np.asarray(logits, dtype=np.float64).copy()
    class_rows = list(classes)
    if values.ndim != 2:
        raise ValueError("logits must be rank 2")
    if not crop:
        return values
    keep = {
        int(row["class_id"])
        for row in class_rows
        if row.get("crop") == crop or int(row["class_id"]) == open_set_id
    }
    if not keep:
        raise ValueError(f"No model classes are configured for crop {crop!r}")
    if max(keep) >= values.shape[1]:
        raise ValueError("class configuration refers to a logit index outside the model output")
    values[:, [i for i in range(values.shape[1]) if i not in keep]] = -np.inf
    return values


def energy_score(logits: np.ndarray) -> np.ndarray:
    """Return the energy score; larger values indicate less in-distribution evidence."""
    values = np.asarray(logits, dtype=np.float64)
    maximum = values.max(axis=1, keepdims=True)
    return -(maximum[:, 0] + np.log(np.exp(values - maximum).sum(axis=1)))


def expected_calibration_error(
    y_true: np.ndarray, probabilities: np.ndarray, bins: int = 15
) -> float:
    y = np.asarray(y_true, dtype=np.int64)
    probs = np.asarray(probabilities, dtype=np.float64)
    if probs.ndim != 2 or len(y) != len(probs) or bins < 1:
        raise ValueError("invalid labels, probabilities, or bin count")
    confidence = probs.max(axis=1)
    correct = probs.argmax(axis=1) == y
    edges = np.linspace(0.0, 1.0, bins + 1)
    ece = 0.0
    for index in range(bins):
        in_bin = (confidence > edges[index]) & (confidence <= edges[index + 1])
        if in_bin.any():
            ece += in_bin.mean() * abs(correct[in_bin].mean() - confidence[in_bin].mean())
    return float(ece)


def calibrate_temperature(logits: np.ndarray, labels: np.ndarray) -> float:
    """Fit one scalar temperature on validation logits by minimizing NLL."""
    values = np.asarray(logits, dtype=np.float64)
    y = np.asarray(labels, dtype=np.int64)
    if values.ndim != 2 or len(y) != len(values) or not len(y):
        raise ValueError("temperature calibration requires non-empty validation logits and labels")
    if y.min() < 0 or y.max() >= values.shape[1]:
        raise ValueError("validation labels do not match logit columns")

    def nll(log_temperature: float) -> float:
        probs = softmax(values / np.exp(log_temperature))
        return float(-np.log(np.clip(probs[np.arange(len(y)), y], 1e-12, 1.0)).mean())

    result = minimize_scalar(nll, bounds=(-5.0, 5.0), method="bounded")
    if not result.success or not np.isfinite(result.fun):
        raise RuntimeError("temperature calibration failed")
    return float(np.exp(result.x))


def _youden_threshold(known_scores: np.ndarray, unknown_scores: np.ndarray) -> float:
    labels = np.concatenate((np.zeros(len(known_scores)), np.ones(len(unknown_scores))))
    scores = np.concatenate((known_scores, unknown_scores))
    if not len(known_scores) or not len(unknown_scores):
        raise ValueError("open-set calibration needs known and out-of-distribution validation examples")
    fpr, tpr, thresholds = roc_curve(labels, scores)
    index = int(np.argmax(tpr - fpr))
    threshold = float(thresholds[index])
    return threshold


def calibrate_reject_rules(
    known_logits: np.ndarray, unknown_logits: np.ndarray
) -> dict[str, float]:
    """Calibrate both reject signals against held-out known and unknown examples."""
    known = np.asarray(known_logits, dtype=np.float64)
    unknown = np.asarray(unknown_logits, dtype=np.float64)
    known_max = softmax(known).max(axis=1)
    unknown_max = softmax(unknown).max(axis=1)
    # ROC thresholds use larger scores for the OOD-positive class.
    max_softmax_threshold = _youden_threshold(-known_max, -unknown_max)
    known_energy = energy_score(known)
    unknown_energy = energy_score(unknown)
    energy_threshold = _youden_threshold(known_energy, unknown_energy)
    return {
        "max_softmax_minimum": float(-max_softmax_threshold),
        "energy_maximum": float(energy_threshold),
    }


def evaluate_open_set(known_logits: np.ndarray, unknown_logits: np.ndarray) -> dict[str, float]:
    """Measure AUROC for two independent open-set rejection signals."""
    known = np.asarray(known_logits, dtype=np.float64)
    unknown = np.asarray(unknown_logits, dtype=np.float64)
    if not len(known) or not len(unknown):
        raise ValueError("open-set evaluation needs known and out-of-distribution examples")
    labels = np.concatenate((np.zeros(len(known)), np.ones(len(unknown))))
    known_ood_confidence = -softmax(known).max(axis=1)
    unknown_ood_confidence = -softmax(unknown).max(axis=1)
    confidence_scores = np.concatenate((known_ood_confidence, unknown_ood_confidence))
    energy_scores = np.concatenate((energy_score(known), energy_score(unknown)))
    return {
        "max_softmax_reject_auroc": float(roc_auc_score(labels, confidence_scores)),
        "energy_reject_auroc": float(roc_auc_score(labels, energy_scores)),
    }


def evaluate_reject_thresholds(
    known_logits: np.ndarray,
    unknown_logits: np.ndarray,
    thresholds: dict[str, float],
) -> dict[str, float]:
    """Evaluate validation-calibrated thresholds on separate test examples."""
    known = np.asarray(known_logits, dtype=np.float64)
    unknown = np.asarray(unknown_logits, dtype=np.float64)
    if not len(known) or not len(unknown):
        raise ValueError("reject-rule evaluation needs known and out-of-distribution test examples")

    def is_rejected(values: np.ndarray) -> np.ndarray:
        confidence = softmax(values).max(axis=1)
        return (confidence <= thresholds["max_softmax_minimum"]) | (
            energy_score(values) >= thresholds["energy_maximum"]
        )

    known_reject_rate = float(is_rejected(known).mean())
    unknown_reject_rate = float(is_rejected(unknown).mean())
    return {
        "known_reject_rate": known_reject_rate,
        "unknown_reject_rate": unknown_reject_rate,
        "balanced_accuracy": float((1.0 - known_reject_rate + unknown_reject_rate) / 2.0),
    }


def lab_field_macro_f1_drop(lab_macro_f1: float, field_macro_f1: float) -> float:
    """Return lab minus field macro-F1; positive values indicate a field drop."""
    if not (0.0 <= lab_macro_f1 <= 1.0 and 0.0 <= field_macro_f1 <= 1.0):
        raise ValueError("macro-F1 values must be between zero and one")
    return float(lab_macro_f1 - field_macro_f1)


def evaluate_logits(
    y_true: np.ndarray,
    logits: np.ndarray,
    class_ids: Iterable[int],
    class_names: Iterable[str],
    trust_scores: np.ndarray | None = None,
) -> dict[str, Any]:
    y = np.asarray(y_true, dtype=np.int64)
    values = np.asarray(logits, dtype=np.float64)
    ids = list(class_ids)
    names = list(class_names)
    if values.shape != (len(y), len(ids)) or len(ids) != len(names):
        raise ValueError("labels, logits, and class metadata dimensions do not match")
    probs = softmax(values)
    predictions = probs.argmax(axis=1)
    recalls = recall_score(y, predictions, labels=ids, average=None, zero_division=0)
    present_ids = sorted(int(value) for value in np.unique(y))
    result: dict[str, Any] = {
        "sample_count": int(len(y)),
        "accuracy": float(accuracy_score(y, predictions)),
        "macro_f1": float(f1_score(y, predictions, labels=present_ids, average="macro", zero_division=0)),
        "macro_f1_scope": "classes_present_in_evaluation_labels",
        "per_class_recall": {name: float(value) for name, value in zip(names, recalls)},
        "confusion_matrix": confusion_matrix(y, predictions, labels=ids).tolist(),
        "ece": expected_calibration_error(y, probs),
    }
    if trust_scores is not None:
        confidence = np.asarray(trust_scores, dtype=np.float64)
        if len(confidence) != len(y):
            raise ValueError("trust score count must match labels")
        correct = (predictions == y).astype(np.int64)
        if len(np.unique(correct)) == 2:
            result["trust_score_auroc"] = float(roc_auc_score(correct, confidence))
        else:
            result["trust_score_auroc"] = None
    return result

