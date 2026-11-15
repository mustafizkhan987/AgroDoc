package com.agrodoc.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrodoc.app.domain.model.DailyForecast
import com.agrodoc.app.domain.model.WeatherData
import com.agrodoc.app.ui.theme.Green900

@Composable
fun TempographCard(
    weather: WeatherData?,
    modifier: Modifier = Modifier,
) {
    if (weather == null || weather.daily.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(0) }
    val days = weather.daily.take(7)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "14-Day Tempograph",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Green900,
                )
                selectedIndex?.let { idx ->
                    val d = days.getOrNull(idx)
                    if (d != null) {
                        Text(
                            text = "${d.date.takeLast(5)}: ${d.tempMaxC.toInt()}° / ${d.tempMinC.toInt()}°C • ${d.precipitationProbabilityPct}% Rain",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Canvas 14-day / 7-day tempograph chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .pointerInput(days) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / days.size
                            val idx = (offset.x / slotWidth).toInt().coerceIn(0, days.size - 1)
                            selectedIndex = idx
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val count = days.size
                val stepX = w / count

                val minTemp = (days.minOfOrNull { it.tempMinC } ?: 10.0).toFloat() - 2f
                val maxTemp = (days.maxOfOrNull { it.tempMaxC } ?: 35.0).toFloat() + 2f
                val tempRange = (maxTemp - minTemp).coerceAtLeast(1f)

                // 1. Draw Rain Bars at Bottom
                val maxRain = (days.maxOfOrNull { it.precipitationMm } ?: 10.0).toFloat().coerceAtLeast(5f)
                days.forEachIndexed { i, day ->
                    val barHeight = (day.precipitationMm.toFloat() / maxRain) * (h * 0.35f)
                    val barWidth = stepX * 0.45f
                    val x = i * stepX + (stepX - barWidth) / 2
                    val y = h - barHeight

                    if (day.precipitationMm > 0.5) {
                        drawRoundRect(
                            color = Color(0xFF64B5F6).copy(alpha = 0.7f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
                }

                // 2. Draw Temperature Band (Max temp curve and Min temp curve)
                val maxPath = Path()
                val minPath = Path()
                val bandPath = Path()

                days.forEachIndexed { i, day ->
                    val x = i * stepX + stepX / 2
                    val yMax = h * 0.15f + (1f - (day.tempMaxC.toFloat() - minTemp) / tempRange) * (h * 0.55f)
                    val yMin = h * 0.15f + (1f - (day.tempMinC.toFloat() - minTemp) / tempRange) * (h * 0.55f)

                    if (i == 0) {
                        maxPath.moveTo(x, yMax)
                        minPath.moveTo(x, yMin)
                        bandPath.moveTo(x, yMax)
                    } else {
                        maxPath.lineTo(x, yMax)
                        minPath.lineTo(x, yMin)
                        bandPath.lineTo(x, yMax)
                    }
                }

                // Close band path by going in reverse along min points
                for (i in days.indices.reversed()) {
                    val day = days[i]
                    val x = i * stepX + stepX / 2
                    val yMin = h * 0.15f + (1f - (day.tempMinC.toFloat() - minTemp) / tempRange) * (h * 0.55f)
                    bandPath.lineTo(x, yMin)
                }
                bandPath.close()

                // Draw filled band
                drawPath(path = bandPath, color = Color(0xFF81C784).copy(alpha = 0.35f))

                // Draw stroke lines
                drawPath(path = maxPath, color = Color(0xFFE65100), style = Stroke(width = 3.dp.toPx()))
                drawPath(path = minPath, color = Color(0xFF1E88E5), style = Stroke(width = 3.dp.toPx()))

                // 3. Selection indicator
                selectedIndex?.let { idx ->
                    val selX = idx * stepX + stepX / 2
                    drawLine(
                        color = Green900,
                        start = Offset(selX, 0f),
                        end = Offset(selX, h),
                        strokeWidth = 2.dp.toPx(),
                    )
                    drawCircle(
                        color = Green900,
                        radius = 4.dp.toPx(),
                        center = Offset(selX, h * 0.15f),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "🌧 Rain Volume", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1976D2))
                Text(text = "● Max Temp", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                Text(text = "● Min Temp", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E88E5))
            }
        }
    }
}
