package com.agrodoc.app.ml

import android.graphics.Bitmap
import android.graphics.Color

data class CamResult(
    val rawCam: Array<FloatArray>,
    val camMax: Float,
    val leafFocus: Float,
    val leafAreaRatio: Float,
    val overlay: Bitmap,
)

object CamGenerator {

    fun generateCam(
        feats: Array<Array<FloatArray>>, // [7][7][1280]
        weights: FloatArray,            // [1280 * numClasses]
        classIndex: Int,
        numClasses: Int,
        px: IntArray,                   // [224 * 224]
        targetOverlaySize: Int = 512,
    ): CamResult {
        // 1. Calculate 7x7 CAM using ReLU(sum_k f[k] * W[k, c])
        val cam = Array(7) { i ->
            FloatArray(7) { j ->
                var acc = 0f
                val f = feats[i][j]
                for (k in 0 until 1280) {
                    acc += f[k] * weights[k * numClasses + classIndex]
                }
                maxOf(acc, 0f)
            }
        }
        val camMax = cam.maxOf { it.max() }.coerceAtLeast(1e-6f)

        // 2. Compute HSV leaf mask per 7x7 cell
        val hsv = FloatArray(3)
        val cellLeaf = Array(7) { FloatArray(7) }
        var leafPx = 0
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                Color.colorToHSV(px[y * 224 + x], hsv)
                // Green-to-yellow range in HSV with sufficient saturation & value
                if (hsv[1] > 0.25f && hsv[2] > 0.15f && hsv[0] in 20f..170f) {
                    cellLeaf[y / 32][x / 32] += 1f / 1024f
                    leafPx++
                }
            }
        }

        // 3. Compute leaf focus
        var num = 0f
        var den = 1e-6f
        for (i in 0..6) {
            for (j in 0..6) {
                val v = cam[i][j] / camMax
                num += v * cellLeaf[i][j]
                den += v
            }
        }
        val focus = num / den
        val areaRatio = leafPx / (224f * 224f)

        // 4. Render 7x7 heatmap and scale up
        val small = Bitmap.createBitmap(7, 7, Bitmap.Config.ARGB_8888)
        for (i in 0..6) {
            for (j in 0..6) {
                val v = cam[i][j] / camMax
                small.setPixel(
                    j, i,
                    Color.HSVToColor(
                        (v * 190).toInt(),
                        floatArrayOf((1f - v) * 240f, 1f, 1f)
                    )
                )
            }
        }
        val overlay = Bitmap.createScaledBitmap(small, targetOverlaySize, targetOverlaySize, true)

        return CamResult(
            rawCam = cam,
            camMax = camMax,
            leafFocus = focus,
            leafAreaRatio = areaRatio,
            overlay = overlay,
        )
    }
}

