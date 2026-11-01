package com.agrodoc.app.di

import com.agrodoc.app.data.weather.NoOpWeatherRepository
import com.agrodoc.app.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OfflineWeatherModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: NoOpWeatherRepository): WeatherRepository
}

