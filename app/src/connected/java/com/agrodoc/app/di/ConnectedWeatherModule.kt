package com.agrodoc.app.di

import com.agrodoc.app.data.weather.OpenMeteoWeatherRepository
import com.agrodoc.app.data.weather.openmeteo.OpenMeteoApi
import com.agrodoc.app.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectedWeatherModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: OpenMeteoWeatherRepository): WeatherRepository

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/"

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            return OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideOpenMeteoApi(client: OkHttpClient): OpenMeteoApi {
            val json = Json { ignoreUnknownKeys = true }
            val contentType = "application/json".toMediaType()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(OpenMeteoApi::class.java)
        }
    }
}

