package com.agrodoc.app.ui.screens.fertilizer

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.agrodoc.app.data.db.FertilizerPlanDao
import com.agrodoc.app.data.db.FertilizerPlanEntity
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.domain.engine.FertilizerAdvisor
import com.agrodoc.app.domain.model.*
import com.agrodoc.app.domain.repository.KnowledgeRepository
import com.agrodoc.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class FertilizerUiState(val crops: List<CropInfo> = emptyList(), val cropId: String = "", val area: String = "1", val unit: String = "acre", val nitrogen: String = "", val phosphorus: String = "", val potassium: String = "", val stages: List<String> = listOf("all"), val stage: String = "all", val plan: FertilizerPlan? = null, val message: String? = null)

@HiltViewModel
class FertilizerViewModel @Inject constructor(private val knowledge: KnowledgeRepository, private val dao: FertilizerPlanDao, private val settings: SettingsRepository, @ApplicationContext private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(FertilizerUiState())
    val state = _state.asStateFlow()
    init { viewModelScope.launch { val crops = knowledge.getCrops(); val id = crops.firstOrNull()?.id.orEmpty(); val rule = knowledge.getFertilizerRuleForCrop(id); _state.value = _state.value.copy(crops = crops, cropId = id, stages = listOf("all") + rule?.splits.orEmpty().map { it.stage }) } }
    fun selectCrop(id: String) { viewModelScope.launch { val rule = knowledge.getFertilizerRuleForCrop(id); val stages = listOf("all") + rule?.splits.orEmpty().map { it.stage }; _state.value = _state.value.copy(cropId = id, stages = stages, stage = "all", plan = null, message = null) } }
    fun setArea(value: String) { _state.value = _state.value.copy(area = value.filter { it.isDigit() || it == '.' }) }
    fun setUnit(value: String) { _state.value = _state.value.copy(unit = value) }
    fun setSoilTest(n: String? = null, p: String? = null, k: String? = null) { val s = _state.value; _state.value = s.copy(nitrogen = n ?: s.nitrogen, phosphorus = p ?: s.phosphorus, potassium = k ?: s.potassium) }
    fun setStage(stage: String) { _state.value = _state.value.copy(stage = stage) }
    fun calculate() { viewModelScope.launch {
        val s = _state.value
        val crop = knowledge.getCropById(s.cropId)
        val rule = knowledge.getFertilizerRuleForCrop(s.cropId)
        val area = s.area.toDoubleOrNull()
        if (crop == null || rule == null || area == null || area <= 0) { _state.value = s.copy(message = context.getString(R.string.error_generic)); return@launch }
        val lang = settings.localeTag.first()
        val soilTest = if (s.nitrogen.isNotBlank() || s.phosphorus.isNotBlank() || s.potassium.isNotBlank()) SoilTest(s.nitrogen.toDoubleOrNull(), s.phosphorus.toDoubleOrNull(), s.potassium.toDoubleOrNull()) else null
        val plan = FertilizerAdvisor.calculatePlan(crop, rule, area, s.unit, growthStage = s.stage, soilTest = soilTest, langCode = lang)
        val districtId = settings.selectedDistrictId.first()
        dao.insert(FertilizerPlanEntity(cropId = crop.id, areaUnit = s.unit, areaValue = area, growthStage = s.stage, soilType = plan.soilType, soilTestJson = soilTest?.let { Json.encodeToString(it) } ?: "{}", planJson = Json.encodeToString(plan), districtId = districtId))
        _state.value = s.copy(plan = plan, message = context.getString(R.string.plan_saved))
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FertilizerScreen(initialCropId: String? = null, viewModel: FertilizerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(initialCropId, state.crops) { if (!initialCropId.isNullOrBlank() && state.crops.any { it.id == initialCropId }) viewModel.selectCrop(initialCropId) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.fertilizer_title)) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(value = state.crops.find { it.id == state.cropId }?.nameEn ?: stringResource(R.string.select_crop), onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.select_crop)) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { state.crops.forEach { crop -> DropdownMenuItem(text = { Text(crop.nameEn) }, onClick = { viewModel.selectCrop(crop.id); expanded = false }) } }
            } }
            item { OutlinedTextField(state.area, viewModel::setArea, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.area_input)) }, singleLine = true) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("acre" to R.string.unit_acre, "hectare" to R.string.unit_ha, "guntha" to R.string.unit_guntha).forEach { (unit, label) -> FilterChip(selected = state.unit == unit, onClick = { viewModel.setUnit(unit) }, label = { Text(stringResource(label)) }) } } }
            item { Text(stringResource(R.string.fertilizer_soil_test), style = MaterialTheme.typography.titleMedium) }
            item { OutlinedTextField(state.nitrogen, { viewModel.setSoilTest(n = it.filter { ch -> ch.isDigit() || ch == '.' }) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.fertilizer_nitrogen)) }, singleLine = true) }
            item { OutlinedTextField(state.phosphorus, { viewModel.setSoilTest(p = it.filter { ch -> ch.isDigit() || ch == '.' }) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.fertilizer_phosphorus)) }, singleLine = true) }
            item { OutlinedTextField(state.potassium, { viewModel.setSoilTest(k = it.filter { ch -> ch.isDigit() || ch == '.' }) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.fertilizer_potassium)) }, singleLine = true) }
            item { Text(stringResource(R.string.fertilizer_application_timing), style = MaterialTheme.typography.titleMedium) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.stages.forEach { stage -> FilterChip(selected = state.stage == stage, onClick = { viewModel.setStage(stage) }, label = { Text(stage.replace('_', ' ')) }) } } }
            item { Button(onClick = viewModel::calculate, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.fertilizer_calculate_save)) } }
            state.message?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium) } }
            state.plan?.let { plan ->
                item { Text("${plan.cropNameEn} • ${plan.areaValue} ${plan.areaUnit}", style = MaterialTheme.typography.titleLarge) }
                plan.schedule.forEach { step -> item { ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(step.timing, style = MaterialTheme.typography.titleMedium); Text(step.nutrientNPK); step.products.forEach { Text("${it.name}: ${it.rateForArea} kg for this area") }; Text(step.notes) } } } }
                plan.organicOptions.forEach { option -> item { ListItem(headlineContent = { Text(option.name) }, supportingContent = { Text("${option.ratePerHa}. ${option.notes}") }) } }
                plan.micronutrientNotes.forEach { item { Text(it) } }
                item { Text(plan.disclaimer, style = MaterialTheme.typography.bodySmall) }
                item { Text("Source: ${plan.sourceReference}", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
