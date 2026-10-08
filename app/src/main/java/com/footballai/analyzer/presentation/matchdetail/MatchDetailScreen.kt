package com.footballai.analyzer.presentation.matchdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.domain.model.MatchExtraData
import com.footballai.analyzer.ui.theme.DarkBackground
import com.footballai.analyzer.ui.theme.DarkCard
import com.footballai.analyzer.ui.theme.GreenPrimary
import com.footballai.analyzer.ui.theme.RedLive
import com.footballai.analyzer.ui.theme.TextPrimary
import com.footballai.analyzer.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    matchId: Long,
    onBack: () -> Unit,
    viewModel: MatchDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Áttekintés", "Statisztika", "Felállás", "Highlightok", "AI")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Meccs részletei", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Vissza",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
            }
            uiState.error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.error ?: "", color = TextSecondary)
                }
            }
            uiState.match != null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Tab sáv
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val selected = uiState.selectedTab == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) GreenPrimary else DarkCard)
                                    .clickable { viewModel.selectTab(index) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (selected) DarkBackground else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Tartalom
                    when (uiState.selectedTab) {
                        0 -> OverviewTab(uiState.match!!, onAiClick = { viewModel.generateAiAnalysis() })
                        1 -> StatsTab(uiState.extra)
                        2 -> LineupsTab(uiState.extra)
                        3 -> HighlightsTab(uiState.extra)
                        4 -> AiTab(uiState, onGenerate = { viewModel.generateAiAnalysis() })
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(match: Match, onAiClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(match.leagueName, color = TextSecondary, fontSize = 13.sp)
            Text(
                if (match.isLive) "ÉLŐ ${match.minute ?: ""}'" else match.status,
                color = if (match.isLive) RedLive else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (match.isLive) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = match.homeTeamLogo,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    match.homeTeamName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }

            Text(
                match.score.replace("-", " - "),
                color = GreenPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = match.awayTeamLogo,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    match.awayTeamName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onAiClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary,
                contentColor = DarkBackground
            )
        ) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("AI Elemzés indítása", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatsTab(extra: MatchExtraData) {
    if (extra.statistics.isEmpty()) {
        EmptyMessage("Nincs elérhető statisztika")
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val home = extra.statistics.getOrNull(0)
        val away = extra.statistics.getOrNull(1)
        val allKeys = (home?.stats?.keys.orEmpty() + away?.stats?.keys.orEmpty()).distinct()

        allKeys.forEach { key ->
            val h = home?.stats?.get(key) ?: "-"
            val a = away?.stats?.get(key) ?: "-"
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(h, color = TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text(
                    key,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1.5f),
                    textAlign = TextAlign.Center
                )
                Text(a, color = TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            }
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f))
        }
    }
}

@Composable
private fun LineupsTab(extra: MatchExtraData) {
    if (extra.lineups.isEmpty()) {
        EmptyMessage("Nincs elérhető felállás")
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        extra.lineups.forEach { team ->
            Text(
                "${team.teamName}${if (team.formation != null) " (${team.formation})" else ""}",
                color = GreenPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(8.dp))
            Text("Kezdőcsapat", color = TextSecondary, fontSize = 12.sp)
            team.starters.forEach { p ->
                Text(
                    "${p.number ?: "-"}  ${p.name}${if (p.position != null) " (${p.position})" else ""}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            if (team.substitutes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Cserejátékosok", color = TextSecondary, fontSize = 12.sp)
                team.substitutes.forEach { p ->
                    Text(
                        "${p.number ?: "-"}  ${p.name}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HighlightsTab(extra: MatchExtraData) {
    val uriHandler = LocalUriHandler.current

    if (extra.highlights.isEmpty()) {
        EmptyMessage("Nincs elérhető highlight")
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        extra.highlights.forEach { h ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .clickable {
                        if (!h.url.isNullOrBlank()) {
                            uriHandler.openUri(h.url)
                        }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!h.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = h.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(h.title, color = TextPrimary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun AiTab(uiState: MatchDetailUiState, onGenerate: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (uiState.isAiLoading) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else if (uiState.aiAnalysis != null) {
            Text(
                uiState.aiAnalysis,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        } else {
            Text(
                "Nyomd meg a gombot az AI elemzés generálásához.",
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onGenerate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("AI Elemzés", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = TextSecondary)
    }
}
