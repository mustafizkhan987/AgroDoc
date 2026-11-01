package com.agrodoc.app.data.weather.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val current: OpenMeteoCurrent? = null,
    val daily: OpenMeteoDaily? = null,
)

@Serializable
data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature2m: Double = 0.0,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int = 0,
    val precipitation: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0,
    @SerialName("wind_speed_10m") val windSpeed10m: Double = 0.0,
)

@Serializable
data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    @SerialName("relative_humidity_2m_max") val relativeHumidity2mMax: List<Int?> = emptyList(),
    @SerialName("relative_humidity_2m_min") val relativeHumidity2mMin: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windSpeed10mMax: List<Double> = emptyList(),
)

