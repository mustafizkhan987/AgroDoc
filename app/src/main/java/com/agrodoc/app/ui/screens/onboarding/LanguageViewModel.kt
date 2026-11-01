package com.agrodoc.app.ui.screens.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrodoc.app.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the language selection screen.
 * Persists the chosen locale tag to DataStore, then applies it via
 * [AppCompatDelegate.setApplicationLocales] — this causes the activity to
 * recreate itself with the new locale, but Navigation-Compose restores state.
 */
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    /**
     * Called when the user taps a language button.
     * @param tag    BCP-47 locale tag, e.g. "kn".
     * @param onDone Callback invoked after DataStore write to navigate forward.
     */
    fun pickLanguage(tag: String, onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setLanguageChosen(tag)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            onDone()
        }
    }
}
