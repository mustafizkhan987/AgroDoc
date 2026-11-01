package com.agrodoc.app.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.agrodoc.app.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrodoc.app.domain.model.DiseaseInfo
import com.agrodoc.app.domain.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(private val knowledge: KnowledgeRepository) : ViewModel() {
    private val _diseases = MutableStateFlow<List<DiseaseInfo>>(emptyList())
    val diseases = _diseases.asStateFlow()
    init { viewModelScope.launch { _diseases.value = knowledge.getDiseases() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onDiseaseClick: (String) -> Unit = {}, viewModel: LibraryViewModel = hiltViewModel()) {
    val diseases by viewModel.diseases.collectAsState()
    val lang = LocalConfiguration.current.locales[0]?.language ?: "en"
    var query by remember { mutableStateOf("") }
    val filtered = diseases.filter { it.nameEn.contains(query, true) || it.nameKn.contains(query, true) || it.nameHi.contains(query, true) || it.nameMr.contains(query, true) || it.cropNameEn.contains(query, true) || it.cropKey.contains(query, true) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.library_title)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.crops_search)) }, singleLine = true)
            if (diseases.isEmpty()) Box(Modifier.fillMaxSize(), androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            else LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { disease ->
                    val name = when (lang) { "kn" -> disease.nameKn; "hi" -> disease.nameHi; "mr" -> disease.nameMr; else -> disease.nameEn }
                    val cropName = when (lang) { "kn" -> disease.cropNameKn; "hi" -> disease.cropNameHi; "mr" -> disease.cropNameMr; else -> disease.cropNameEn }
                    OutlinedCard(onClick = { onDiseaseClick(disease.id) }, modifier = Modifier.fillMaxWidth()) {
                        ListItem(headlineContent = { Text(name) }, supportingContent = { Text("${cropName.ifBlank { disease.cropKey }} • ${stringResource(if (disease.hasModelSupport) R.string.disease_model_supported else R.string.disease_info_only)}") }, trailingContent = { Text(disease.cause.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseDetailScreen(diseaseId: String, onBack: () -> Unit, viewModel: DiseaseDetailViewModel = hiltViewModel()) {
    val disease by viewModel.get(diseaseId).collectAsState(initial = null)
    val lang = LocalConfiguration.current.locales[0]?.language ?: "en"
    Scaffold(topBar = { TopAppBar(title = { Text(disease?.let { diseaseName(it, lang) } ?: stringResource(R.string.library_title)) }, navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) } }) }) { padding ->
        val d = disease
        if (d == null) Box(Modifier.fillMaxSize().padding(padding), androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("${diseaseCrop(d, lang).ifBlank { d.cropKey }} • ${d.cause} • ${stringResource(if (d.hasModelSupport) R.string.disease_model_supported else R.string.disease_info_only)}", style = MaterialTheme.typography.titleMedium) }
            item { GuideSection(stringResource(R.string.disease_symptoms), localized(d.symptomsEn, d.symptomsKn, d.symptomsHi, d.symptomsMr, lang)) }
            item { GuideSection(stringResource(R.string.disease_cause_spread), "${d.cause.replaceFirstChar { it.uppercase() }}. ${localized(d.spreadEn, d.spreadKn, d.spreadHi, d.spreadMr, lang)}") }
            item { GuideSection(stringResource(R.string.disease_weather), localized(d.favourableConditions.descriptionEn, d.favourableConditions.descriptionKn, d.favourableConditions.descriptionHi, d.favourableConditions.descriptionMr, lang).ifBlank { "—" }) }
            item { GuideSection(stringResource(R.string.disease_prevention), localized(d.preventionEn, d.preventionKn, d.preventionHi, d.preventionMr, lang)) }
            item { GuideSection(stringResource(R.string.disease_management), localized(d.treatmentEn, d.treatmentKn, d.treatmentHi, d.treatmentMr, lang)) }
            item { GuideSection(stringResource(R.string.disease_consult), d.whenToConsultOfficer.ifBlank { "—" }) }
            item { Text("Source: ${d.source.ifBlank { "Not listed" }} • ${d.reviewStatus}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun localized(en: String, kn: String, hi: String, mr: String, lang: String) = when (lang) { "kn" -> kn.ifBlank { en }; "hi" -> hi.ifBlank { en }; "mr" -> mr.ifBlank { en }; else -> en }
private fun diseaseName(d: DiseaseInfo, lang: String) = when (lang) { "kn" -> d.nameKn; "hi" -> d.nameHi; "mr" -> d.nameMr; else -> d.nameEn }
private fun diseaseCrop(d: DiseaseInfo, lang: String) = when (lang) { "kn" -> d.cropNameKn; "hi" -> d.cropNameHi; "mr" -> d.cropNameMr; else -> d.cropNameEn }

@HiltViewModel
class DiseaseDetailViewModel @Inject constructor(private val knowledge: KnowledgeRepository) : ViewModel() {
    fun get(id: String) = kotlinx.coroutines.flow.flow { emit(knowledge.getDiseaseById(id)) }
}

@Composable
private fun GuideSection(title: String, body: String) {
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(body.ifBlank { "Not specified in the knowledge base." }) } }
}
