package com.footballai.analyzer.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.footballai.analyzer.presentation.components.MatchRow
import com.footballai.analyzer.ui.theme.DarkBackground
import com.footballai.analyzer.ui.theme.DarkCard
import com.footballai.analyzer.ui.theme.GreenPrimary
import com.footballai.analyzer.ui.theme.RedLive
import com.footballai.analyzer.ui.theme.TextPrimary
import com.footballai.analyzer.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMatchClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = LocalDate.now()
    val dates = (-3..7).map { today.plusDays(it.toLong()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FociElemző",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadMatches() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Frissítés", tint = GreenPrimary)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Beállítások", tint = GreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // NAPTÁR SÁV
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dates.forEach { date ->
                    val isSelected = date == uiState.selectedDate
                    val label = when (date) {
                        today.minusDays(1) -> "Tegnap"
                        today -> "Ma"
                        today.plusDays(1) -> "Holnap"
                        else -> {
                            val day = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("hu", "HU"))
                            "\( {date.dayOfMonth}/ \){date.monthValue} $day"
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) GreenPrimary else DarkCard)
                            .clickable { viewModel.selectDate(date) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBackground else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // SZŰRŐ TABOK
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip("Összes", uiState.filter == MatchFilter.ALL) {
                    viewModel.setFilter(MatchFilter.ALL)
                }
                FilterChip("ÉLŐ", uiState.filter == MatchFilter.LIVE, isLive = true) {
                    viewModel.setFilter(MatchFilter.LIVE)
                }
                FilterChip("Vége", uiState.filter == MatchFilter.FINISHED) {
                    viewModel.setFilter(MatchFilter.FINISHED)
                }
                FilterChip("Hátralévő", uiState.filter == MatchFilter.UPCOMING) {
                    viewModel.setFilter(MatchFilter.UPCOMING)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary)
                    }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.error ?: "", color = TextSecondary)
                    }
                }
                uiState.leagueGroups.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Nincs meccs ezen a napon", color = TextSecondary)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        uiState.leagueGroups.forEach { group ->
                            item {
                                Text(
                                    text = group.leagueName.uppercase(),
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                            items(group.matches, key = { it.id }) { match ->
                                MatchRow(
                                    match = match,
                                    onClick = { onMatchClick(match.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    isLive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    selected && isLive -> RedLive
                    selected -> GreenPrimary
                    else -> DarkCard
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) DarkBackground else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
