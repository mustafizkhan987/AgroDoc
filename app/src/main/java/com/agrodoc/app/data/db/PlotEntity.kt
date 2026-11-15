package com.agrodoc.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plots")
data class PlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cropId: String,
    val variety: String = "",
    val areaValue: Double,
    val areaUnit: String, // "acre", "hectare", "guntha"
    val sowingDateEpoch: Long,
    val districtId: String = "karnataka_bengaluru_urban",
    val irrigationType: String = "rainfed", // "rainfed", "drip", "sprinkler", "flood"
    val soilTestJson: String? = null,
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

