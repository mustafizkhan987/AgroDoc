package com.agrodoc.app.ui.navigation

/** Compile-time route constants used by Navigation-Compose. */
object NavRoutes {
    const val LANGUAGE = "language"
    const val HOME = "home"
    const val CROPS = "crops"
    const val CROP_DETAIL = "crop_detail/{cropId}"
    const val LIBRARY = "library"
    const val DISEASE_DETAIL = "disease_detail/{diseaseId}"
    const val ASSISTANT = "assistant"
    const val FERTILIZER = "fertilizer"
    const val CAMERA = "camera"
    const val RESULT = "result"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun cropDetail(cropId: String) = "crop_detail/$cropId"
    fun diseaseDetail(diseaseId: String) = "disease_detail/$diseaseId"
    fun fertilizer(cropId: String? = null) = if (cropId != null) "fertilizer?cropId=$cropId" else "fertilizer"
    fun result(scanId: Long? = null) = if (scanId != null) "result?scanId=$scanId" else "result"
}
