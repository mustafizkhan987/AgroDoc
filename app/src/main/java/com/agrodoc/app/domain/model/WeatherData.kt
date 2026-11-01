package com.agrodoc.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Weather data for a single location, covering today and up to 7 days forecast.
 * All fields use SI units unless noted.
 *
 * This is a pure domain model — it is not stored directly in Room.
 * The Room entity [com.agrodoc.app.data.db.WeatherCacheEntity] holds the JSON blob.
 */
@Serializable
data class WeatherData(
    /** Stable district identifier matching districts.json */
    val districtId: String,
    /** Unix epoch millis when this data was fetched */
    val fetchedAt: Long,
    /** Current / today conditions */
    val current: CurrentConditions,
    /** 7-day forecast, index 0 = today */
    val daily: List<DailyForecast>,
    /** Data source name for attribution */
    val provider: String = "Open-Meteo",
)

@Serializable
data class CurrentConditions(
    /** °C */
    val tempC: Double,
    /** 0–100 % */
    val relativeHumidityPct: Int,
    /** WMO weather code */
    val weatherCode: Int,
    /** mm/hr */
    val precipitationMmPerHr: Double,
    /** km/h */
    val windKph: Double,
)

@Serializable
data class DailyForecast(
    /** YYYY-MM-DD */
    val date: String,
    val tempMinC: Double,
    val tempMaxC: Double,
    val humidityMinPct: Int,
    val humidityMaxPct: Int,
    /** Total rainfall mm */
    val precipitationMm: Double,
    /** 0–100 % */
    val precipitationProbabilityPct: Int,
    val windMaxKph: Double,
    /** WMO weather code for the day */
    val weatherCode: Int,
    /**
     * Estimated hours per day where relative humidity ≥ 90 %.
     * Proxy for leaf-wetness duration. Derived from hourly data or approximated.
     * Labelled "estimate" in UI.
     */
    val highHumidityHoursEstimate: Int,
)

