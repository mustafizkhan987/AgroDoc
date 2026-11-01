package com.agrodoc.app.domain.engine

import com.agrodoc.app.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class DiseaseRiskEngineTest {
    private val disease = DiseaseInfo(
        id = "sample", cropKey = "paddy", nameEn = "Sample disease", nameKn = "ರೋಗ", nameHi = "रोग", nameMr = "रोग",
        symptomsEn = "symptom", symptomsKn = "ಲಕ್ಷಣ", symptomsHi = "लक्षण", symptomsMr = "लक्षण",
        favourableConditions = DiseaseWeatherCondition(tempMinC = 18.0, tempMaxC = 28.0, humidityMinPct = 80, rainFavourable = true, leafWetnessHoursMin = 6),
    )

    @Test fun missingWeatherDoesNotRaiseRisk() {
        assertEquals(RiskLevel.LOW, DiseaseRiskEngine.evaluateRisk(disease, null).riskLevel)
    }

    @Test fun consecutiveFavourableDaysRaiseHighRisk() {
        val days = (1..3).map { DailyForecast("2026-08-0$it", 20.0, 25.0, 80, 96, 8.0, 90, 5.0, 63, 9) }
        val weather = WeatherData("test", 1L, CurrentConditions(23.0, 90, 63, 2.0, 5.0), days)
        assertEquals(RiskLevel.HIGH, DiseaseRiskEngine.evaluateRisk(disease, weather).riskLevel)
    }
}
