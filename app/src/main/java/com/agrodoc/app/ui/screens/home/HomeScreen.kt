package com.agrodoc.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agrodoc.app.R
import com.agrodoc.app.domain.model.DailyForecast
import com.agrodoc.app.domain.model.RiskLevel
import com.agrodoc.app.domain.model.WeatherData
import com.agrodoc.app.data.db.top1Label
import com.agrodoc.app.data.db.top1Prob
import com.agrodoc.app.ui.theme.Green900

@Composable
fun HomeScreen(
    onScanClick: () -> Unit,
    onHistoryItemClick: (Long) -> Unit,
    onNavigateToCrops: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // App Bar & Location Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "AgroDoc",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Green900,
                    )
                    Text(
                        text = uiState.selectedDistrict?.let { "${it.district}, ${it.state}" }
                            ?: stringResource(R.string.district_select),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.district_select), tint = Green900)
                }
            }
        }

        // Feature 1: Weather Card & 7-Day Strip
        item {
            WeatherCard(
                weather = uiState.weather,
                isStale = uiState.isWeatherStale,
                districtName = uiState.selectedDistrict?.district ?: "",
                onRefresh = { viewModel.refreshWeather() },
            )
        }

        // Disease Alerts Card (Secondary Signal from Weather)
        if (uiState.diseaseAlerts.isNotEmpty()) {
            item {
                DiseaseAlertsCard(alerts = uiState.diseaseAlerts)
            }
        }

        // Primary Scan & Gallery Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onScanClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green900),
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.scan_leaf),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                OutlinedButton(
                    onClick = onScanClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.Photo, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.from_gallery),
                        fontSize = 16.sp,
                    )
                }
            }
        }

        // My Crops Quick Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.my_crops),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onNavigateToCrops) {
                    Text(stringResource(R.string.crops_title), color = Green900)
                }
            }

            if (uiState.myCrops.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCrops() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Green900)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.add_to_my_crops),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.myCrops.forEach { crop ->
                        SuggestionChip(
                            onClick = onNavigateToCrops,
                            label = {
                                Text(
                                    when (uiState.currentLang) {
                                        "kn" -> crop.nameKn
                                        "hi" -> crop.nameHi
                                        "mr" -> crop.nameMr
                                        else -> crop.nameEn
                                    }
                                )
                            },
                            icon = { Icon(Icons.Default.Eco, contentDescription = null, tint = Green900, modifier = Modifier.size(16.dp)) },
                        )
                    }
                }
            }
        }

        // Recent Scans
        item {
            Text(
                text = stringResource(R.string.recent_scans),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (uiState.recentScans.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_recent_scans),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.recentScans) { scan ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHistoryItemClick(scan.id) },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = scan.top1Label,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "${stringResource(R.string.confidence)}: ${(scan.top1Prob * 100).toInt()}% • ${scan.cropKey}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AssistChip(
                            onClick = {},
                            label = { Text(scan.trustLevel.name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(
    weather: WeatherData?,
    isStale: Boolean,
    districtName: String,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = Green900)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.weather_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.weather_pull_to_refresh))
                }
            }

            if (weather != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${weather.current.tempC.toInt()}°C",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Green900,
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${stringResource(R.string.weather_humidity)}: ${weather.current.relativeHumidityPct}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "${stringResource(R.string.weather_wind)}: ${weather.current.windKph.toInt()} km/h",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (weather.daily.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        weather.daily.take(7).forEach { day ->
                            DailyWeatherItem(day)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.weather_offline_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun DailyWeatherItem(day: DailyForecast) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.date.takeLast(5), // MM-DD
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${day.tempMaxC.toInt()}° / ${day.tempMinC.toInt()}°",
            style = MaterialTheme.typography.bodySmall,
        )
        if (day.precipitationProbabilityPct > 20) {
            Text(
                text = "${day.precipitationProbabilityPct}% 🌧",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF1E88E5),
            )
        }
    }
}

@Composable
private fun DiseaseAlertsCard(alerts: List<com.agrodoc.app.domain.model.DiseaseRisk>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.disease_alerts),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            alerts.take(3).forEach { alert ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "${alert.cropId.replace("_", " ").capitalize()}: ${alert.diseaseName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = alert.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}
