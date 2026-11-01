package com.agrodoc.app.domain.repository

import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.DiseaseInfo
import com.agrodoc.app.domain.model.District
import com.agrodoc.app.domain.model.FertilizerRule

interface KnowledgeRepository {
    suspend fun getDistricts(): List<District>
    suspend fun getDistrictById(id: String): District?
    suspend fun getCrops(): List<CropInfo>
    suspend fun getCropById(id: String): CropInfo?
    suspend fun getDiseases(): List<DiseaseInfo>
    suspend fun getDiseaseById(id: String): DiseaseInfo?
    suspend fun getDiseasesForCrop(cropId: String): List<DiseaseInfo>
    suspend fun getFertilizerRules(): List<FertilizerRule>
    suspend fun getFertilizerRuleForCrop(cropId: String): FertilizerRule?
}

