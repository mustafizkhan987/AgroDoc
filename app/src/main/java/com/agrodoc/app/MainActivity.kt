package com.agrodoc.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.ui.navigation.MainScaffold
import com.agrodoc.app.ui.navigation.NavRoutes
import com.agrodoc.app.ui.theme.AgroDocTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity host for the entire AgroDoc UI.
 *
 * On creation it reads the persisted locale and language-chosen flag from
 * DataStore (blocking briefly on the main thread only until the first emission,
 * which is virtually instant for an in-process read).  The Compose tree is then
 * set with the correct start destination so the language screen only shows on
 * first launch.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply the persisted locale before the UI is drawn so there is no
        // visual flash of the wrong language.
        lifecycleScope.launch {
            val tag = settings.localeTag.first()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }

        setContent {
            AgroDocTheme {
                var textSize by remember { mutableStateOf("M") }
                LaunchedEffect(Unit) { settings.textSize.collect { textSize = it } }
                val baseDensity = LocalDensity.current
                // Collect the one-shot "language chosen" flag.  While the value
                // is loading we start on the language screen as a safe default.
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val chosen = settings.languageChosen.first()
                    startDestination = if (chosen) NavRoutes.HOME else NavRoutes.LANGUAGE
                }

                startDestination?.let { start ->
                    val scale = when (textSize) { "S" -> 0.9f; "L" -> 1.15f; else -> 1f }
                    CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * scale)) {
                        MainScaffold(startDestination = start)
                    }
                }
            }
        }
    }
}
