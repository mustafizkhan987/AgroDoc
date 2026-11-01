package com.agrodoc.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanEntity): Long

    /** Observe all scans, newest first. */
    @Query("SELECT * FROM scans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentScans(limit: Int): Flow<List<ScanEntity>>

    /** Observe scans filtered by crop key. */
    @Query("SELECT * FROM scans WHERE cropKey = :cropKey ORDER BY createdAt DESC")
    fun observeByCrop(cropKey: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScanEntity?

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM scans")
    suspend fun deleteAll()
}
