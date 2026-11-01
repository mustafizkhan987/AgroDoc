package com.agrodoc.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns to scans table
        db.execSQL("ALTER TABLE scans ADD COLUMN districtId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE scans ADD COLUMN weatherSnapshotJson TEXT DEFAULT NULL")

        // Create weather_cache table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS weather_cache (
                districtId TEXT NOT NULL PRIMARY KEY,
                fetchedAt INTEGER NOT NULL,
                weatherJson TEXT NOT NULL,
                provider TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Create my_crops table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS my_crops (
                cropId TEXT NOT NULL PRIMARY KEY,
                addedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Create fertilizer_plans table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS fertilizer_plans (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                cropId TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                areaUnit TEXT NOT NULL,
                areaValue REAL NOT NULL,
                growthStage TEXT NOT NULL,
                soilType TEXT NOT NULL,
                soilTestJson TEXT NOT NULL,
                planJson TEXT NOT NULL,
                districtId TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

