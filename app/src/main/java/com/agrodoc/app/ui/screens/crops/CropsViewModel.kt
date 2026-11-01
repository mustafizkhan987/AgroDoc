package com.agrodoc.app.ui.screens.crops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrodoc.app.data.db.MyCropDao
import com.agrodoc.app.data.db.MyCropEntity
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.domain.engine.CropSuitability
import com.agrodoc.app.domain.engine.CropSuitabilityEngine
import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.DiseaseInfo
import com.agrodoc.app.domain.model.District
import com.agrodoc.app.domain.model.WeatherData
import com.agrodoc.app.domain.repository.KnowledgeRepository
import com.agrodoc.app.domain.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CropsUiState(
    val crops: List<CropInfo> = emptyList(),
    val filteredCrops: List<CropInfo> = emptyList(),
    val myCropIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedSeason: String? = null,
    val selectedDistrict: District? = null,
    val weather: WeatherData? = null,
    val currentLang: String = "kn",
    val isLoading: Boolean = true,
)

@HiltViewModel
class CropsViewModel @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
    private val myCropDao: MyCropDao,
    private val settingsRepository: SettingsRepository,
    private val weatherRepository: WeatherRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CropsUiState())
    val uiState: StateFlow<CropsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val crops = knowledgeRepository.getCrops()
            val lang = settingsRepository.localeTag.first()

            _uiState.value = _uiState.value.copy(
                crops = crops,
                filteredCrops = crops,
                currentLang = lang,
                isLoading = false,
            )

            // Observe My Crops
            myCropDao.observeAll().collectLatest { entities ->
                _uiState.value = _uiState.value.copy(
                    myCropIds = entities.map { it.cropId }.toSet()
                )
            }
        }

        viewModelScope.launch {
            settingsRepository.selectedDistrictId.collectLatest { dId ->
                val id = dId ?: "karnataka_bengaluru_urban"
                val dist = knowledgeRepository.getDistrictById(id) ?: run {
                    val latitude = settingsRepository.selectedLatitude.first()
                    val longitude = settingsRepository.selectedLongitude.first()
                    if (latitude != null && longitude != null) District(id, settingsRepository.selectedState.first(), settingsRepository.selectedDistrictName.first(), latitude, longitude) else null
                }
                _uiState.value = _uiState.value.copy(selectedDistrict = dist)
                if (dist != null) {
                    val cached = weatherRepository.observeWeather(dist.id).first()
                    _uiState.value = _uiState.value.copy(weather = cached)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setSeasonFilter(season: String?) {
        _uiState.value = _uiState.value.copy(selectedSeason = season)
        applyFilters()
    }

    private fun applyFilters() {
        val q = _uiState.value.searchQuery.trim().lowercase()
        val s = _uiState.value.selectedSeason

        val result = _uiState.value.crops.filter { crop ->
            val matchesQuery = q.isEmpty() ||
                crop.id.contains(q) ||
                crop.nameEn.lowercase().contains(q) ||
                crop.nameKn.lowercase().contains(q) ||
                crop.nameHi.lowercase().contains(q) ||
                crop.nameMr.lowercase().contains(q) ||
                crop.localNamesKn.any { it.lowercase().contains(q) }

            val matchesSeason = s == null || crop.seasons.any { it.equals(s, ignoreCase = true) }

            matchesQuery && matchesSeason
        }

        _uiState.value = _uiState.value.copy(filteredCrops = result)
    }

    fun toggleMyCrop(cropId: String) {
        viewModelScope.launch {
            if (_uiState.value.myCropIds.contains(cropId)) {
                myCropDao.delete(cropId)
            } else {
                myCropDao.insert(MyCropEntity(cropId = cropId))
            }
        }
    }

    suspend fun getCropDetail(cropId: String): CropInfo? {
        return knowledgeRepository.getCropById(cropId)
    }

    fun getSuitability(crop: CropInfo): CropSuitability {
        return CropSuitabilityEngine.evaluate(
            crop = crop,
            district = _uiState.value.selectedDistrict,
            weather = _uiState.value.weather,
            langCode = _uiState.value.currentLang,
        )
    }

    suspend fun getDiseasesForCrop(cropId: String): List<DiseaseInfo> {
        return knowledgeRepository.getDiseasesForCrop(cropId)
    }
}

