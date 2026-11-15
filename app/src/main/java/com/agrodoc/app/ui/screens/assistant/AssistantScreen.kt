package com.agrodoc.app.ui.screens.assistant

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrodoc.app.R
import com.agrodoc.app.data.prefs.SettingsRepository
import com.agrodoc.app.domain.engine.AssistantAnswer
import com.agrodoc.app.domain.engine.KnowledgeRetriever
import com.agrodoc.app.domain.repository.AdvisorEngine
import com.agrodoc.app.domain.repository.KnowledgeRepository
import com.agrodoc.app.ui.theme.Green900
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val knowledge: KnowledgeRepository,
    private val settings: SettingsRepository,
    private val advisorEngine: AdvisorEngine,
) : ViewModel() {

    private val _answer = MutableStateFlow<AssistantAnswer?>(null)
    val answer = _answer.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun ask(question: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val lang = settings.language.first() ?: "kn"
            val crops = knowledge.getCrops()
            val diseases = knowledge.getDiseases()

            val rawAnswer = KnowledgeRetriever.answerQuery(question, crops, diseases, lang)
            val consultedText = advisorEngine.consult(question, rawAnswer, lang)

            _answer.value = rawAnswer.copy(answer = consultedText)
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel = hiltViewModel()) {
    val answer by viewModel.answer.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var question by remember { mutableStateOf("") }
    val context = LocalContext.current

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialized
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    val starterPrompts = listOf(
        "Ragi sowing time in Karnataka",
        "Tomato late blight prevention",
        "Paddy blast disease treatment",
        "Fertilizer dose for maize",
        "Coconut koleroga control",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = Green900)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.assistant_title), fontWeight = FontWeight.Bold, color = Green900)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = stringResource(R.string.assistant_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            item {
                Text(
                    text = "Quick Questions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Green900,
                )
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(starterPrompts) { prompt ->
                        SuggestionChip(
                            onClick = {
                                question = prompt
                                viewModel.ask(prompt)
                            },
                            label = { Text(prompt, fontSize = 12.sp) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.assistant_ask_hint)) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (question.isNotBlank()) {
                                    viewModel.ask(question)
                                }
                            },
                            enabled = question.isNotBlank() && !isLoading,
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Ask", tint = Green900)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                )
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Green900)
                    }
                }
            }

            answer?.let { a ->
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Response",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Green900,
                                )
                                IconButton(
                                    onClick = {
                                        tts?.speak(a.answer, TextToSpeech.QUEUE_FLUSH, null, "agrodoc_tts")
                                    }
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Read Aloud", tint = Green900)
                                }
                            }

                            Text(
                                text = a.answer,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 22.sp,
                            )

                            if (a.source.isNotBlank()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "${stringResource(R.string.assistant_sources)}: ${a.source}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (a.suggestedFollowUps.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.assistant_suggested),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    items(a.suggestedFollowUps) { suggestion ->
                        OutlinedButton(
                            onClick = {
                                question = suggestion
                                viewModel.ask(suggestion)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(suggestion, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
