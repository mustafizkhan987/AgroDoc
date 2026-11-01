package com.agrodoc.app.domain.model

import android.graphics.Bitmap

/**
 * Represents one complete leaf-scan result, as used inside the app's domain layer.
 * The bitmaps are held in memory only for the current result screen; the
 * persisted version stores file paths in [com.agrodoc.app.data.db.ScanEntity].
 */
data class Scan(
    /** Database row id; 0 before the row is inserted. */
    val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),

    /** Original (downscaled, EXIF-corrected) photo. */
    val photo: Bitmap,
    /** Heatmap overlay composited on the photo. */
    val overlay: Bitmap,
    /** Raw 7×7 CAM float array, row-major [0..1]. */
    val rawCam: FloatArray,

    val top1: Prediction,
    val top3: List<Prediction>,

    val trust: TrustLevel,
    /** Ratio of CAM mass that falls inside the leaf mask (0–1). */
    val leafFocus: Float,
    /** Ratio of leaf pixels to total image pixels (0–1). */
    val leafAreaRatio: Float,
    /** The class index used to generate the CAM. */
    val camClassId: Int,

    /** Free-form explanation text in the user's current locale. */
    val explanationText: String,

    /** Version tag from model_meta.json, e.g. "v1.0". */
    val modelVersion: String,
    /** BCP-47 locale tag at the time of the scan, e.g. "kn". */
    val languageAtScan: String,
)
