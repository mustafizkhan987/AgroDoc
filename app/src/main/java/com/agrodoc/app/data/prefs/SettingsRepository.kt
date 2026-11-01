package com.agrodoc.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "agrodoc_prefs")

/**
 * Single source of truth for user preferences, backed by DataStore.
 * All writes are suspend functions; reads are Flows.
 */
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val ctx: Context) {

    companion object {
        private val KEY_LANGUAGE_CHOSEN = booleanPreferencesKey("language_chosen")
        private val KEY_LOCALE_TAG = stringPreferencesKey("locale_tag")
        private val KEY_VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        private val KEY_TEXT_SIZE = stringPreferencesKey("text_size") // "S", "M", "L"
        private val KEY_HEATMAP_STYLE = stringPreferencesKey("heatmap_style") // "default", "cb"
        private val KEY_STATE = stringPreferencesKey("location_state")
        private val KEY_DISTRICT_ID = stringPreferencesKey("location_district_id")
        private val KEY_DISTRICT_NAME = stringPreferencesKey("location_district_name")
        private val KEY_LOCATION_LAT = stringPreferencesKey("location_latitude")
        private val KEY_LOCATION_LON = stringPreferencesKey("location_longitude")
    }

    /** True once the user has completed the first-launch language screen. */
    val languageChosen: Flow<Boolean> =
        ctx.dataStore.data.map { it[KEY_LANGUAGE_CHOSEN] ?: false }

    /** BCP-47 locale tag chosen by the user, e.g. "kn". Defaults to "kn" (Kannada first). */
    val localeTag: Flow<String> =
        ctx.dataStore.data.map { it[KEY_LOCALE_TAG] ?: "kn" }

    val voiceEnabled: Flow<Boolean> =
        ctx.dataStore.data.map { it[KEY_VOICE_ENABLED] ?: true }

    val textSize: Flow<String> =
        ctx.dataStore.data.map { it[KEY_TEXT_SIZE] ?: "M" }

    val heatmapStyle: Flow<String> =
        ctx.dataStore.data.map { it[KEY_HEATMAP_STYLE] ?: "default" }

    val selectedState: Flow<String> =
        ctx.dataStore.data.map { it[KEY_STATE] ?: "Karnataka" }

    val selectedDistrictId: Flow<String> =
        ctx.dataStore.data.map { it[KEY_DISTRICT_ID] ?: "karnataka_bengaluru_urban" }

    val selectedDistrictName: Flow<String> =
        ctx.dataStore.data.map { it[KEY_DISTRICT_NAME] ?: "Bengaluru Urban" }
    val selectedLatitude: Flow<Double?> = ctx.dataStore.data.map { it[KEY_LOCATION_LAT]?.toDoubleOrNull() }
    val selectedLongitude: Flow<Double?> = ctx.dataStore.data.map { it[KEY_LOCATION_LON]?.toDoubleOrNull() }

    suspend fun setLanguageChosen(tag: String) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE_CHOSEN] = true
            prefs[KEY_LOCALE_TAG] = tag
        }
    }

    suspend fun setLocaleTag(tag: String) {
        ctx.dataStore.edit { it[KEY_LOCALE_TAG] = tag }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        ctx.dataStore.edit { it[KEY_VOICE_ENABLED] = enabled }
    }

    suspend fun setTextSize(size: String) {
        ctx.dataStore.edit { it[KEY_TEXT_SIZE] = size }
    }

    suspend fun setHeatmapStyle(style: String) {
        ctx.dataStore.edit { it[KEY_HEATMAP_STYLE] = style }
    }

    suspend fun setLocation(state: String, districtId: String, districtName: String, latitude: Double? = null, longitude: Double? = null) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_STATE] = state
            prefs[KEY_DISTRICT_ID] = districtId
            prefs[KEY_DISTRICT_NAME] = districtName
            if (latitude != null && longitude != null) {
                prefs[KEY_LOCATION_LAT] = latitude.toString()
                prefs[KEY_LOCATION_LON] = longitude.toString()
            } else {
                prefs.remove(KEY_LOCATION_LAT)
                prefs.remove(KEY_LOCATION_LON)
            }
        }
    }
}
