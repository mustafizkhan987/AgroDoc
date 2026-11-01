package com.agrodoc.app.ui.screens.crops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agrodoc.app.R
import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.ui.theme.Green900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropsScreen(
    onCropClick: (String) -> Unit,
    viewModel: CropsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.crops_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Green900,
        )

        Spacer(Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.crops_search)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        // Season Filter Chips
        val seasons = listOf(
            null to stringResource(R.string.filter_all_seasons),
            "kharif" to "Kharif",
            "rabi" to "Rabi",
            "summer" to "Summer",
            "perennial" to "Perennial",
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            seasons.forEach { (key, label) ->
                val selected = uiState.selectedSeason == key
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setSeasonFilter(if (selected) null else key) },
                    label = { Text(label) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Crops Grid (2 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(uiState.filteredCrops) { crop ->
                val isMyCrop = uiState.myCropIds.contains(crop.id)
                CropCard(
                    crop = crop,
                    isMyCrop = isMyCrop,
                    currentLang = uiState.currentLang,
                    onToggleMyCrop = { viewModel.toggleMyCrop(crop.id) },
                    onClick = { onCropClick(crop.id) },
                )
            }
        }
    }
}

@Composable
private fun CropCard(
    crop: CropInfo,
    isMyCrop: Boolean,
    currentLang: String,
    onToggleMyCrop: () -> Unit,
    onClick: () -> Unit,
) {
    val cropName = when (currentLang) {
        "kn" -> crop.nameKn
        "hi" -> crop.nameHi
        "mr" -> crop.nameMr
        else -> crop.nameEn
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMyCrop) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Eco,
                    contentDescription = null,
                    tint = Green900,
                    modifier = Modifier.size(24.dp),
                )
                IconButton(
                    onClick = onToggleMyCrop,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        if (isMyCrop) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.my_crops),
                        tint = if (isMyCrop) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = cropName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = crop.category.capitalize(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = crop.seasons.joinToString(", ") { it.capitalize() },
                style = MaterialTheme.typography.labelSmall,
                color = Green900,
            )
        }
    }
}

