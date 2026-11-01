package com.agrodoc.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CropInfo(
    val id: String,
    val nameEn: String,
    val nameKn: String,
    val nameHi: String,
    val nameMr: String,
    val localNamesKn: List<String> = emptyList(),
    val category: String, // "cereal", "millet", "pulse", "oilseed", "vegetable", "fruit", "plantation", "cash"
    val seasons: List<String>, // ["kharif", "rabi", "summer", "perennial"]
    val tempMinC: Double? = null,
    val tempOptMinC: Double? = null,
    val tempOptMaxC: Double? = null,
    val tempMaxC: Double? = null,
    val annualRainfallMm: Int? = null,
    val humidityPreference: String? = null, // "low", "moderate", "high"
    val soilTypes: List<String> = emptyList(),
    val soilPhMin: Double? = null,
    val soilPhMax: Double? = null,
    val drainageRequirement: String? = null,
    val altitudeMaxM: Int? = null,
    val karnatakaZones: List<String> = emptyList(),
    val karnatakaDistricts: List<String> = emptyList(),
    val sowingWindows: List<SowingWindow> = emptyList(),
    val durationDaysMin: Int? = null,
    val durationDaysMax: Int? = null,
    val growthStages: List<GrowthStage> = emptyList(),
    val commonDiseaseIds: List<String> = emptyList(),
    val hasModelSupport: Boolean = false,
    val source: String = "",
    val reviewStatus: String = "draft",
    val reviewedBy: String? = null,
)

@Serializable
data class SowingWindow(
    val season: String,
    val sowingMonths: String,
    val harvestMonths: String,
    val notes: String? = null,
)

@Serializable
data class GrowthStage(
    val name: String,
    val nameKn: String? = null,
    val nameHi: String? = null,
    val nameMr: String? = null,
    val startDayAfterSowing: Int,
    val endDayAfterSowing: Int,
    val keyCare: String = "",
    val keyCareKn: String? = null,
)

