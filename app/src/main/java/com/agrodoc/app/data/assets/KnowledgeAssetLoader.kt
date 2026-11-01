package com.agrodoc.app.data.assets

import android.content.Context
import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.DiseaseInfo
import com.agrodoc.app.domain.model.District
import com.agrodoc.app.domain.model.FertilizerRule
import com.agrodoc.app.domain.repository.KnowledgeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeAssetLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) : KnowledgeRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val mutex = Mutex()
    private var cachedDistricts: List<District>? = null
    private var cachedCrops: List<CropInfo>? = null
    private var cachedDiseases: List<DiseaseInfo>? = null
    private var cachedFertilizerRules: List<FertilizerRule>? = null

    override suspend fun getDistricts(): List<District> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedDistricts ?: loadJsonAsset<List<District>>("districts.json").also {
                cachedDistricts = it
            }
        }
    }

    override suspend fun getDistrictById(id: String): District? {
        return getDistricts().find { it.id == id }
    }

    override suspend fun getCrops(): List<CropInfo> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedCrops ?: loadJsonAsset<List<CropInfo>>("knowledge/crops.json").also {
                cachedCrops = it
            }
        }
    }

    override suspend fun getCropById(id: String): CropInfo? {
        return getCrops().find { it.id.equals(cropIdSanitized(id), ignoreCase = true) }
    }

    override suspend fun getDiseases(): List<DiseaseInfo> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedDiseases ?: loadJsonAsset<List<DiseaseInfo>>("knowledge/disease_info.json").also {
                cachedDiseases = it
            }
        }
    }

    override suspend fun getDiseaseById(id: String): DiseaseInfo? {
        return getDiseases().find { it.id.equals(id, ignoreCase = true) }
    }

    override suspend fun getDiseasesForCrop(cropId: String): List<DiseaseInfo> {
        val sanitized = cropIdSanitized(cropId)
        return getDiseases().filter { it.cropKey.equals(sanitized, ignoreCase = true) }
    }

    override suspend fun getFertilizerRules(): List<FertilizerRule> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedFertilizerRules ?: loadJsonAsset<List<FertilizerRule>>("knowledge/fertilizer_rules.json").also {
                cachedFertilizerRules = it
            }
        }
    }

    override suspend fun getFertilizerRuleForCrop(cropId: String): FertilizerRule? {
        val sanitized = cropIdSanitized(cropId)
        return getFertilizerRules().find { it.cropId.equals(sanitized, ignoreCase = true) }
    }

    private fun cropIdSanitized(cropId: String): String {
        return cropId.trim().lowercase().replace(" ", "_")
    }

    private inline fun <reified T> loadJsonAsset(assetPath: String): T {
        val jsonString = context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString(jsonString)
    }
}

