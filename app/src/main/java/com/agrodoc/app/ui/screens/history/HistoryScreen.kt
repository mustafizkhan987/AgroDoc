package com.agrodoc.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.agrodoc.app.R
import com.agrodoc.app.data.db.ScanDao
import com.agrodoc.app.data.db.ScanEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File

@HiltViewModel
class HistoryViewModel @Inject constructor(private val dao: ScanDao) : ViewModel() {
    val scans = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun delete(scan: ScanEntity) { viewModelScope.launch { dao.delete(scan.id); File(scan.imagePath).delete(); File(scan.overlayPath).delete() } }
    fun clear() { viewModelScope.launch { scans.value.forEach { File(it.imagePath).delete(); File(it.overlayPath).delete() }; dao.deleteAll() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onItemClick: (Long) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val scans by viewModel.scans.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }, actions = { if (scans.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.clear_history)) } }) }) { padding ->
        if (scans.isEmpty()) Box(Modifier.fillMaxSize().padding(padding), androidx.compose.ui.Alignment.Center) { Text(stringResource(R.string.history_empty)) }
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(scans, key = { it.id }) { scan ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { onItemClick(scan.id) }) {
                    ListItem(headlineContent = { Text(scan.cropKey.replace('_', ' ').replaceFirstChar { it.uppercase() }) }, supportingContent = { Text("${scan.trustLevel} • ${(scan.confidence * 100).toInt()}% • ${java.text.DateFormat.getDateTimeInstance().format(scan.createdAt)}") }, trailingContent = { TextButton(onClick = { viewModel.delete(scan) }) { Text(stringResource(R.string.delete_scan)) } })
                }
            }
        }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text(stringResource(R.string.clear_history_confirm)) }, text = { Text(stringResource(R.string.clear_history_confirm)) }, confirmButton = { TextButton(onClick = { viewModel.clear(); confirmClear = false }) { Text(stringResource(R.string.confirm)) } }, dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } })
}
