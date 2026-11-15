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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS plots (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                cropId TEXT NOT NULL,
                variety TEXT NOT NULL,
                areaValue REAL NOT NULL,
                areaUnit TEXT NOT NULL,
                sowingDateEpoch INTEGER NOT NULL,
                districtId TEXT NOT NULL,
                irrigationType TEXT NOT NULL,
                soilTestJson TEXT DEFAULT NULL,
                photoUri TEXT DEFAULT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS farm_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                plotId INTEGER NOT NULL,
                logType TEXT NOT NULL,
                note TEXT NOT NULL,
                photoUri TEXT DEFAULT NULL,
                weatherSnapshotJson TEXT DEFAULT NULL,
                loggedAtEpoch INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

