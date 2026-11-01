package com.agrodoc.app.ui.screens.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agrodoc.app.R

/**
 * About / Model card screen — Phase 0 skeleton.
 * Real metrics from model_meta.json, limitation descriptions and licence
 * text will be populated in Phase 6.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.about_datasets))
            Text(stringResource(R.string.about_plantvillage_credit))
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.about_plantdoc_credit))

            Spacer(Modifier.height(24.dp))
            SectionHeader(stringResource(R.string.about_metrics))
            Text("No validated model metrics are available because the trained TFLite model and evaluation report are not bundled yet.")

            Spacer(Modifier.height(24.dp))
            SectionHeader(stringResource(R.string.about_limitations))
            Text(
                "• Photo diagnosis remains unavailable until model files are installed.\n" +
                "• Disease library entries marked information-only are not diagnosed by the image model.\n" +
                "• Weather and rule-based guidance are secondary signals, not a confirmed diagnosis.\n" +
                "• Confirm pesticide choice, dose, and pre-harvest interval against the current product label and local agriculture officer."
            )

            Spacer(Modifier.height(24.dp))
            SectionHeader(stringResource(R.string.about_licences))
            Text(stringResource(R.string.about_plantvillage_credit))
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.about_plantdoc_credit))

            Spacer(Modifier.height(24.dp))
            SectionHeader("Weather and privacy")
            Text(stringResource(if (com.agrodoc.app.BuildConfig.FLAVOR == "connected") R.string.privacy_statement_connected else R.string.privacy_statement_offline))
            if (com.agrodoc.app.BuildConfig.FLAVOR == "connected") {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.weather_attribution))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
}
