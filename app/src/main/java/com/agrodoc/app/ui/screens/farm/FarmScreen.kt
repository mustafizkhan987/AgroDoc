package com.agrodoc.app.ui.screens.farm

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.agrodoc.app.R
import com.agrodoc.app.data.db.PlotEntity
import com.agrodoc.app.ui.theme.Green900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmScreen(
    onNavigateToFertilizer: (String) -> Unit = {},
    viewModel: FarmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddPlotDialog by remember { mutableStateOf(false) }
    var showAddLogDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Farm", fontWeight = FontWeight.Bold, color = Green900) },
                actions = {
                    if (uiState.selectedPlot != null) {
                        IconButton(
                            onClick = {
                                val file = viewModel.exportFarmReportCsv(context)
                                if (file != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file,
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Farm Report"))
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export Report", tint = Green900)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPlotDialog = true },
                containerColor = Green900,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Plot")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (uiState.plots.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Agriculture,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Green900.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No plots added yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Add your crop plots to track growth, weather, and fertilizer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Text(
                            text = "Your Plots",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            uiState.plots.forEach { plot ->
                                val isSelected = uiState.selectedPlot?.id == plot.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectPlot(plot) },
                                    label = { Text(plot.name) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Eco,
                                            contentDescription = null,
                                            tint = if (isSelected) Green900 else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                )
                            }
                        }
                    }

                    uiState.selectedPlot?.let { plot ->
                        item {
                            PlotDashboardCard(
                                plot = plot,
                                onCalculateFertilizer = { onNavigateToFertilizer(plot.cropId) },
                                onAddLog = { showAddLogDialog = true },
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Activity & Farm Logs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                TextButton(onClick = { showAddLogDialog = true }) {
                                    Text("+ Add Log", color = Green900)
                                }
                            }
                        }

                        if (uiState.selectedPlotLogs.isEmpty()) {
                            item {
                                Text(
                                    text = "No logs recorded for this plot yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            items(uiState.selectedPlotLogs) { log ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = log.logType.capitalize(),
                                                fontWeight = FontWeight.Bold,
                                                color = Green900,
                                            )
                                            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                            Text(
                                                text = sdf.format(Date(log.loggedAtEpoch)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(text = log.note, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPlotDialog) {
        AddPlotDialog(
            crops = uiState.crops,
            currentLang = uiState.currentLang,
            onDismiss = { showAddPlotDialog = false },
            onConfirm = { name, cropId, variety, area, unit, irrigation ->
                viewModel.addPlot(
                    name = name,
                    cropId = cropId,
                    variety = variety,
                    areaValue = area,
                    areaUnit = unit,
                    sowingEpoch = System.currentTimeMillis() - (15 * 24 * 3600 * 1000L), // default 15 days ago
                    irrigationType = irrigation,
                )
                showAddPlotDialog = false
            }
        )
    }

    if (showAddLogDialog && uiState.selectedPlot != null) {
        AddLogDialog(
            onDismiss = { showAddLogDialog = false },
            onConfirm = { type, note ->
                viewModel.addLog(uiState.selectedPlot!!.id, type, note)
                showAddLogDialog = false
            }
        )
    }
}

@Composable
private fun PlotDashboardCard(
    plot: PlotEntity,
    onCalculateFertilizer: () -> Unit,
    onAddLog: () -> Unit,
) {
    val daysAfterSowing = ((System.currentTimeMillis() - plot.sowingDateEpoch) / (24 * 3600 * 1000L)).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(text = plot.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${plot.cropId.replace("_", " ").capitalize()} • ${plot.areaValue} ${plot.areaUnit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Green900),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$daysAfterSowing",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                        Text(text = "DAS", color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onCalculateFertilizer,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Green900),
                ) {
                    Text("Fertilizer", fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = onAddLog,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Log Entry", fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlotDialog(
    crops: List<com.agrodoc.app.domain.model.CropInfo>,
    currentLang: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedCropId by remember { mutableStateOf(crops.firstOrNull()?.id ?: "paddy") }
    var variety by remember { mutableStateOf("") }
    var areaText by remember { mutableStateOf("1.0") }
    var areaUnit by remember { mutableStateOf("acre") }
    var irrigation by remember { mutableStateOf("rainfed") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Plot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Plot Name (e.g. North Field)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = variety,
                    onValueChange = { variety = it },
                    label = { Text("Variety / Hybrid (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = areaText,
                        onValueChange = { areaText = it },
                        label = { Text("Area") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    var unitExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = areaUnit.capitalize(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            listOf("acre", "hectare", "guntha").forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u.capitalize()) },
                                    onClick = { areaUnit = u; unitExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val area = areaText.toDoubleOrNull() ?: 1.0
                    val plotName = name.ifBlank { "Field 1" }
                    onConfirm(plotName, selectedCropId, variety, area, areaUnit, irrigation)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green900),
            ) {
                Text("Save Plot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var logType by remember { mutableStateOf("observation") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Farm Log") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("observation", "fertilizer", "work").forEach { t ->
                        FilterChip(
                            selected = logType == t,
                            onClick = { logType = t },
                            label = { Text(t.capitalize()) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Observation / Work details") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (note.isNotBlank()) onConfirm(logType, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green900),
            ) {
                Text("Add Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
