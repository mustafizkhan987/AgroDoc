package com.agrodoc.app.ml

import com.agrodoc.app.domain.model.TrustLevel

data class TrustConfig(
    val focusLow: Float = 0.45f,
    val focusHigh: Float = 0.70f,
    val confLow: Float = 0.50f,
    val confHigh: Float = 0.85f,
    val margin: Float = 0.30f,
    val areaMin: Float = 0.15f,
)

object TrustScorer {

    fun score(
        confidence: Float,
        margin: Float,
        leafFocus: Float,
        leafAreaRatio: Float,
        config: TrustConfig = TrustConfig(),
    ): TrustLevel {
        return when {
            leafFocus < config.focusLow || confidence < config.confLow || leafAreaRatio < config.areaMin ->
                TrustLevel.LOW
            leafFocus >= config.focusHigh && confidence >= config.confHigh && margin >= config.margin ->
                TrustLevel.HIGH
            else -> TrustLevel.MEDIUM
        }
    }
}

