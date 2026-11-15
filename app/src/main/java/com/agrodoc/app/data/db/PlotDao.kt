package com.agrodoc.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlotDao {

    @Query("SELECT * FROM plots ORDER BY createdAt DESC")
    fun getAllPlots(): Flow<List<PlotEntity>>

    @Query("SELECT * FROM plots WHERE id = :plotId LIMIT 1")
    suspend fun getPlotById(plotId: Long): PlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlot(plot: PlotEntity): Long

    @Update
    suspend fun updatePlot(plot: PlotEntity)

    @Query("DELETE FROM plots WHERE id = :plotId")
    suspend fun deletePlot(plotId: Long)
}
