package com.agrodoc.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FertilizerPlanDao {
    @Query("SELECT * FROM fertilizer_plans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FertilizerPlanEntity>>

    @Query("SELECT * FROM fertilizer_plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FertilizerPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FertilizerPlanEntity): Long

    @Query("DELETE FROM fertilizer_plans WHERE id = :id")
    suspend fun delete(id: Long)
}

