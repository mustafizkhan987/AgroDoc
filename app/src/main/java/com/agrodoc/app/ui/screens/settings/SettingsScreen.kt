package com.agrodoc.app.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.agrodoc.app.R
import com.agrodoc.app.data.db.ScanDao
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.domain.model.District
import com.agrodoc.app.domain.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File
import kotlinx.coroutines.flow.first

data class SettingsUiState(val districts: List<District> = emptyList(), val districtId: String = "", val districtName: String = "", val state: String = "", val language: String = "kn", val voice: Boolean = true, val textSize: String = "M", val heatmapStyle: String = "default", val locationMessage: String? = null)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val settings: SettingsRepository, private val knowledge: KnowledgeRepository, private val scanDao: ScanDao) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()
    init {
        viewModelScope.launch {
            val districts = knowledge.getDistricts()
            settings.selectedDistrictId.collect { id ->
                val district = districts.find { it.id == id }
                _state.value = _state.value.copy(
                    districts = districts,
                    districtId = id,
                    districtName = district?.district ?: settings.selectedDistrictName.first(),
                    state = district?.state ?: settings.selectedState.first(),
                )
            }
        }
        viewModelScope.launch { settings.localeTag.collect { _state.value = _state.value.copy(language = it) } }
        viewModelScope.launch { settings.voiceEnabled.collect { _state.value = _state.value.copy(voice = it) } }
        viewModelScope.launch { settings.textSize.collect { _state.value = _state.value.copy(textSize = it) } }
        viewModelScope.launch { settings.heatmapStyle.collect { _state.value = _state.value.copy(heatmapStyle = it) } }
    }
    fun chooseDistrict(d: District) { viewModelScope.launch { settings.setLocation(d.state, d.id, d.district) } }
    fun useCurrentLocation(context: Context) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fun save(location: Location) { viewModelScope.launch { settings.setLocation("India", "gps_${location.latitude}_${location.longitude}", "Current location", location.latitude, location.longitude); _state.value = _state.value.copy(locationMessage = context.getString(R.string.settings_location_approx)) } }
        val providers = manager.getProviders(true)
        val cached = providers.mapNotNull { provider -> try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null } }.maxByOrNull { it.time }
        if (cached != null) { save(cached); return }
        val provider = providers.firstOrNull() ?: run { _state.value = _state.value.copy(locationMessage = context.getString(R.string.settings_location_unavailable)); return }
        try {
            manager.requestSingleUpdate(provider, object : LocationListener {
                override fun onLocationChanged(location: Location) = save(location)
                @Deprecated("Deprecated by Android") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) { _state.value = _state.value.copy(locationMessage = context.getString(R.string.settings_location_unavailable)) }
            }, android.os.Looper.getMainLooper())
            _state.value = _state.value.copy(locationMessage = context.getString(R.string.settings_location_finding))
        } catch (_: SecurityException) { _state.value = _state.value.copy(locationMessage = context.getString(R.string.settings_location_permission)) }
    }
    fun chooseLanguage(tag: String) { viewModelScope.launch { settings.setLocaleTag(tag); AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) } }
    fun voice(enabled: Boolean) { viewModelScope.launch { settings.setVoiceEnabled(enabled) } }
    fun textSize(size: String) { viewModelScope.launch { settings.setTextSize(size) } }
    fun heatmap(style: String) { viewModelScope.launch { settings.setHeatmapStyle(style) } }
    fun clearHistory() { viewModelScope.launch { scanDao.observeAll().first().forEach { File(it.imagePath).delete(); File(it.overlayPath).delete() }; scanDao.deleteAll() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onAboutClick: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showStates by remember { mutableStateOf(false) }
    var selectedState by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.useCurrentLocation(context)
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("kn" to "ಕನ್ನಡ", "hi" to "हिन्दी", "mr" to "मराठी", "en" to "English").forEach { (tag, label) -> FilterChip(selected = state.language == tag, onClick = { viewModel.chooseLanguage(tag) }, label = { Text(label) }) } } }
            item { HorizontalDivider() }
            item { Text(stringResource(R.string.settings_weather_location), style = MaterialTheme.typography.titleMedium) }
            item { OutlinedButton(onClick = { showStates = true }, modifier = Modifier.fillMaxWidth()) { Text("${state.state} • ${state.districtName}") } }
            if (com.agrodoc.app.BuildConfig.FLAVOR == "connected") item { OutlinedButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) viewModel.useCurrentLocation(context)
                else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_use_current_location)) } }
            state.locationMessage?.let { message -> item { Text(message, style = MaterialTheme.typography.bodySmall) } }
            item { HorizontalDivider() }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.settings_voice)); Switch(checked = state.voice, onCheckedChange = viewModel::voice) } }
            item { Text(stringResource(R.string.settings_text_size), style = MaterialTheme.typography.titleMedium) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("S", "M", "L").forEach { FilterChip(selected = state.textSize == it, onClick = { viewModel.textSize(it) }, label = { Text(it) }) } } }
            item { Text(stringResource(R.string.settings_heatmap_style), style = MaterialTheme.typography.titleMedium) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("default", "cb").forEach { style -> FilterChip(selected = state.heatmapStyle == style, onClick = { viewModel.heatmap(style) }, label = { Text(if (style == "cb") stringResource(R.string.settings_heatmap_cb) else stringResource(R.string.settings_heatmap_default)) }) } } }
            item { OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_clear_history)) } }
            item { OutlinedButton(onClick = onAboutClick, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.about_title)) } }
        }
    }
    if (showStates) AlertDialog(onDismissRequest = { showStates = false; selectedState = null }, title = { Text(stringResource(R.string.settings_choose_state_district)) }, text = {
        val states = state.districts.map { it.state }.distinct().sorted()
        Column(Modifier.heightIn(max = 420.dp)) {
            if (selectedState == null) LazyColumn { items(states) { name -> ListItem(headlineContent = { Text(name) }, modifier = Modifier.clickable { selectedState = name }) } }
            else { TextButton(onClick = { selectedState = null }) { Text("← ${stringResource(R.string.state_select)}") }; LazyColumn { items(state.districts.filter { it.state == selectedState }.sortedBy { it.district }) { d -> ListItem(headlineContent = { Text(d.district) }, modifier = Modifier.clickable { viewModel.chooseDistrict(d); showStates = false; selectedState = null }) } } }
        }
    }, confirmButton = { TextButton(onClick = { showStates = false; selectedState = null }) { Text(stringResource(R.string.cancel)) } })
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text(stringResource(R.string.clear_history_confirm)) }, text = { Text(stringResource(R.string.clear_history_confirm)) }, confirmButton = { TextButton(onClick = { viewModel.clearHistory(); confirmClear = false }) { Text(stringResource(R.string.confirm)) } }, dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } })
}
