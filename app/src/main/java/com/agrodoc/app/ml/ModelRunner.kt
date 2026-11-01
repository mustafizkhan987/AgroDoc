package com.agrodoc.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.json.JSONArray
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.min

sealed class ModelRunnerResult {
    data class Success(
        val logits: FloatArray,
        val features: Array<Array<FloatArray>>,
        val labels: List<String>,
        val camWeights: FloatArray,
        val pixels: IntArray,
        val squareBitmap: Bitmap,
    ) : ModelRunnerResult()

    data class Error(
        val message: String,
        val isMissingModel: Boolean = false,
    ) : ModelRunnerResult()
}

class ModelRunner(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String>? = null
    private var camWeights: FloatArray? = null
    private var isInitialized = false
    private var initError: String? = null

    init {
        tryLoadModel()
    }

    fun isModelLoaded(): Boolean = isInitialized && interpreter != null

    private fun tryLoadModel() {
        try {
            val fd = context.assets.openFd("agrodoc_model.tflite")
            val map = FileInputStream(fd.fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            interpreter = Interpreter(map)

            val jsonArray = JSONArray(context.assets.open("labels.json").bufferedReader().readText())
            labels = List(jsonArray.length()) {
                jsonArray.getString(it).replace("___", " - ").replace('_', ' ')
            }

            val bytes = context.assets.open("cam_weights.bin").readBytes()
            camWeights = FloatArray(bytes.size / 4).also {
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(it)
            }
            isInitialized = true
        } catch (e: Exception) {
            isInitialized = false
            initError = e.message ?: "Model files not found"
        }
    }

    fun runInference(src: Bitmap): ModelRunnerResult {
        if (!isInitialized || interpreter == null || labels == null || camWeights == null) {
            return ModelRunnerResult.Error(
                message = initError ?: "Diagnostic model is not bundled. Please use the Knowledge Library for manual crop diagnosis.",
                isMissingModel = true,
            )
        }

        val interp = interpreter!!
        val lbls = labels!!
        val weights = camWeights!!

        return try {
            // Centre-crop to square
            val s = min(src.width, src.height)
            val sq = Bitmap.createBitmap(src, (src.width - s) / 2, (src.height - s) / 2, s, s)
            val bmp = Bitmap.createScaledBitmap(sq, 224, 224, true)
            val px = IntArray(224 * 224).also { bmp.getPixels(it, 0, 224, 0, 0, 224, 224) }

            // 0..255 pixel float buffer
            val inp = ByteBuffer.allocateDirect(224 * 224 * 3 * 4).order(ByteOrder.nativeOrder())
            for (p in px) {
                inp.putFloat(Color.red(p).toFloat())
                inp.putFloat(Color.green(p).toFloat())
                inp.putFloat(Color.blue(p).toFloat())
            }
            inp.rewind()

            val n = lbls.size
            val logits = Array(1) { FloatArray(n) }
            val feats = Array(1) { Array(7) { Array(7) { FloatArray(1280) } } }
            val outs = HashMap<Int, Any>()
            for (i in 0 until interp.outputTensorCount) {
                outs[i] = if (interp.getOutputTensor(i).shape().size == 2) logits else feats
            }
            interp.runForMultipleInputsOutputs(arrayOf<Any>(inp), outs)

            ModelRunnerResult.Success(
                logits = logits[0],
                features = feats[0],
                labels = lbls,
                camWeights = weights,
                pixels = px,
                squareBitmap = sq,
            )
        } catch (e: Exception) {
            ModelRunnerResult.Error(
                message = "Inference error: ${e.localizedMessage}",
                isMissingModel = false,
            )
        }
    }
}

