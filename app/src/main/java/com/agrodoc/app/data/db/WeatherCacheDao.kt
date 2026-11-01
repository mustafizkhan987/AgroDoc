package com.agrodoc.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE districtId = :districtId")
    fun observe(districtId: String): Flow<WeatherCacheEntity?>

    @Query("SELECT * FROM weather_cache WHERE districtId = :districtId LIMIT 1")
    suspend fun get(districtId: String): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE fetchedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}

