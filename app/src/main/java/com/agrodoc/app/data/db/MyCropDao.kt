package com.agrodoc.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MyCropDao {
    @Query("SELECT * FROM my_crops ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<MyCropEntity>>

    @Query("SELECT cropId FROM my_crops")
    fun observeMyCropIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MyCropEntity)

    @Query("DELETE FROM my_crops WHERE cropId = :cropId")
    suspend fun delete(cropId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM my_crops WHERE cropId = :cropId)")
    fun observeIsMyCrop(cropId: String): Flow<Boolean>
}

