package com.agrodoc.app.domain.repository

import com.agrodoc.app.domain.model.WeatherData
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for weather data. The `offline` flavor binds [NoOpWeatherRepository]
 * which always returns null. The `connected` flavor binds the Open-Meteo implementation.
 *
 * All callers must handle null weather gracefully — weather is always optional.
 */
interface WeatherRepository {

    /**
     * Observes the most recent cached weather for the given location.
     * Emits null when no cache is available or in the offline flavor.
     */
    fun observeWeather(districtId: String): Flow<WeatherData?>

    /**
     * Fetches fresh weather from the remote source and stores it in the cache.
     * No-ops in the offline flavor.
     *
     * @return true if the fetch succeeded, false otherwise
     */
    suspend fun refresh(districtId: String, lat: Double, lon: Double): Boolean

    /**
     * True if this implementation can actually make network calls.
     * Always false in the offline flavor.
     */
    val isNetworkCapable: Boolean
}

