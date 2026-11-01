package com.agrodoc.app.data.weather

import android.util.Log
import com.agrodoc.app.data.db.WeatherCacheDao
import com.agrodoc.app.data.db.WeatherCacheEntity
import com.agrodoc.app.data.weather.openmeteo.OpenMeteoApi
import com.agrodoc.app.domain.model.CurrentConditions
import com.agrodoc.app.domain.model.DailyForecast
import com.agrodoc.app.domain.model.WeatherData
import com.agrodoc.app.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenMeteoWeatherRepository @Inject constructor(
    private val api: OpenMeteoApi,
    private val cacheDao: WeatherCacheDao,
) : WeatherRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeWeather(districtId: String): Flow<WeatherData?> =
        cacheDao.observe(districtId).map { entity ->
            entity?.let {
                try {
                    json.decodeFromString<WeatherData>(it.weatherJson)
                } catch (e: Exception) {
                    Log.e("WeatherRepo", "Failed to deserialize cached weather", e)
                    null
                }
            }
        }

    override suspend fun refresh(districtId: String, lat: Double, lon: Double): Boolean {
        return try {
            val response = api.getForecast(latitude = lat, longitude = lon)
            val current = CurrentConditions(
                tempC = response.current?.temperature2m ?: 25.0,
                relativeHumidityPct = response.current?.relativeHumidity2m ?: 60,
                weatherCode = response.current?.weatherCode ?: 0,
                precipitationMmPerHr = response.current?.precipitation ?: 0.0,
                windKph = response.current?.windSpeed10m ?: 5.0,
            )

            val dailyList = mutableListOf<DailyForecast>()
            val daily = response.daily
            if (daily != null) {
                val count = daily.time.size
                for (i in 0 until count) {
                    val hMax = daily.relativeHumidity2mMax.getOrNull(i) ?: 80
                    val hMin = daily.relativeHumidity2mMin.getOrNull(i) ?: 40
                    val estHighHours = if (hMax >= 90) ((hMax - 85) * 1.5).toInt().coerceIn(1, 12) else 0
                    dailyList.add(
                        DailyForecast(
                            date = daily.time[i],
                            tempMinC = daily.temperature2mMin.getOrNull(i) ?: 18.0,
                            tempMaxC = daily.temperature2mMax.getOrNull(i) ?: 30.0,
                            humidityMinPct = hMin,
                            humidityMaxPct = hMax,
                            precipitationMm = daily.precipitationSum.getOrNull(i) ?: 0.0,
                            precipitationProbabilityPct = daily.precipitationProbabilityMax.getOrNull(i) ?: 0,
                            windMaxKph = daily.windSpeed10mMax.getOrNull(i) ?: 10.0,
                            weatherCode = daily.weatherCode.getOrNull(i) ?: 0,
                            highHumidityHoursEstimate = estHighHours,
                        )
                    )
                }
            }

            val weatherData = WeatherData(
                districtId = districtId,
                fetchedAt = System.currentTimeMillis(),
                current = current,
                daily = dailyList,
                provider = "Open-Meteo",
            )

            cacheDao.upsert(
                WeatherCacheEntity(
                    districtId = districtId,
                    fetchedAt = weatherData.fetchedAt,
                    weatherJson = json.encodeToString(weatherData),
                    provider = "Open-Meteo",
                )
            )
            true
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Failed to fetch weather from Open-Meteo", e)
            false
        }
    }

    override val isNetworkCapable: Boolean = true
}

