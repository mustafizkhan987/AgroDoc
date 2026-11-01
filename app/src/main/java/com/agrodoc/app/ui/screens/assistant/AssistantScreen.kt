package com.agrodoc.app.ui.screens.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.agrodoc.app.R
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.domain.engine.AssistantAnswer
import com.agrodoc.app.domain.engine.KnowledgeRetriever
import com.agrodoc.app.domain.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(private val knowledge: KnowledgeRepository, private val settings: SettingsRepository) : ViewModel() {
    private val _answer = MutableStateFlow<AssistantAnswer?>(null)
    val answer = _answer.asStateFlow()
    fun ask(question: String) { viewModelScope.launch {
        val lang = settings.localeTag.first()
        _answer.value = KnowledgeRetriever.answerQuery(question, knowledge.getCrops(), knowledge.getDiseases(), lang)
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel = hiltViewModel()) {
    val answer by viewModel.answer.collectAsState()
    var question by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.assistant_title)) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.assistant_empty), style = MaterialTheme.typography.bodyMedium) }
            item { OutlinedTextField(question, { question = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.assistant_ask_hint)) }, minLines = 2) }
            item { Button(onClick = { viewModel.ask(question) }, enabled = question.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.assistant_send)) } }
            answer?.let { a ->
                item { ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(a.answer); if (a.source.isNotBlank()) Text("${stringResource(R.string.assistant_sources)}: ${a.source}", style = MaterialTheme.typography.bodySmall) } } }
                a.suggestedFollowUps.forEach { suggestion -> item { SuggestionChip(onClick = { question = suggestion; viewModel.ask(suggestion) }, label = { Text(suggestion) }) } }
            }
        }
    }
}
