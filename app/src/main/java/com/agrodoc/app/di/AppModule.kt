package com.agrodoc.app.di

import android.content.Context
import androidx.room.Room
import com.agrodoc.app.data.db.AppDatabase
import com.agrodoc.app.data.db.FertilizerPlanDao
import com.agrodoc.app.data.db.MIGRATION_1_2
import com.agrodoc.app.data.db.MyCropDao
import com.agrodoc.app.data.db.ScanDao
import com.agrodoc.app.data.db.WeatherCacheDao
import com.agrodoc.app.data.prefs.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "agrodoc.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    @Singleton
    fun provideScanDao(db: AppDatabase): ScanDao = db.scanDao()

    @Provides
    @Singleton
    fun provideWeatherCacheDao(db: AppDatabase): WeatherCacheDao = db.weatherCacheDao()

    @Provides
    @Singleton
    fun provideMyCropDao(db: AppDatabase): MyCropDao = db.myCropDao()

    @Provides
    @Singleton
    fun provideFertilizerPlanDao(db: AppDatabase): FertilizerPlanDao = db.fertilizerPlanDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext ctx: Context): SettingsRepository =
        SettingsRepository(ctx)

    @Provides
    @Singleton
    fun provideKnowledgeRepository(loader: com.agrodoc.app.data.assets.KnowledgeAssetLoader): com.agrodoc.app.domain.repository.KnowledgeRepository =
        loader

    @Provides
    @Singleton
    fun provideAnalyzer(@ApplicationContext ctx: Context): com.agrodoc.app.ml.Analyzer =
        com.agrodoc.app.ml.Analyzer(ctx)
}
