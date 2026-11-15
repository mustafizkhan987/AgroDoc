package com.agrodoc.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ScanEntity::class,
        WeatherCacheEntity::class,
        MyCropEntity::class,
        FertilizerPlanEntity::class,
        PlotEntity::class,
        FarmLogEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun myCropDao(): MyCropDao
    abstract fun fertilizerPlanDao(): FertilizerPlanDao
    abstract fun plotDao(): PlotDao
    abstract fun farmLogDao(): FarmLogDao
}
