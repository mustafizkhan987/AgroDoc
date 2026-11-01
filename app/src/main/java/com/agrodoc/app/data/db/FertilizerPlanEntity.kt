package com.agrodoc.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fertilizer_plans")
data class FertilizerPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cropId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val areaUnit: String,
    val areaValue: Double,
    val growthStage: String,
    val soilType: String,
    val soilTestJson: String,
    val planJson: String,
    val districtId: String,
)

