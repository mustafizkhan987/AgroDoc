package com.agrodoc.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DiseaseInfo(
    val id: String,
    val classId: Int? = null,
    val cropKey: String,
    val cropNameEn: String = "",
    val cropNameKn: String = "",
    val cropNameHi: String = "",
    val cropNameMr: String = "",
    val nameEn: String,
    val nameKn: String,
    val nameHi: String,
    val nameMr: String,
    val symptomsEn: String,
    val symptomsKn: String,
    val symptomsHi: String,
    val symptomsMr: String,
    val cause: String = "fungal", // "fungal", "bacterial", "viral", "pest", "nutrient"
    val favourableConditions: DiseaseWeatherCondition = DiseaseWeatherCondition(),
    val spreadEn: String = "",
    val spreadKn: String = "",
    val spreadHi: String = "",
    val spreadMr: String = "",
    val preventionEn: String = "",
    val preventionKn: String = "",
    val preventionHi: String = "",
    val preventionMr: String = "",
    val treatmentEn: String = "",
    val treatmentKn: String = "",
    val treatmentHi: String = "",
    val treatmentMr: String = "",
    val whenToConsultOfficer: String = "",
    val hasModelSupport: Boolean = false,
    val source: String = "",
    val reviewedBy: String? = null,
    val reviewStatus: String = "draft",
)

@Serializable
data class DiseaseWeatherCondition(
    val tempMinC: Double? = null,
    val tempMaxC: Double? = null,
    val humidityMinPct: Int? = null,
    val rainFavourable: Boolean = false,
    val leafWetnessHoursMin: Int? = null,
    val descriptionEn: String = "",
    val descriptionKn: String = "",
    val descriptionHi: String = "",
    val descriptionMr: String = "",
)
