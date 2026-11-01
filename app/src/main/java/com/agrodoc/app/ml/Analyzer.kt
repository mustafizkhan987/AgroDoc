package com.agrodoc.app.ml

import android.content.Context
import android.graphics.Bitmap
import com.agrodoc.app.domain.model.Prediction
import com.agrodoc.app.domain.model.Scan
import com.agrodoc.app.domain.model.TrustLevel
import kotlin.math.exp

sealed class AnalysisResult {
    data class Success(val scan: Scan) : AnalysisResult()
    data class MissingModel(val message: String) : AnalysisResult()
    data class Failed(val error: String) : AnalysisResult()
}

/**
 * High-level image analyzer combining:
 * 1. [ModelRunner] for neural network inference.
 * 2. [CamGenerator] for Class Activation Mapping (CAM) and leaf focus calculation.
 * 3. [TrustScorer] for multi-factor diagnostic trust scoring.
 */
class Analyzer(ctx: Context) {

    private val modelRunner = ModelRunner(ctx)

    fun isModelReady(): Boolean = modelRunner.isModelLoaded()

    fun analyze(
        src: Bitmap,
        langCode: String = "en",
        explanation: String = "",
    ): AnalysisResult {
        when (val result = modelRunner.runInference(src)) {
            is ModelRunnerResult.Error -> {
                return if (result.isMissingModel) {
                    AnalysisResult.MissingModel(result.message)
                } else {
                    AnalysisResult.Failed(result.message)
                }
            }
            is ModelRunnerResult.Success -> {
                val logits = result.logits
                val n = result.labels.size
                val mx = logits.maxOrNull() ?: 0f
                val e = FloatArray(n) { exp(logits[it] - mx) }
                val z = e.sum().coerceAtLeast(1e-6f)
                val p = FloatArray(n) { e[it] / z }
                val order = p.indices.sortedByDescending { p[it] }
                val c = order[0]
                val margin = if (n > 1) p[c] - p[order[1]] else p[c]

                // CAM computation
                val camResult = CamGenerator.generateCam(
                    feats = result.features,
                    weights = result.camWeights,
                    classIndex = c,
                    numClasses = n,
                    px = result.pixels,
                    targetOverlaySize = 512,
                )

                // Trust calculation
                val trust = TrustScorer.score(
                    confidence = p[c],
                    margin = margin,
                    leafFocus = camResult.leafFocus,
                    leafAreaRatio = camResult.leafAreaRatio,
                )

                val top1 = Prediction(
                    classId = c,
                    label = result.labels[c],
                    prob = p[c],
                )
                val top3 = order.take(3).map {
                    Prediction(
                        classId = it,
                        label = result.labels[it],
                        prob = p[it],
                    )
                }

                // Flatten 7x7 raw CAM to FloatArray(49)
                val rawCamFlat = FloatArray(49)
                for (i in 0..6) {
                    for (j in 0..6) {
                        rawCamFlat[i * 7 + j] = camResult.rawCam[i][j] / camResult.camMax
                    }
                }

                val scaledPhoto = Bitmap.createScaledBitmap(result.squareBitmap, 512, 512, true)

                val scan = Scan(
                    photo = scaledPhoto,
                    overlay = camResult.overlay,
                    rawCam = rawCamFlat,
                    top1 = top1,
                    top3 = top3,
                    trust = trust,
                    leafFocus = camResult.leafFocus,
                    leafAreaRatio = camResult.leafAreaRatio,
                    camClassId = c,
                    explanationText = explanation,
                    modelVersion = "v1.0",
                    languageAtScan = langCode,
                )

                return AnalysisResult.Success(scan)
            }
        }
    }
}
