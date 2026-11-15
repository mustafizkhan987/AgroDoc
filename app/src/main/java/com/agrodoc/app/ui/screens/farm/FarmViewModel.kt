package com.agrodoc.app.ui.screens.farm

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrodoc.app.data.db.FarmLogDao
import com.agrodoc.app.data.db.FarmLogEntity
import com.agrodoc.app.data.db.PlotDao
import com.agrodoc.app.data.db.PlotEntity
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class FarmUiState(
    val plots: List<PlotEntity> = emptyList(),
    val crops: List<CropInfo> = emptyList(),
    val selectedPlot: PlotEntity? = null,
    val selectedPlotLogs: List<FarmLogEntity> = emptyList(),
    val currentLang: String = "kn",
    val isLoading: Boolean = true,
)

@HiltViewModel
class FarmViewModel @Inject constructor(
    private val plotDao: PlotDao,
    private val farmLogDao: FarmLogDao,
    private val knowledgeRepository: KnowledgeRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FarmUiState())
    val uiState: StateFlow<FarmUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val crops = knowledgeRepository.getCrops()
            val lang = settingsRepository.language.first() ?: "kn"
            _uiState.value = _uiState.value.copy(crops = crops, currentLang = lang, isLoading = false)

            plotDao.getAllPlots().collectLatest { plots ->
                _uiState.value = _uiState.value.copy(plots = plots)
                if (_uiState.value.selectedPlot == null && plots.isNotEmpty()) {
                    selectPlot(plots.first())
                }
            }
        }
    }

    fun selectPlot(plot: PlotEntity) {
        _uiState.value = _uiState.value.copy(selectedPlot = plot)
        viewModelScope.launch {
            farmLogDao.getLogsForPlot(plot.id).collectLatest { logs ->
                _uiState.value = _uiState.value.copy(selectedPlotLogs = logs)
            }
        }
    }

    fun addPlot(
        name: String,
        cropId: String,
        variety: String,
        areaValue: Double,
        areaUnit: String,
        sowingEpoch: Long,
        irrigationType: String,
    ) {
        viewModelScope.launch {
            val districtId = settingsRepository.selectedDistrictId.first() ?: "karnataka_bengaluru_urban"
            val newPlot = PlotEntity(
                name = name,
                cropId = cropId,
                variety = variety,
                areaValue = areaValue,
                areaUnit = areaUnit,
                sowingDateEpoch = sowingEpoch,
                districtId = districtId,
                irrigationType = irrigationType,
            )
            val id = plotDao.insertPlot(newPlot)
            selectPlot(newPlot.copy(id = id))
        }
    }

    fun addLog(plotId: Long, logType: String, note: String) {
        viewModelScope.launch {
            farmLogDao.insertLog(
                FarmLogEntity(
                    plotId = plotId,
                    logType = logType,
                    note = note,
                )
            )
        }
    }

    fun deletePlot(plotId: Long) {
        viewModelScope.launch {
            plotDao.deletePlot(plotId)
            val remaining = _uiState.value.plots.filter { it.id != plotId }
            _uiState.value = _uiState.value.copy(
                selectedPlot = remaining.firstOrNull(),
                selectedPlotLogs = emptyList(),
            )
        }
    }

    fun exportFarmReportCsv(context: Context): File? {
        val plot = _uiState.value.selectedPlot ?: return null
        val logs = _uiState.value.selectedPlotLogs
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "farm_report_${plot.id}_${System.currentTimeMillis()}.csv")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            file.bufferedWriter().use { writer ->
                writer.write("AgroDoc Farm Report\n")
                writer.write("Plot Name,${plot.name}\n")
                writer.write("Crop,${plot.cropId}\n")
                writer.write("Variety,${plot.variety}\n")
                writer.write("Area,${plot.areaValue} ${plot.areaUnit}\n")
                writer.write("Sowing Date,${sdf.format(Date(plot.sowingDateEpoch))}\n")
                writer.write("Irrigation,${plot.irrigationType}\n\n")
                writer.write("Timestamp,Type,Notes\n")
                logs.forEach { log ->
                    writer.write("\"${sdf.format(Date(log.loggedAtEpoch))}\",\"${log.logType}\",\"${log.note.replace("\"", "\"\"")}\"\n")
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }
}
