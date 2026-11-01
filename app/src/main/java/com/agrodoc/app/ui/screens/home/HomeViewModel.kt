package com.agrodoc.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrodoc.app.data.db.MyCropDao
import com.agrodoc.app.data.db.ScanDao
import com.agrodoc.app.data.db.ScanEntity
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.domain.engine.DiseaseRiskEngine
import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.DiseaseInfo
import com.agrodoc.app.domain.model.DiseaseRisk
import com.agrodoc.app.domain.model.District
import com.agrodoc.app.domain.model.RiskLevel
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

data class HomeUiState(
    val isLoading: Boolean = true,
    val selectedDistrict: District? = null,
    val weather: WeatherData? = null,
    val isWeatherStale: Boolean = false,
    val myCrops: List<CropInfo> = emptyList(),
    val diseaseAlerts: List<DiseaseRisk> = emptyList(),
    val recentScans: List<ScanEntity> = emptyList(),
    val currentLang: String = "kn",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val myCropDao: MyCropDao,
    private val scanDao: ScanDao,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var allDiseases: List<DiseaseInfo> = emptyList()
    private var allCrops: List<CropInfo> = emptyList()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val lang = settingsRepository.localeTag.first()
            allDiseases = knowledgeRepository.getDiseases()
            allCrops = knowledgeRepository.getCrops()

            // Observe district
            settingsRepository.selectedDistrictId.collectLatest { districtId ->
                val targetDistrictId = districtId ?: "karnataka_bengaluru_urban"
                val district = knowledgeRepository.getDistrictById(targetDistrictId) ?: run {
                    val latitude = settingsRepository.selectedLatitude.first()
                    val longitude = settingsRepository.selectedLongitude.first()
                    if (latitude != null && longitude != null) {
                        District(targetDistrictId, settingsRepository.selectedState.first(), settingsRepository.selectedDistrictName.first(), latitude, longitude)
                    } else knowledgeRepository.getDistricts().firstOrNull()
                }

                _uiState.value = _uiState.value.copy(
                    selectedDistrict = district,
                    currentLang = lang,
                )

                // Observe weather
                if (district != null) {
                    observeWeatherAndAlerts(district, lang)
                }
            }
        }

        // Observe recent scans
        viewModelScope.launch {
            scanDao.observeAll().collectLatest { scans ->
                _uiState.value = _uiState.value.copy(recentScans = scans.take(5))
            }
        }

        // Observe my crops
        viewModelScope.launch {
            myCropDao.observeAll().collectLatest { entities ->
                val myCropIds = entities.map { it.cropId }.toSet()
                val crops = allCrops.filter { myCropIds.contains(it.id) }
                _uiState.value = _uiState.value.copy(myCrops = crops)
                recomputeAlerts()
            }
        }
    }

    private fun observeWeatherAndAlerts(district: District, lang: String) {
        viewModelScope.launch {
            weatherRepository.observeWeather(district.id).collectLatest { cached ->
                if (cached != null) {
                    val isStale = (System.currentTimeMillis() - cached.fetchedAt) > 6 * 3600 * 1000L
                    _uiState.value = _uiState.value.copy(
                        weather = cached,
                        isWeatherStale = isStale,
                        isLoading = false,
                    )
                    recomputeAlerts()
                } else {
                    // Try fetch if connected
                    refreshWeather()
                }
            }
        }
    }

    fun refreshWeather() {
        val district = _uiState.value.selectedDistrict ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            weatherRepository.refresh(district.id, district.lat, district.lon)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun recomputeAlerts() {
        val state = _uiState.value
        val cropIds = state.myCrops.map { it.id }.toSet()
        if (cropIds.isEmpty() || allDiseases.isEmpty()) {
            _uiState.value = _uiState.value.copy(diseaseAlerts = emptyList())
            return
        }

        val risks = DiseaseRiskEngine.evaluateRisksForCrops(
            cropIds = cropIds,
            allDiseases = allDiseases,
            weather = state.weather,
            langCode = state.currentLang,
        ).filter { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.MODERATE }

        _uiState.value = _uiState.value.copy(diseaseAlerts = risks)
    }
}

