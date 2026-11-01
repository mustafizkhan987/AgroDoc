package com.agrodoc.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agrodoc.app.domain.model.TrustLevel
import org.json.JSONArray

/**
 * Persisted scan row. Bitmaps are stored on disk; only paths are kept here.
 * top3Json is a JSON array string, e.g. [{"classId":0,"label":"…","prob":0.9},...].
 */
@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    /** Absolute path to the original (downscaled) photo file in app-private storage. */
    val imagePath: String,
    /** Absolute path to the saved heatmap overlay file in app-private storage. */
    val overlayPath: String,
    /** Short crop key, e.g. "tomato". */
    val cropKey: String,
    /** Top-1 class index. */
    val classId: Int,
    /** Top-1 softmax probability. */
    val confidence: Float,
    /** JSON representation of the top-3 predictions. */
    val top3Json: String,
    val trustLevel: TrustLevel,
    /** Leaf focus score (0–1). */
    val leafFocus: Float,
    /** Model version tag, e.g. "v1.0". */
    val modelVersion: String,
    /** BCP-47 locale tag at scan time. */
    val languageAtScan: String,
    /** Optional district ID at scan time. */
    val districtId: String? = null,
    /** Optional weather snapshot JSON at scan time. */
    val weatherSnapshotJson: String? = null,
)

val ScanEntity.top1Label: String
    get() = runCatching { JSONArray(top3Json).getJSONObject(0).optString("label") }.getOrNull().orEmpty().ifBlank { cropKey }

val ScanEntity.top1Prob: Float
    get() = runCatching { JSONArray(top3Json).getJSONObject(0).optDouble("prob", confidence.toDouble()).toFloat() }.getOrDefault(confidence)
