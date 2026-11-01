package com.agrodoc.app.domain.engine

import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.SowingWindow
import org.junit.Assert.assertEquals
import org.junit.Test

class CropSuitabilityEngineTest {
    private val crop = CropInfo("sample", "Sample", "ಮಾದರಿ", "नमूना", "नमुना", category = "cereal", seasons = listOf("kharif"), tempMinC = 12.0, tempOptMinC = 20.0, tempOptMaxC = 30.0, tempMaxC = 38.0, sowingWindows = listOf(SowingWindow("kharif", "June, July", "October", null)))

    @Test fun goodMonthAndWeatherAreSuitable() {
        val weather = com.agrodoc.app.domain.model.WeatherData("test", 1L, com.agrodoc.app.domain.model.CurrentConditions(25.0, 60, 0, 0.0, 5.0), emptyList())
        val result = CropSuitabilityEngine.evaluate(crop, null, weather, currentMonthIndex = 5)
        assertEquals(SuitabilityStatus.GOOD, result.status)
    }

    @Test fun outsideWindowAndExtremeWeatherIsNotRecommended() {
        val weather = com.agrodoc.app.domain.model.WeatherData("test", 1L, com.agrodoc.app.domain.model.CurrentConditions(45.0, 60, 0, 0.0, 5.0), emptyList())
        val result = CropSuitabilityEngine.evaluate(crop, null, weather, currentMonthIndex = 0)
        assertEquals(SuitabilityStatus.NOT_RECOMMENDED, result.status)
    }
}
