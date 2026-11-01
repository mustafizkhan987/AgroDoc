package com.agrodoc.app.ui.screens.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrodoc.app.data.db.ScanDao
import com.agrodoc.app.data.db.ScanEntity
import com.agrodoc.app.R
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.domain.model.Scan
import com.agrodoc.app.domain.repository.KnowledgeRepository
import com.agrodoc.app.ml.AnalysisResult
import com.agrodoc.app.ml.Analyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

object ScanSession {
    var scan: Scan? = null
    var message: String? = null
    var persistedId: Long? = null
}

data class ScanFlowState(val isAnalyzing: Boolean = false, val error: String? = null)

@HiltViewModel
class ScanFlowViewModel @Inject constructor(
    private val analyzer: Analyzer,
    private val scans: ScanDao,
    private val knowledge: KnowledgeRepository,
    private val settings: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(ScanFlowState())
    val state = _state.asStateFlow()

    fun analyzeUri(uri: Uri, onFinished: () -> Unit) = viewModelScope.launch {
        _state.value = ScanFlowState(isAnalyzing = true)
        val bitmap = withContext(Dispatchers.IO) { decodeOrientedImage(uri) }
        if (bitmap == null) { _state.value = ScanFlowState(error = "Could not read this image. Choose another photo."); return@launch }
        analyze(bitmap, onFinished)
    }

    fun analyzeBitmap(bitmap: Bitmap, onFinished: () -> Unit) = viewModelScope.launch {
        _state.value = ScanFlowState(isAnalyzing = true)
        analyze(bitmap, onFinished)
    }

    private suspend fun analyze(bitmap: Bitmap, onFinished: () -> Unit) {
        val lang = settings.localeTag.first()
        val result = withContext(Dispatchers.Default) { analyzer.analyze(bitmap, lang) }
        when (result) {
            is AnalysisResult.MissingModel -> {
                ScanSession.scan = null
                ScanSession.persistedId = null
                val message = context.getString(R.string.model_missing)
                ScanSession.message = message
                _state.value = ScanFlowState(error = message)
                onFinished()
            }
            is AnalysisResult.Failed -> _state.value = ScanFlowState(error = result.error)
            is AnalysisResult.Success -> {
                val scan = result.scan
                ScanSession.scan = scan
                ScanSession.message = null
                try {
                    val files = withContext(Dispatchers.IO) { persistImages(scan.photo, scan.overlay) }
                    val predictedLabel = scan.top1.label
                    val disease = knowledge.getDiseases().firstOrNull { it.classId == scan.top1.classId || predictedLabel.contains(it.nameEn, true) }
                    val cropId = disease?.cropKey ?: predictedLabel.substringBefore(" - ").trim().lowercase().replace(" ", "_")
                    val topJson = JSONArray().apply { scan.top3.forEach { p -> put(JSONObject().put("classId", p.classId).put("label", p.label).put("prob", p.prob)) } }.toString()
                    ScanSession.persistedId = scans.insert(ScanEntity(createdAt = scan.createdAt, imagePath = files.first, overlayPath = files.second, cropKey = cropId, classId = scan.top1.classId, confidence = scan.top1.prob, top3Json = topJson, trustLevel = scan.trust, leafFocus = scan.leafFocus, modelVersion = scan.modelVersion, languageAtScan = lang, districtId = settings.selectedDistrictId.first()))
                } catch (_: Exception) { ScanSession.persistedId = null }
                _state.value = ScanFlowState()
                onFinished()
            }
        }
    }

    private fun persistImages(photo: Bitmap, overlay: Bitmap): Pair<String, String> {
        val dir = File(context.filesDir, "scans").apply { mkdirs() }
        fun write(name: String, bitmap: Bitmap, format: Bitmap.CompressFormat): String {
            val file = File(dir, name)
            file.outputStream().use { bitmap.compress(format, if (format == Bitmap.CompressFormat.JPEG) 90 else 100, it) }
            return file.absolutePath
        }
        val id = System.currentTimeMillis()
        return write("$id-photo.jpg", photo, Bitmap.CompressFormat.JPEG) to write("$id-overlay.png", overlay, Bitmap.CompressFormat.PNG)
    }

    private fun decodeOrientedImage(uri: Uri): Bitmap? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val orientation = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> { matrix.setRotate(180f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
