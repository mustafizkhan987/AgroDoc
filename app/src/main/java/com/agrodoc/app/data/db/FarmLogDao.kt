package com.agrodoc.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmLogDao {

    @Query("SELECT * FROM farm_logs WHERE plotId = :plotId ORDER BY loggedAtEpoch DESC")
    fun getLogsForPlot(plotId: Long): Flow<List<FarmLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FarmLogEntity): Long

    @Delete
    suspend fun deleteLog(log: FarmLogEntity)
}
