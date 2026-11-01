package com.agrodoc.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_crops")
data class MyCropEntity(
    @PrimaryKey val cropId: String,
    val addedAt: Long = System.currentTimeMillis(),
)

