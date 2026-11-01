package com.agrodoc.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val districtId: String,
    val fetchedAt: Long,
    val weatherJson: String,
    val provider: String,
)

