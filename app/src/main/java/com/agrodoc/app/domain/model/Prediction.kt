package com.agrodoc.app.domain.model

/**
 * A single class prediction with its softmax probability.
 *
 * @param classId  Index into the model's label list.
 * @param label    Human-readable English label (e.g. "Tomato - Late Blight").
 * @param prob     Softmax probability in [0, 1].
 */
data class Prediction(
    val classId: Int,
    val label: String,
    val prob: Float,
)
