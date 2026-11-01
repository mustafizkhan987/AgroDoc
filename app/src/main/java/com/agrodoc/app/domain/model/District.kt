package com.agrodoc.app.domain.model

import kotlinx.serialization.Serializable

/** Represents a district entry from districts.json */
@Serializable
data class District(
    val id: String,           // "{state_slug}_{district_slug}"
    val state: String,        // Full state name in English
    val district: String,     // Full district name in English
    val lat: Double,
    val lon: Double,
)

/** Represents a disease risk assessment for a specific disease over the forecast window */
data class DiseaseRisk(
    val diseaseId: String,
    val diseaseName: String,  // in current locale
    val cropId: String,
    val riskLevel: RiskLevel,
    /** One-line localized reason (template-based, from knowledge base) */
    val reason: String,
    /** Which forecast days drove this risk (indices into WeatherData.daily) */
    val drivingDays: List<Int>,
)

enum class RiskLevel { LOW, MODERATE, HIGH }

/** Represents a crop the farmer has marked as "My crop" */
@Serializable
data class MyCrop(
    val cropId: String,
    val addedAt: Long = System.currentTimeMillis(),
)

