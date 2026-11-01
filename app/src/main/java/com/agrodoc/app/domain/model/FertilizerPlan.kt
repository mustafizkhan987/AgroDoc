package com.agrodoc.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FertilizerPlan(
    val cropId: String,
    val cropNameEn: String,
    val areaUnit: String, // "acre", "hectare", "guntha"
    val areaValue: Double,
    val growthStage: String,
    val soilType: String,
    val soilTest: SoilTest? = null,
    val schedule: List<FertilizerApplication> = emptyList(),
    val organicOptions: List<OrganicOption> = emptyList(),
    val micronutrientNotes: List<String> = emptyList(),
    val disclaimer: String = "",
    val sourceReference: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class SoilTest(
    val nitrogenKgHa: Double? = null,
    val phosphorusKgHa: Double? = null,
    val potassiumKgHa: Double? = null,
    val phLevel: Double? = null,
    val organicCarbonPct: Double? = null,
)

@Serializable
data class FertilizerApplication(
    val timing: String, // e.g. "Basal", "Top Dressing 1 (21–25 DAS)"
    val nutrientNPK: String, // e.g. "50:25:25 kg NPK/ha"
    val products: List<FertilizerProduct> = emptyList(),
    val notes: String = "",
)

@Serializable
data class FertilizerProduct(
    val name: String, // e.g. "Urea", "DAP", "MOP"
    val rateKgPerHa: Double,
    val rateForArea: Double,
    val areaUnit: String,
    val cautionNote: String? = null,
)

@Serializable
data class OrganicOption(
    val name: String, // e.g. "Farm Yard Manure (FYM)", "Vermicompost"
    val ratePerHa: String,
    val notes: String = "",
)

