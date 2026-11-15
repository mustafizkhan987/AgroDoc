package com.agrodoc.app.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.agrodoc.app.data.db.MyCropDao
import com.agrodoc.app.data.notification.DiseaseAlertNotifier
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.domain.engine.DiseaseRiskEngine
import com.agrodoc.app.domain.model.RiskLevel
import com.agrodoc.app.domain.repository.KnowledgeRepository
import com.agrodoc.app.domain.repository.WeatherRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class WeatherSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WeatherWorkerEntryPoint {
        fun weatherRepository(): WeatherRepository
        fun knowledgeRepository(): KnowledgeRepository
        fun myCropDao(): MyCropDao
        fun settingsRepository(): SettingsRepository
        fun diseaseAlertNotifier(): DiseaseAlertNotifier
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WeatherWorkerEntryPoint::class.java,
        )

        return try {
            val settings = entryPoint.settingsRepository()
            val districtId = settings.selectedDistrictId.first() ?: "karnataka_bengaluru_urban"
            val lang = settings.language.first() ?: "kn"

            val knowledge = entryPoint.knowledgeRepository()
            val district = knowledge.getDistrictById(districtId) ?: return Result.success()

            val weatherRepo = entryPoint.weatherRepository()
            val refreshed = weatherRepo.refresh(district.id, district.lat, district.lon)
            if (!refreshed) return Result.retry()

            val weather = weatherRepo.observeWeather(district.id).first() ?: return Result.success()
            val myCrops = entryPoint.myCropDao().observeAll().first()
            if (myCrops.isEmpty()) return Result.success()

            val cropIds = myCrops.map { it.cropId }.toSet()
            val allDiseases = knowledge.getDiseases()

            val alerts = DiseaseRiskEngine.evaluateRisksForCrops(
                cropIds = cropIds,
                allDiseases = allDiseases,
                weather = weather,
                langCode = lang,
            )

            val notifier = entryPoint.diseaseAlertNotifier()
            alerts.filter { it.riskLevel == RiskLevel.HIGH }.forEach { alert ->
                notifier.showHighRiskAlert(
                    cropName = alert.cropId,
                    diseaseName = alert.diseaseName,
                    reason = alert.reason,
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "AgroDocWeatherSync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
