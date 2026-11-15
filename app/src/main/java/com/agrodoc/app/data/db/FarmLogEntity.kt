package com.agrodoc.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farm_logs")
data class FarmLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plotId: Long,
    val logType: String, // "observation", "fertilizer", "irrigation", "scan", "work"
    val note: String,
    val photoUri: String? = null,
    val weatherSnapshotJson: String? = null,
    val loggedAtEpoch: Long = System.currentTimeMillis(),
)
