package com.agrodoc.app.ui.screens.result

import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.unit.IntSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrodoc.app.data.db.ScanDao
import com.agrodoc.app.data.db.ScanEntity
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.domain.repository.WeatherRepository
import com.agrodoc.app.domain.repository.KnowledgeRepository
import com.agrodoc.app.ui.screens.camera.ScanSession
import com.agrodoc.app.domain.model.TrustLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject
import java.io.File
import java.util.Locale

data class SavedPrediction(val label: String, val probability: Float)
data class ResultState(val row: ScanEntity? = null, val photo: android.graphics.Bitmap? = null, val overlay: android.graphics.Bitmap? = null, val predictions: List<SavedPrediction> = emptyList(), val guidance: String? = null, val source: String? = null, val voiceEnabled: Boolean = true, val heatmapStyle: String = "default")

@HiltViewModel
class ResultViewModel @Inject constructor(private val dao: ScanDao, private val knowledge: KnowledgeRepository, private val weather: WeatherRepository, private val settings: SettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow(ResultState())
    val state = _state.asStateFlow()
    fun load(id: Long?) { viewModelScope.launch {
        if (id == null) {
            val scan = ScanSession.scan
            if (scan == null) { _state.value = ResultState(guidance = ScanSession.message ?: "No diagnosis result is available. Add the trained model assets to enable photo diagnosis."); return@launch }
            val disease = knowledge.getDiseases().firstOrNull { it.classId == scan.top1.classId || scan.top1.label.contains(it.nameEn, true) }
            val crop = disease?.let { knowledge.getCropById(it.cropKey) }
            val report = disease?.let {
                val districtId = settings.selectedDistrictId.first()
                val weatherSnapshot = weather.observeWeather(districtId).first()
                com.agrodoc.app.domain.engine.ExplanationGenerator.generateExplanation(crop, it, weatherSnapshot, scan.languageAtScan, scan.top1.prob)
            }
            _state.value = ResultState(photo = scan.photo, overlay = scan.overlay, predictions = scan.top3.map { SavedPrediction(it.label, it.prob) }, guidance = report?.let { "${it.summary}\n\n${it.whatCausedIt}\n\n${it.weatherInfluence}\n\nManagement: ${it.immediateSteps.joinToString("; ")}\n\nPrevention: ${it.longTermPrevention.joinToString("; ")}\n\n${it.officerAdvisory}" }, source = disease?.source, voiceEnabled = settings.voiceEnabled.first(), heatmapStyle = settings.heatmapStyle.first())
        } else {
            val row = dao.getById(id) ?: return@launch
            val array = JSONArray(row.top3Json)
            val preds = (0 until array.length()).map { array.getJSONObject(it).let { obj -> SavedPrediction(obj.optString("label"), obj.optDouble("prob").toFloat()) } }
            val disease = knowledge.getDiseases().firstOrNull { it.classId == row.classId }
            val crop = disease?.let { knowledge.getCropById(it.cropKey) }
            val weatherSnapshot = row.districtId?.let { weather.observeWeather(it).first() }
            val report = disease?.let { com.agrodoc.app.domain.engine.ExplanationGenerator.generateExplanation(crop, it, weatherSnapshot, row.languageAtScan, row.confidence) }
            val details = report?.let { "${it.summary}\n\n${it.whatCausedIt}\n\n${it.weatherInfluence}\n\nManagement: ${it.immediateSteps.joinToString("; ")}\n\nPrevention: ${it.longTermPrevention.joinToString("; ")}\n\n${it.officerAdvisory}" }
            _state.value = ResultState(row, BitmapFactory.decodeFile(row.imagePath), BitmapFactory.decodeFile(row.overlayPath), preds, details, disease?.source, settings.voiceEnabled.first(), settings.heatmapStyle.first())
        }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(onRetake: () -> Unit, onBack: () -> Unit, scanId: Long? = null, viewModel: ResultViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var blend by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val tts = remember(context) { TextToSpeech(context, null) }
    var voiceMessage by remember { mutableStateOf<String?>(null) }
    DisposableEffect(tts) { onDispose { tts.stop(); tts.shutdown() } }
    LaunchedEffect(scanId) { viewModel.load(scanId) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(com.agrodoc.app.R.string.result_title)) }, navigationIcon = { IconButton(onClick = onBack) { Text("←") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.photo != null && state.overlay != null) {
                val photo = state.photo!!.asImageBitmap()
                val overlay = state.overlay!!.asImageBitmap()
                Canvas(Modifier.fillMaxWidth().height(320.dp)) {
                    val dst = IntSize(size.width.toInt(), size.height.toInt())
                    drawImage(photo, dstSize = dst)
                    if (blend > 0f) drawImage(overlay, dstSize = dst, alpha = blend, colorFilter = if (state.heatmapStyle == "cb") ColorFilter.tint(Color(0xFF00BCD4), BlendMode.SrcAtop) else null)
                }
                Text("${stringResource(com.agrodoc.app.R.string.photo)}  •  ${stringResource(com.agrodoc.app.R.string.heatmap)}")
                Slider(value = blend, onValueChange = { blend = it }, valueRange = 0f..1f)
            } else {
                state.photo?.let { Image(it.asImageBitmap(), contentDescription = "Scanned leaf photo", modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) }
            }
            val top = state.predictions.firstOrNull()
            if (top != null) {
                val trust = state.row?.trustLevel ?: ScanSession.scan?.trust
                val trustRes = when (trust) { TrustLevel.HIGH -> com.agrodoc.app.R.string.trust_high; TrustLevel.MEDIUM -> com.agrodoc.app.R.string.trust_medium; TrustLevel.LOW -> com.agrodoc.app.R.string.trust_low; else -> com.agrodoc.app.R.string.trust_low }
                Text(top.label, style = MaterialTheme.typography.headlineSmall)
                Text("${stringResource(com.agrodoc.app.R.string.result_model_confidence, (top.probability * 100).toInt())} • ${stringResource(trustRes)}")
                if ((state.row?.confidence ?: ScanSession.scan?.top1?.prob ?: 1f) < 0.6f) Text(stringResource(com.agrodoc.app.R.string.low_trust_caution), color = MaterialTheme.colorScheme.error)
                Text(stringResource(com.agrodoc.app.R.string.result_other_predictions), style = MaterialTheme.typography.titleMedium)
                state.predictions.drop(1).forEach { Text("${it.label} — ${(it.probability * 100).toInt()}%") }
            }
            state.guidance?.let { Text(it) }
            state.source?.let { Text("Source: $it", style = MaterialTheme.typography.bodySmall) }
            if (top != null) Text(stringResource(com.agrodoc.app.R.string.result_screening_notice), style = MaterialTheme.typography.bodySmall)
            voiceMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (top != null && state.voiceEnabled) OutlinedButton(onClick = {
                val tag = ScanSession.scan?.languageAtScan ?: state.row?.languageAtScan ?: "en"
                val locale = when (tag) { "kn" -> Locale("kn", "IN"); "hi" -> Locale("hi", "IN"); "mr" -> Locale("mr", "IN"); else -> Locale.ENGLISH }
                val availability = tts.setLanguage(locale)
                if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) voiceMessage = context.getString(com.agrodoc.app.R.string.result_tts_missing)
                else { voiceMessage = null; tts.speak("${top.label}. ${(top.probability * 100).toInt()} percent confidence.", TextToSpeech.QUEUE_FLUSH, null, "agrodoc-result") }
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(com.agrodoc.app.R.string.speak)) }
            if (top != null || state.guidance != null) Button(onClick = onRetake, modifier = Modifier.fillMaxWidth()) { Text(stringResource(com.agrodoc.app.R.string.retake)) }
        }
    }
}
