package com.footballai.analyzer.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.
            // Kereső
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("Csapat vagy liga keresése…", color = c.textSecondary, fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.green,
                    unfocusedBorderColor = c.card,
                    focusedTextColor = c.textPrimary,
                    unfocusedTextColor = c.textPrimary,
                    cursorColor = c.green,
                    focusedContainerColor = c.card,
                    unfocusedContainerColor = c.card
                )
            )

            Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.footballai.analyzer.presentation.components.MatchRow
import com.footballai.analyzer.ui.theme.LocalAppColors
import com.footballai.analyzer.util.LeagueTranslator
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMatchClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onStandingsClick: (leagueId: Long, season: Int, title: String) -> Unit = { _, _, _ -> },
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val c = LocalAppColors.current
    val today = LocalDate.now()
    val dates = (-3..7).map { today.plusDays(it.toLong()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FociElemző",
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadMatches(isRefresh = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Frissítés",
                            tint = c.green
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Beállítások",
                            tint = c.green
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = c.background,
                    titleContentColor = c.green,
                    actionIconContentColor = c.green
                )
            )
        },
        containerColor = c.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dates.forEach { date ->
                    val isSelected = date == uiState.selectedDate
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) c.green else c.card)
                            .clickable { viewModel.selectDate(date) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = buildDateLabel(date, today),
                            color = if (isSelected) {
                                if (c.isDark) c.background else Color.White
                            } else c.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
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
                FilterChip("Top ligák", uiState.onlyTopLeagues) {
                    viewModel.toggleTopLeagues()
                }
                FilterChip("Kedvencek", uiState.onlyFavorites) {
                    viewModel.toggleFavoritesFilter()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val countText = when {
                    uiState.onlyTopLeagues ->
                        "${uiState.filteredMatchCount} meccs (Top ligák / kedvenc)"
                    uiState.filter != MatchFilter.ALL ->
                        "${uiState.filteredMatchCount} / ${uiState.totalMatchCount} meccs"
                    else ->
                        "${uiState.totalMatchCount} meccs"
                }
                Text(text = countText, color = c.textSecondary, fontSize = 13.sp)
                if (uiState.isLoading || uiState.isRefreshing) {
                    Text(text = "Betöltés...", color = c.green, fontSize = 13.sp)
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.loadMatches(isRefresh = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.leagueGroups.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = c.green)
                        }
                    }
                    uiState.error != null && uiState.leagueGroups.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = uiState.error ?: "", color = c.textSecondary)
                        }
                    }
                    uiState.leagueGroups.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Nincs meccs ezen a napon", color = c.textSecondary)
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            uiState.leagueGroups.forEach { group ->
                                item {
                                    LeagueHeader(
                                        title = group.leagueName,
                                        isPinned = group.isPinned,
                                        isTop = LeagueTranslator.isTopLeague(group.leagueName),
                                        onTogglePin = { viewModel.togglePin(group.leagueName) },
                                        onOpenStandings = {
                                            val m = group.matches.firstOrNull()
                                            val lid = m?.leagueId ?: 0L
                                            val season = m?.season ?: 0
                                            onStandingsClick(lid, season, group.leagueName)
                                        }
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
}

@Composable
private fun LeagueHeader(
    title: String,
    isPinned: Boolean,
    isTop: Boolean,
    onTogglePin: () -> Unit,
    onOpenStandings: () -> Unit = {}
) {
    val c = LocalAppColors.current
    val accent = when {
        isPinned -> c.pinGold
        isTop -> c.green
        else -> c.blueAccent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.card),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(40.dp)
                .background(accent)
        )
        IconButton(
            onClick = onTogglePin,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = if (isPinned) "Kedvenc levétel" else "Kedvenc",
                tint = if (isPinned) c.pinGold else c.textSecondary.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title,
            color = when {
                isPinned -> c.pinGold
                isTop -> c.green
                else -> c.textPrimary
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
                .clickable { onOpenStandings() }
        )
    }
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    isLive: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    selected && isLive -> c.redLive
                    selected -> c.green
                    else -> c.card
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) {
                Color.White
            } else if (isLive) c.redLive else c.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun buildDateLabel(date: LocalDate, today: LocalDate): String {
    return when (date) {
        today.minusDays(1) -> "Tegnap"
        today -> "Ma"
        today.plusDays(1) -> "Holnap"
        else -> {
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("hu", "HU"))
            date.dayOfMonth.toString() + "/" + date.monthValue.toString() + " " + dayName
        }
    }
}
