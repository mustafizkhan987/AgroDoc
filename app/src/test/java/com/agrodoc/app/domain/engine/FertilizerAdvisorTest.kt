package com.agrodoc.app.domain.engine

import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.FertilizerRule
import com.agrodoc.app.domain.model.FertilizerSplitRule
import com.agrodoc.app.domain.model.SoilTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FertilizerAdvisorTest {

    private val sampleCrop = CropInfo(
        id = "paddy",
        nameEn = "Paddy",
        nameKn = "ಭತ್ತ",
        nameHi = "धान",
        nameMr = "भात",
        category = "cereal",
        seasons = listOf("kharif"),
    )

    private val sampleRule = FertilizerRule(
        cropId = "paddy",
        recommendedNpkKgPerHa = listOf(100.0, 50.0, 50.0),
        fymTonPerHa = 10.0,
        vermicompostTonPerHa = 2.5,
        splits = listOf(
            FertilizerSplitRule(
                stage = "basal",
                nPercent = 50,
                pPercent = 100,
                kPercent = 50,
            ),
            FertilizerSplitRule(
                stage = "tillering",
                nPercent = 50,
                pPercent = 0,
                kPercent = 50,
            ),
        ),
    )

    @Test
    fun calculatePlanForOneHectareProducesValidSchedule() {
        val plan = FertilizerAdvisor.calculatePlan(
            crop = sampleCrop,
            rule = sampleRule,
            areaValue = 1.0,
            areaUnit = "hectare",
        )

        assertEquals("paddy", plan.cropId)
        assertEquals(2, plan.schedule.size)

        // Basal split supplies 100% P (50 kg P2O5). DAP is 46% P2O5 -> ~108.7 kg DAP
        val basalDap = plan.schedule[0].products.find { it.name.startsWith("DAP") }
        assertTrue(basalDap != null)
        assertTrue(basalDap!!.rateForArea > 100.0)
    }

    @Test
    fun areaUnitConversionAcreVsHectareScalesProportionately() {
        val planHa = FertilizerAdvisor.calculatePlan(
            crop = sampleCrop,
            rule = sampleRule,
            areaValue = 1.0,
            areaUnit = "hectare",
        )

        val planAcre = FertilizerAdvisor.calculatePlan(
            crop = sampleCrop,
            rule = sampleRule,
            areaValue = 2.47105, // 1 hectare = ~2.471 acres
            areaUnit = "acre",
        )

        val dapHa = planHa.schedule[0].products.first().rateForArea
        val dapAcre = planAcre.schedule[0].products.first().rateForArea
        assertEquals(dapHa, dapAcre, 1.0)
    }

    @Test
    fun soilTestLowNitrogenIncreasesNitrogenDose() {
        val standardPlan = FertilizerAdvisor.calculatePlan(
            crop = sampleCrop,
            rule = sampleRule,
            areaValue = 1.0,
            areaUnit = "hectare",
        )

        val lowNTest = SoilTest(nitrogenKgHa = 200.0) // < 280 kg/ha is Low N
        val adjustedPlan = FertilizerAdvisor.calculatePlan(
            crop = sampleCrop,
            rule = sampleRule,
            areaValue = 1.0,
            areaUnit = "hectare",
            soilTest = lowNTest,
        )

        // Low soil N should increase N recommendations (+25%)
        val stdUrea = standardPlan.schedule[1].products.find { it.name.startsWith("Urea") }?.rateForArea ?: 0.0
        val adjUrea = adjustedPlan.schedule[1].products.find { it.name.startsWith("Urea") }?.rateForArea ?: 0.0
        assertTrue("Adjusted Urea ($adjUrea) should exceed Standard Urea ($stdUrea)", adjUrea > stdUrea)
    }
}
