package com.agrodoc.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FertilizerRule(
    val cropId: String,
    val recommendedNpkKgPerHa: List<Double> = listOf(0.0, 0.0, 0.0), // [N, P, K]
    val fymTonPerHa: Double = 10.0,
    val vermicompostTonPerHa: Double = 2.5,
    val splits: List<FertilizerSplitRule> = emptyList(),
    val micronutrients: LocalizedText = LocalizedText(),
    val cautionNotes: LocalizedText = LocalizedText(),
    val source: String = "",
)

@Serializable
data class FertilizerSplitRule(
    val stage: String,
    val name: LocalizedText = LocalizedText(),
    val nPercent: Int = 0,
    val pPercent: Int = 0,
    val kPercent: Int = 0,
    val timingDays: String = "0",
    val instructions: LocalizedText = LocalizedText(),
)

@Serializable
data class LocalizedText(
    val en: String = "",
    val kn: String = "",
    val hi: String = "",
    val mr: String = "",
) {
    fun getForLocale(langCode: String): String = when (langCode.lowercase()) {
        "kn" -> kn.ifEmpty { en }
        "hi" -> hi.ifEmpty { en }
        "mr" -> mr.ifEmpty { en }
        else -> en.ifEmpty { kn }
    }
}

