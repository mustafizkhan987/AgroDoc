package com.agrodoc.app.domain.engine

import com.agrodoc.app.domain.model.DailyForecast
import com.agrodoc.app.domain.model.DiseaseInfo
import com.agrodoc.app.domain.model.DiseaseRisk
import com.agrodoc.app.domain.model.RiskLevel
import com.agrodoc.app.domain.model.WeatherData

object DiseaseRiskEngine {

    /**
     * Evaluates disease risk across a 7-day forecast for a single disease.
     */
    fun evaluateRisk(
        disease: DiseaseInfo,
        weather: WeatherData?,
        langCode: String = "en",
    ): DiseaseRisk {
        val diseaseName = when (langCode.lowercase()) {
            "kn" -> disease.nameKn.ifEmpty { disease.nameEn }
            "hi" -> disease.nameHi.ifEmpty { disease.nameEn }
            "mr" -> disease.nameMr.ifEmpty { disease.nameEn }
            else -> disease.nameEn
        }

        if (weather == null || weather.daily.isEmpty()) {
            return DiseaseRisk(
                diseaseId = disease.id,
                diseaseName = diseaseName,
                cropId = disease.cropKey,
                riskLevel = RiskLevel.LOW,
                reason = getNoWeatherReason(langCode),
                drivingDays = emptyList(),
            )
        }

        val condition = disease.favourableConditions
        val drivingDays = mutableListOf<Int>()
        var consecutiveFavourableDays = 0
        var maxConsecutiveDays = 0

        weather.daily.forEachIndexed { index, day ->
            if (isDayFavourable(day, condition)) {
                drivingDays.add(index)
                consecutiveFavourableDays++
                if (consecutiveFavourableDays > maxConsecutiveDays) {
                    maxConsecutiveDays = consecutiveFavourableDays
                }
            } else {
                consecutiveFavourableDays = 0
            }
        }

        val totalFavourable = drivingDays.size
        val riskLevel = when {
            maxConsecutiveDays >= 3 || totalFavourable >= 4 -> RiskLevel.HIGH
            maxConsecutiveDays >= 2 || totalFavourable >= 2 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        val reason = buildRiskReason(
            riskLevel = riskLevel,
            favourableDaysCount = totalFavourable,
            conditionDesc = condition.descriptionEn,
            langCode = langCode,
            disease = disease,
        )

        return DiseaseRisk(
            diseaseId = disease.id,
            diseaseName = diseaseName,
            cropId = disease.cropKey,
            riskLevel = riskLevel,
            reason = reason,
            drivingDays = drivingDays,
        )
    }

    /**
     * Evaluates risks for multiple crops (e.g. farmer's "My Crops" list).
     * Returns list sorted by risk level (HIGH first), then crop name.
     */
    fun evaluateRisksForCrops(
        cropIds: Set<String>,
        allDiseases: List<DiseaseInfo>,
        weather: WeatherData?,
        langCode: String = "en",
    ): List<DiseaseRisk> {
        val targetDiseases = allDiseases.filter { disease ->
            cropIds.any { it.equals(disease.cropKey, ignoreCase = true) }
        }

        return targetDiseases.map { disease ->
            evaluateRisk(disease, weather, langCode)
        }.sortedWith(
            compareByDescending<DiseaseRisk> { it.riskLevel.ordinal }
                .thenBy { it.cropId }
                .thenBy { it.diseaseName }
        )
    }

    private fun isDayFavourable(day: DailyForecast, cond: com.agrodoc.app.domain.model.DiseaseWeatherCondition): Boolean {
        // Temperature check:
        val tempMin = cond.tempMinC ?: 10.0
        val tempMax = cond.tempMaxC ?: 38.0
        val avgTemp = (day.tempMinC + day.tempMaxC) / 2.0
        val tempMatches = avgTemp in tempMin..tempMax

        // Humidity check:
        val reqHum = cond.humidityMinPct ?: 70
        val humidityMean = (day.humidityMinPct + day.humidityMaxPct) / 2
        val humMatches = humidityMean >= reqHum

        // Rain condition check:
        val rainMatches = if (cond.rainFavourable) {
            day.precipitationMm > 1.0 || day.precipitationProbabilityPct >= 50
        } else {
            true
        }

        // Leaf wetness proxy:
        val wetnessMatches = if (cond.leafWetnessHoursMin != null && cond.leafWetnessHoursMin > 0) {
            day.highHumidityHoursEstimate >= cond.leafWetnessHoursMin
        } else {
            true
        }

        return tempMatches && humMatches && (rainMatches || wetnessMatches)
    }

    private fun buildRiskReason(
        riskLevel: RiskLevel,
        favourableDaysCount: Int,
        conditionDesc: String,
        langCode: String,
        disease: DiseaseInfo,
    ): String {
        val condText = when (langCode.lowercase()) {
            "kn" -> disease.favourableConditions.descriptionKn.ifEmpty { disease.favourableConditions.descriptionEn }
            "hi" -> disease.favourableConditions.descriptionHi.ifEmpty { disease.favourableConditions.descriptionEn }
            "mr" -> disease.favourableConditions.descriptionMr.ifEmpty { disease.favourableConditions.descriptionEn }
            else -> disease.favourableConditions.descriptionEn
        }

        return when (langCode.lowercase()) {
            "kn" -> when (riskLevel) {
                RiskLevel.HIGH -> "ಹೆಚ್ಚಿನ ಅಪಾಯ: ಮುಂದಿನ 7 ದಿನಗಳಲ್ಲಿ $favourableDaysCount ದಿನಗಳು ಈ ರೋಗಕ್ಕೆ ಪೂರಕವಾಗಿವೆ ($condText)."
                RiskLevel.MODERATE -> "ಮಧ್ಯಮ ಅಪಾಯ: $favourableDaysCount ದಿನಗಳು ರೋಗ ಹರಡುವ ಸಾಧ್ಯತೆ ಹೊಂದಿವೆ."
                RiskLevel.LOW -> "ಕಡಿಮೆ ಅಪಾಯ: ಪ್ರಸ್ತುತ ಹವಾಮಾನವು ಈ ರೋಗಕ್ಕೆ ಪೂರಕವಾಗಿಲ್ಲ."
            }
            "hi" -> when (riskLevel) {
                RiskLevel.HIGH -> "उच्च जोखिम: अगले 7 दिनों में $favourableDaysCount दिन इस रोग के अनुकूल हैं ($condText)।"
                RiskLevel.MODERATE -> "मध्यम जोखिम: आगामी $favourableDaysCount दिन रोग फैलने के आसार हैं।"
                RiskLevel.LOW -> "कम जोखिम: वर्तमान मौसम इस रोग के अनुकूल नहीं है।"
            }
            "mr" -> when (riskLevel) {
                RiskLevel.HIGH -> "उच्च धोका: पुढील ७ दिवसांपैकी $favourableDaysCount दिवस या रोगासाठी अनुकूल आहेत ($condText)."
                RiskLevel.MODERATE -> "मध्यम धोका: $favourableDaysCount दिवस रोग वाढीसाठी पोषक वातावरण आहे."
                RiskLevel.LOW -> "कमी धोका: सध्याचे हवामान या रोगासाठी अनुकूल नाही."
            }
            else -> when (riskLevel) {
                RiskLevel.HIGH -> "High Risk: $favourableDaysCount days in the next 7 days favour disease development ($condText)."
                RiskLevel.MODERATE -> "Moderate Risk: Weather conditions on $favourableDaysCount days are conducive."
                RiskLevel.LOW -> "Low Risk: Weather conditions do not strongly favour this disease right now."
            }
        }
    }

    private fun getNoWeatherReason(langCode: String): String = when (langCode.lowercase()) {
        "kn" -> "ಹವಾಮಾನ ಮಾಹಿತಿ ಲಭ್ಯವಿಲ್ಲ — ಅಪಾಯ ಲೆಕ್ಕಹಾಕಲಾಗಿಲ್ಲ."
        "hi" -> "मौसम की जानकारी उपलब्ध नहीं — जोखिम गणना नहीं की जा सकी।"
        "mr" -> "हवामान माहिती उपलब्ध नाही — धोका तपासता आला नाही."
        else -> "Weather data unavailable — risk calculation paused."
    }
}

