package com.agrodoc.app.ui.screens.crops

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.DiseaseInfo
import com.agrodoc.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropDetailScreen(
    cropId: String,
    onDiseaseClick: (String) -> Unit,
    onFertilizerClick: (String) -> Unit,
    viewModel: CropsViewModel = hiltViewModel(),
) {
    var crop by remember(cropId) { mutableStateOf<CropInfo?>(null) }
    var diseases by remember(cropId) { mutableStateOf<List<DiseaseInfo>>(emptyList()) }
    val lang = LocalConfiguration.current.locales[0]?.language ?: "en"
    fun cropName(c: CropInfo) = when (lang) { "kn" -> c.nameKn; "hi" -> c.nameHi; "mr" -> c.nameMr; else -> c.nameEn }
    LaunchedEffect(cropId) {
        crop = viewModel.getCropDetail(cropId)
        diseases = viewModel.getDiseasesForCrop(cropId)
    }
    Scaffold(topBar = { TopAppBar(title = { Text(crop?.let(::cropName) ?: stringResource(R.string.crops_title)) }) }) { padding ->
        val c = crop
        if (c == null) Box(Modifier.fillMaxSize().padding(padding), androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("${c.category.replaceFirstChar { it.uppercase() }} • ${c.seasons.joinToString()}", style = MaterialTheme.typography.titleMedium) }
            item {
                val suitability = viewModel.getSuitability(c)
                ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.suitability_badge), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(suitability.headline)
                    suitability.reasons.forEach { Text(it) }
                } }
            }
            item { ElevatedCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.suitable_climate), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${stringResource(R.string.ideal_temp)}: ${c.tempOptMinC ?: c.tempMinC ?: "—"}–${c.tempOptMaxC ?: c.tempMaxC ?: "—"} °C")
                Text("${stringResource(R.string.annual_rainfall)}: ${c.annualRainfallMm?.let { "$it mm" } ?: "—"}")
                Text("${stringResource(R.string.soil_types)}: ${c.soilTypes.joinToString().ifBlank { "—" }}")
                Text("${stringResource(R.string.crop_soil_ph)}: ${c.soilPhMin ?: "—"}–${c.soilPhMax ?: "—"}")
                Text("District fit: ${c.karnatakaDistricts.joinToString().ifBlank { "Check local guidance" }}")
            } } }
            if (c.sowingWindows.isNotEmpty()) item { Text("Sowing windows", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(c.sowingWindows) { w -> ListItem(headlineContent = { Text("${w.season}: ${w.sowingMonths}") }, supportingContent = { Text("Harvest: ${w.harvestMonths}${w.notes?.let { " • $it" } ?: ""}") }) }
            if (c.growthStages.isNotEmpty()) item { Text(stringResource(R.string.growth_stages), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(c.growthStages) { stage ->
                val stageName = when (lang) { "kn" -> stage.nameKn ?: stage.name; "hi" -> stage.nameHi ?: stage.name; "mr" -> stage.nameMr ?: stage.name; else -> stage.name }
                val care = if (lang == "kn") stage.keyCareKn ?: stage.keyCare else stage.keyCare
                ListItem(headlineContent = { Text(stageName) }, supportingContent = { Text("${stage.startDayAfterSowing}–${stage.endDayAfterSowing} days after sowing • $care") })
            }
            item { Button(onClick = { onFertilizerClick(c.id) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calculate_fertilizer)) } }
            if (diseases.isNotEmpty()) item { Text(stringResource(R.string.view_diseases), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(diseases) { d ->
                val name = when (lang) { "kn" -> d.nameKn; "hi" -> d.nameHi; "mr" -> d.nameMr; else -> d.nameEn }
                OutlinedCard(onClick = { onDiseaseClick(d.id) }, modifier = Modifier.fillMaxWidth()) { ListItem(headlineContent = { Text(name) }, supportingContent = { Text(if (d.hasModelSupport) "Photo diagnosis class available" else "Information guide • no model class") }) }
            }
            item { Text(stringResource(R.string.crop_source_status, c.source.ifBlank { "—" }, c.reviewStatus), style = MaterialTheme.typography.bodySmall) }
        }
    }
}
