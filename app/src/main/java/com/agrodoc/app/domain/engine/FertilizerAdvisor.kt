package com.agrodoc.app.domain.engine

import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.FertilizerApplication
import com.agrodoc.app.domain.model.FertilizerPlan
import com.agrodoc.app.domain.model.FertilizerProduct
import com.agrodoc.app.domain.model.FertilizerRule
import com.agrodoc.app.domain.model.OrganicOption
import com.agrodoc.app.domain.model.SoilTest
import kotlin.math.max
import kotlin.math.roundToInt

object FertilizerAdvisor {

    private const val ACRES_PER_HA = 2.47105
    private const val GUNTHAS_PER_HA = 98.842

    fun calculatePlan(
        crop: CropInfo,
        rule: FertilizerRule,
        areaValue: Double,
        areaUnit: String, // "acre", "hectare", "guntha"
        growthStage: String = "all",
        soilTest: SoilTest? = null,
        langCode: String = "en",
    ): FertilizerPlan {
        val areaInHa = convertToHectares(areaValue, areaUnit)

        // Base recommended NPK (kg/ha)
        var recN = rule.recommendedNpkKgPerHa.getOrElse(0) { 0.0 }
        var recP = rule.recommendedNpkKgPerHa.getOrElse(1) { 0.0 }
        var recK = rule.recommendedNpkKgPerHa.getOrElse(2) { 0.0 }

        // Adjust for soil test if provided
        soilTest?.let { test ->
            test.nitrogenKgHa?.let { n ->
                when {
                    n < 280.0 -> recN *= 1.25 // Low N -> increase 25%
                    n > 560.0 -> recN *= 0.75 // High N -> decrease 25%
                }
            }
            test.phosphorusKgHa?.let { p ->
                when {
                    p < 10.0 -> recP *= 1.25 // Low P -> increase 25%
                    p > 25.0 -> recP *= 0.75 // High P -> decrease 25%
                }
            }
            test.potassiumKgHa?.let { k ->
                when {
                    k < 110.0 -> recK *= 1.25 // Low K -> increase 25%
                    k > 280.0 -> recK *= 0.75 // High K -> decrease 25%
                }
            }
        }

        // Generate schedule for each split
        val schedule = mutableListOf<FertilizerApplication>()
        rule.splits.forEach { split ->
            if (growthStage == "all" || split.stage.equals(growthStage, ignoreCase = true)) {
                val splitN = recN * (split.nPercent / 100.0)
                val splitP = recP * (split.pPercent / 100.0)
                val splitK = recK * (split.kPercent / 100.0)

                // Commercial fertilizer products: DAP + Urea + MOP
                val products = mutableListOf<FertilizerProduct>()

                // 1. DAP supplies 46% P2O5 and 18% N
                val dapRateHa = if (splitP > 0) splitP / 0.46 else 0.0
                val nFromDap = dapRateHa * 0.18

                if (dapRateHa > 0) {
                    products.add(
                        FertilizerProduct(
                            name = "DAP (18-46-0)",
                            rateKgPerHa = roundOneDecimal(dapRateHa),
                            rateForArea = roundOneDecimal(dapRateHa * areaInHa),
                            areaUnit = areaUnit,
                            cautionNote = null,
                        )
                    )
                }

                // 2. Remaining N via Urea (46% N)
                val remainingN = max(0.0, splitN - nFromDap)
                val ureaRateHa = if (remainingN > 0) remainingN / 0.46 else 0.0
                if (ureaRateHa > 0) {
                    products.add(
                        FertilizerProduct(
                            name = "Urea (46% N)",
                            rateKgPerHa = roundOneDecimal(ureaRateHa),
                            rateForArea = roundOneDecimal(ureaRateHa * areaInHa),
                            areaUnit = areaUnit,
                            cautionNote = rule.cautionNotes.getForLocale(langCode),
                        )
                    )
                }

                // 3. Potash via MOP (60% K2O)
                val mopRateHa = if (splitK > 0) splitK / 0.60 else 0.0
                if (mopRateHa > 0) {
                    products.add(
                        FertilizerProduct(
                            name = "MOP (Muriate of Potash 60% K2O)",
                            rateKgPerHa = roundOneDecimal(mopRateHa),
                            rateForArea = roundOneDecimal(mopRateHa * areaInHa),
                            areaUnit = areaUnit,
                            cautionNote = null,
                        )
                    )
                }

                val stageName = split.name.getForLocale(langCode)
                val instructions = split.instructions.getForLocale(langCode)
                val nutrientNPK = "N: ${roundOneDecimal(splitN)}, P₂O₅: ${roundOneDecimal(splitP)}, K₂O: ${roundOneDecimal(splitK)} kg/ha"

                schedule.add(
                    FertilizerApplication(
                        timing = stageName,
                        nutrientNPK = nutrientNPK,
                        products = products,
                        notes = instructions,
                    )
                )
            }
        }

        // Organic options
        val organicOptions = listOf(
            OrganicOption(
                name = when (langCode.lowercase()) {
                    "kn" -> "ಕೊಟ್ಟಿಗೆ ಗೊಬ್ಬರ (FYM)"
                    "hi" -> "गोबर की सड़ी खाद (FYM)"
                    "mr" -> "शेणखत (FYM)"
                    else -> "Farm Yard Manure (FYM)"
                },
                ratePerHa = "${roundOneDecimal(rule.fymTonPerHa * areaInHa)} tonnes (for $areaValue $areaUnit)",
                notes = when (langCode.lowercase()) {
                    "kn" -> "ಬಿತ್ತನೆಗೆ 2-3 ವಾರಗಳ ಮುನ್ನ ಭೂಮಿಗೆ ಹಾಕಿ ಉಳುಮೆ ಮಾಡಿ."
                    "hi" -> "बुवाई से 2-3 सप्ताह पूर्व जुताई करके मिट्टी में मिलाएँ।"
                    "mr" -> "पेरणीच्या २-३ आठवडे आधी मातीत मिसळावे."
                    else -> "Incorporate 2-3 weeks prior to sowing/transplanting."
                }
            ),
            OrganicOption(
                name = when (langCode.lowercase()) {
                    "kn" -> "ಎರೆಹುಳು ಗೊಬ್ಬರ (ವರ್ಮಿಕಾಂಪೋಸ್ಟ್)"
                    "hi" -> "केंचुआ खाद (वर्मीकम्पोस्ट)"
                    "mr" -> "गांडूळ खत (वर्मीकंपोस्ट)"
                    else -> "Vermicompost"
                },
                ratePerHa = "${roundOneDecimal(rule.vermicompostTonPerHa * areaInHa)} tonnes (for $areaValue $areaUnit)",
                notes = when (langCode.lowercase()) {
                    "kn" -> "ಬಿತ್ತನೆ ಸಾಲಿನಲ್ಲಿ ಅಥವಾ ಗಿಡದ ಬುಡಕ್ಕೆ ಹಾಕಿ."
                    "hi" -> "बुवाई की पंक्तियों में या थालों में डालें।"
                    "mr" -> "पेरणीच्या ओळीत किंवा बुंध्यापाशी द्यावे."
                    else -> "Apply in furrows or planting basins."
                }
            )
        )

        val micronutrientNotes = mutableListOf<String>()
        val microText = rule.micronutrients.getForLocale(langCode)
        if (microText.isNotEmpty()) {
            micronutrientNotes.add(microText)
        }

        val disclaimer = when (langCode.lowercase()) {
            "kn" -> "ಸೂಚನೆ: ಈ ಶಿಫಾರಸು ಕೃಷಿ ವಿಶ್ವವಿದ್ಯಾಲಯದ ಸಾಮಾನ್ಯ ಕೈಪಿಡಿಯನ್ನು ಆಧರಿಸಿದೆ. ಸ್ಥಳೀಯ ಮಣ್ಣು ಪರೀಕ್ಷೆ ಮತ್ತು ಕೃಷಿ ಅಧಿಕಾರಿಯ ಸಲಹೆ ಪಡೆಯಿರಿ."
            "hi" -> "सूचना: यह अनुशंसा कृषि विश्वविद्यालय की सामान्य मार्गदर्शिका पर आधारित है। स्थानीय मृदा परीक्षण और कृषि अधिकारी से सलाह लें।"
            "mr" -> "टीप: ही शिफारस कृषी विद्यापीठाच्या पॅकेजवर आधारित आहे. स्थानिक माती परीक्षण व कृषी अधिकाऱ्यांचा सल्ला घ्यावा."
            else -> "Notice: Based on university Package of Practices. Validate with official soil test and local Agricultural Officer (Raitha Samparka Kendra)."
        }

        val cropName = when (langCode.lowercase()) {
            "kn" -> crop.nameKn
            "hi" -> crop.nameHi
            "mr" -> crop.nameMr
            else -> crop.nameEn
        }

        return FertilizerPlan(
            cropId = crop.id,
            cropNameEn = cropName,
            areaUnit = areaUnit,
            areaValue = areaValue,
            growthStage = growthStage,
            soilType = crop.soilTypes.firstOrNull() ?: "Alluvial/Red Loam",
            soilTest = soilTest,
            schedule = schedule,
            organicOptions = organicOptions,
            micronutrientNotes = micronutrientNotes,
            disclaimer = disclaimer,
            sourceReference = rule.source,
            generatedAt = System.currentTimeMillis(),
        )
    }

    private fun convertToHectares(value: Double, unit: String): Double {
        return when (unit.lowercase().trim()) {
            "hectare", "ha", "hectares" -> value
            "acre", "acres" -> value / ACRES_PER_HA
            "guntha", "gunthas", "gunta", "guntas" -> value / GUNTHAS_PER_HA
            else -> value / ACRES_PER_HA // default to acre
        }
    }

    private fun roundOneDecimal(num: Double): Double {
        return (num * 10.0).roundToInt() / 10.0
    }
}

