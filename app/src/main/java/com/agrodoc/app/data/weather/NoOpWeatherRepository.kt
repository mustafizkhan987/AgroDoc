package com.agrodoc.app.data.weather

import com.agrodoc.app.domain.model.WeatherData
import com.agrodoc.app.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpWeatherRepository @Inject constructor() : WeatherRepository {
    override fun observeWeather(districtId: String): Flow<WeatherData?> = flowOf(null)
    override suspend fun refresh(districtId: String, lat: Double, lon: Double): Boolean = false
    override val isNetworkCapable: Boolean = false
}

