package com.footballai.analyzer.presentation.matchdetail

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.domain.model.MatchEvent
import com.footballai.analyzer.domain.model.MatchExtraData
import com.footballai.analyzer.domain.model.Player
import com.footballai.analyzer.ui.theme.LocalAppColors
import com.footballai.analyzer.util.StatHelpers
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    matchId: Long,
    onBack: () -> Unit,
    onStandingsClick: (Long, Int, String, Long, Long) -> Unit = { _, _, _, _, _ -> },
    onMatchClick: (Long) -> Unit = {},
    viewModel: MatchDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val c = LocalAppColors.current
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    // Ha a VM permissiont kér
    if (uiState.needsNotificationPermission) {
        androidx.compose.runtime.LaunchedEffect(uiState.needsNotificationPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onNotificationPermissionResult(true)
            }
        }
    }
    val tabs = listOf("Áttekintés", "Események", "Stat", "Felállás", "Highlight", "Odds", "AI")
    val match = uiState.match

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (match != null) {
                        Column {
                            Text(
                                "${match.homeTeamName} ${match.score} ${match.awayTeamName}",
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (match.isLive) {
                                    Text(
                                        "ÉLŐ ${match.minute ?: "?"}′",
                                        color = c.redLive,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        translateStatus(match.status),
                                        color = c.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                uiState.lastUpdatedLabel?.let {
                                    Text(" · $it", color = c.textSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        Text("Meccs részletei", fontWeight = FontWeight.Bold, color = c.textPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = c.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatchMatch() }) {
                        Icon(
                            if (uiState.isWatched) Icons.Filled.Notifications else Icons.Filled.NotificationsNone,
                            contentDescription = "Értesítés",
                            tint = if (uiState.isWatched) c.green else c.textSecondary
                        )
                    }
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, viewModel.overviewShareText())
                        }
                        context.startActivity(Intent.createChooser(send, "Meccs megosztása"))
                    }) {
                        Icon(Icons.Filled.Share, null, tint = c.textPrimary)
                    }
                    IconButton(onClick = { viewModel.loadAll(silent = false) }) {
                        Icon(Icons.Filled.Refresh, null, tint = c.green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.background)
            )
        },
        containerColor = c.background
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = c.green) }

            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.error ?: "", color = c.textSecondary, textAlign = TextAlign.Center)
            }

            match != null -> Column(Modifier.fillMaxSize().padding(padding)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = uiState.selectedTab == index
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) c.green else c.card)
                                .clickable { viewModel.selectTab(index) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                title,
                                color = if (selected) {
                                    if (c.isDark) c.background else Color.White
                                } else c.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                when (uiState.selectedTab) {
                    0 -> OverviewTab(
                        match = match,
                        h2h = uiState.headToHead,
                        extra = uiState.extra,
                        favoriteTeams = uiState.favoriteTeams,
                        similar = uiState.similarMatches,
                        aiLine = viewModel.aiOneLiner(),
                        isWatched = uiState.isWatched,
                        onToggleFavorite = { viewModel.toggleFavoriteTeam(it) },
                        onStandingsClick = {
                            onStandingsClick(
                                match.leagueId ?: 0L,
                                match.season ?: 0,
                                match.leagueName,
                                match.homeTeamId ?: 0L,
                                match.awayTeamId ?: 0L
                            )
                        },
                        onToggleWatch = { viewModel.toggleWatchMatch() },
                        onSimilarClick = onMatchClick,
                        onAiClick = { viewModel.selectTab(6) }
                    )
                    1 -> EventsTab(uiState.extra, match)
                    2 -> StatsTab(uiState.extra, match)
                    3 -> LineupsTab(uiState.extra, match, viewModel)
                    4 -> HighlightsTab(uiState.extra, match)
                    5 -> OddsTab(uiState.odds, match)
                    6 -> AiTab(
                        uiState = uiState,
                        onGenerate = { viewModel.generateAiAnalysis() },
                        onRegenerate = { viewModel.regenerateAi() }
                    )
                }
            }
        }

        if (uiState.selectedPlayerInfo != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearSelectedPlayer() },
                title = { Text(uiState.selectedPlayerName ?: "Játékos", color = c.textPrimary) },
                text = { Text(uiState.selectedPlayerInfo ?: "", color = c.textSecondary) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearSelectedPlayer() }) {
                        Text("OK", color = c.green)
                    }
                },
                containerColor = c.card
            )
        }
    }
}

@Composable
private fun OverviewTab(
    match: Match,
    h2h: com.footballai.analyzer.domain.model.HeadToHeadData,
    extra: MatchExtraData,
    favoriteTeams: Set<String>,
    similar: List<Match>,
    aiLine: String?,
    isWatched: Boolean,
    onToggleFavorite: (String) -> Unit,
    onStandingsClick: () -> Unit,
    onToggleWatch: () -> Unit,
    onSimilarClick: (Long) -> Unit,
    onAiClick: () -> Unit
) {
    val c = LocalAppColors.current
    val momentum = StatHelpers.momentum(match.homeTeamName, match.awayTeamName, extra)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(match.leagueName, color = c.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))

        // Score block
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                match.homeTeamName,
                color = c.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                match.score.replace("-", "–"),
                color = if (match.isLive) c.redLive else c.green,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                match.awayTeamName,
                color = c.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
        Text(
            if (match.isLive) "ÉLŐ ${match.minute ?: "?"}′" else translateStatus(match.status),
            color = if (match.isLive) c.redLive else c.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        if (!match.isLive && !isFinished(match.status)) {
            Text(
                "Kezdés: ${formatKickoff(match.date)}",
                color = c.blueAccent,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        // AI one-liner
        if (!aiLine.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.green.copy(alpha = 0.12f))
                    .clickable { onAiClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AI", color = c.green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    aiLine,
                    color = c.textPrimary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Momentum
        momentum?.let { m ->
            Spacer(Modifier.height(16.dp))
            Text("Momentum · ${m.label}", color = c.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(c.surface)
            ) {
                Row(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .weight(m.homePercent.coerceIn(0.05f, 0.95f))
                            .background(c.green)
                    )
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .weight((1f - m.homePercent).coerceIn(0.05f, 0.95f))
                            .background(c.blueAccent)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Hazai", color = c.green, fontSize = 11.sp)
                Text("Vendég", color = c.blueAccent, fontSize = 11.sp)
            }
        }

        // Form + H2H mini
        Spacer(Modifier.height(16.dp))
        Text("Forma & H2H", color = c.green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        if (h2h.formHome.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${match.homeTeamName}  ", color = c.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                com.footballai.analyzer.presentation.components.FormDots(match.homeTeamName, h2h.formHome)
            }
            Spacer(Modifier.height(6.dp))
        }
        if (h2h.formAway.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${match.awayTeamName}  ", color = c.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                com.footballai.analyzer.presentation.components.FormDots(match.awayTeamName, h2h.formAway)
            }
            Spacer(Modifier.height(6.dp))
        }
        if (h2h.h2h.isNotEmpty()) {
            val hw = h2h.h2h.count { fm ->
                val parts = fm.score.replace("–", "-").split("-").map { it.trim().toIntOrNull() }
                parts.size == 2 && parts[0] != null && parts[1] != null && (
                    (fm.homeName.equals(match.homeTeamName, true) && parts[0]!! > parts[1]!!) ||
                        (fm.awayName.equals(match.homeTeamName, true) && parts[1]!! > parts[0]!!)
                    )
            }
            val aw = h2h.h2h.count { fm ->
                val parts = fm.score.replace("–", "-").split("-").map { it.trim().toIntOrNull() }
                parts.size == 2 && parts[0] != null && parts[1] != null && (
                    (fm.homeName.equals(match.awayTeamName, true) && parts[0]!! > parts[1]!!) ||
                        (fm.awayName.equals(match.awayTeamName, true) && parts[1]!! > parts[0]!!)
                    )
            }
            val draws = (h2h.h2h.size - hw - aw).coerceAtLeast(0)
            Text(
                "Egymás ellen (utolsó ${h2h.h2h.size}): Hazai győzelem $hw · Döntetlen $draws · Vendég $aw",
                color = c.textPrimary,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if ((match.leagueId ?: 0) > 0 && (match.season ?: 0) > 0) {
                Button(
                    onClick = onStandingsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = c.surface),
                    modifier = Modifier.weight(1f)
                ) { Text("Tabella", color = c.green, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
            Button(
                onClick = onToggleWatch,
                colors = ButtonDefaults.buttonColors(containerColor = c.surface),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (isWatched) "🔔 Be" else "🔔 Ki",
                    color = if (isWatched) c.green else c.textPrimary,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onToggleFavorite(match.homeTeamName) },
                colors = ButtonDefaults.buttonColors(containerColor = c.surface),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (match.homeTeamName in favoriteTeams) "★ Hazai" else "☆ Hazai",
                    color = if (match.homeTeamName in favoriteTeams) c.pinGold else c.textPrimary,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { onToggleFavorite(match.awayTeamName) },
                colors = ButtonDefaults.buttonColors(containerColor = c.surface),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (match.awayTeamName in favoriteTeams) "★ Vendég" else "☆ Vendég",
                    color = if (match.awayTeamName in favoriteTeams) c.pinGold else c.textPrimary,
                    fontSize = 12.sp
                )
            }
        }

        if (similar.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Ugyanez a liga ma", color = c.green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            similar.forEach { sm ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.card)
                        .clickable { onSimilarClick(sm.id) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${sm.homeTeamName} ${sm.score} ${sm.awayTeamName}",
                        color = c.textPrimary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (sm.isLive) {
                        Text("ÉLŐ", color = c.redLive, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EventsTab(extra: MatchExtraData, match: Match) {
    val c = LocalAppColors.current
    if (extra.events.isEmpty()) {
        EmptyMessage(smartEmpty("events", match))
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        extra.events.sortedBy {
            it.minute.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0
        }.forEach { ev ->
            EventTimelineRow(ev, match.homeTeamName)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun EventTimelineRow(ev: MatchEvent, homeName: String) {
    val c = LocalAppColors.current
    val isHome = ev.teamName?.let {
        it.contains(homeName, true) || homeName.contains(it, true)
    } ?: true
    val icon = when {
        ev.type.contains("goal", true) -> "⚽"
        ev.type.contains("yellow", true) -> "🟨"
        ev.type.contains("red", true) -> "🟥"
        ev.type.contains("sub", true) -> "🔄"
        else -> "•"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (isHome) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("${ev.player ?: ev.type} $icon", color = c.textPrimary, fontSize = 13.sp, textAlign = TextAlign.End)
                ev.assist?.let { Text("($it)", color = c.textSecondary, fontSize = 11.sp) }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Box(
            Modifier
                .width(44.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(ev.minute, color = c.green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        if (!isHome) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text("$icon ${ev.player ?: ev.type}", color = c.textPrimary, fontSize = 13.sp)
                ev.assist?.let { Text("($it)", color = c.textSecondary, fontSize = 11.sp) }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatsTab(extra: MatchExtraData, match: Match) {
    val c = LocalAppColors.current
    val pairs = StatHelpers.statBarPairs(match.homeTeamName, match.awayTeamName, extra)
    if (pairs.isEmpty()) {
        EmptyMessage(smartEmpty("stats", match))
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        pairs.forEach { (label, h, a) ->
            StatBarRow(label, h, a)
        }
    }
}

@Composable
private fun StatBarRow(label: String, home: Float, away: Float) {
    val c = LocalAppColors.current
    val total = (home + away).coerceAtLeast(0.1f)
    val hW = home / total
    val aW = away / total
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatStat(home), color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(label, color = c.textSecondary, fontSize = 12.sp)
            Text(formatStat(away), color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.surface)
        ) {
            Box(Modifier.fillMaxHeight().weight(hW.coerceIn(0.02f, 0.98f)).background(c.green))
            Box(Modifier.fillMaxHeight().weight(aW.coerceIn(0.02f, 0.98f)).background(c.blueAccent))
        }
    }
}

private fun formatStat(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else "%.1f".format(v)

@Composable
private fun LineupsTab(
    extra: MatchExtraData,
    match: Match,
    viewModel: MatchDetailViewModel
) {
    val c = LocalAppColors.current
    if (extra.lineups.isEmpty()) {
        EmptyMessage(smartEmpty("lineups", match))
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
                "${team.teamName} (${team.formation ?: "?"})",
                color = c.green,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            if (team.formationRows.isNotEmpty()) {
                team.formationRows.forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { p -> PlayerChip(p) { viewModel.selectPlayer(p.name, p.number, p.position) } }
                    }
                }
            } else {
                team.starters.forEach { p ->
                    PlayerChip(p, fullWidth = true) {
                        viewModel.selectPlayer(p.name, p.number, p.position)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            if (team.substitutes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Cserepad", color = c.textSecondary, fontSize = 12.sp)
                team.substitutes.forEach { p ->
                    PlayerChip(p, fullWidth = true) {
                        viewModel.selectPlayer(p.name, p.number, p.position)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlayerChip(p: Player, fullWidth: Boolean = false, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(8.dp))
            .background(c.card)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!p.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = p.photoUrl,
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(6.dp))
        } else {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(c.green.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(p.number?.toString() ?: "?", color = c.green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
        }
        Column {
            Text(p.name, color = c.textPrimary, fontSize = 12.sp, maxLines = 1)
            p.position?.let { Text(it, color = c.textSecondary, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun HighlightsTab(extra: MatchExtraData, match: Match) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    if (extra.highlights.isEmpty()) {
        EmptyMessage(smartEmpty("highlights", match))
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        extra.highlights.forEach { h ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.card)
                    .clickable {
                        val url = h.url
                        if (!url.isNullOrBlank()) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
                Column(Modifier.weight(1f)) {
                    Text(h.title, color = c.textPrimary, fontSize = 14.sp)
                    if (!h.url.isNullOrBlank()) {
                        Text("Megnyitás ▶", color = c.green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OddsTab(odds: List<com.footballai.analyzer.domain.model.OddsRow>, match: Match) {
    val c = LocalAppColors.current
    if (odds.isEmpty()) {
        EmptyMessage("Nincs elérhető odds ehhez a meccshez.\n(Az API nem adott 1X2 adatot.)")
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text("Iroda", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1.4f))
            Text("1", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
            Text("X", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
            Text("2", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
        }
        odds.forEach { o ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.card)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(o.bookmaker, color = c.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1.4f), maxLines = 1)
                Text(o.home ?: "–", color = c.green, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text(o.draw ?: "–", color = c.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text(o.away ?: "–", color = c.blueAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AiTab(
    uiState: MatchDetailUiState,
    onGenerate: () -> Unit,
    onRegenerate: () -> Unit
) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        when {
            uiState.isAiLoading && uiState.aiAnalysis == null -> {
                Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.green)
                }
            }
            uiState.aiAnalysis != null -> {
                if (uiState.isInstantAi) {
                    Text("Azonnali elemzés", color = c.green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                }
                if (uiState.isAiLoading) {
                    Text("Részletes AI töltése…", color = c.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                }
                val sections = com.footballai.analyzer.util.AiSectionParser.parse(uiState.aiAnalysis ?: "")
                if (sections.size <= 1) {
                    Text(uiState.aiAnalysis ?: "", color = c.textPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                } else {
                    sections.forEach { sec ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.card)
                                .padding(12.dp)
                        ) {
                            Text(sec.title, color = c.green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(sec.body, color = c.textPrimary, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRegenerate,
                        colors = ButtonDefaults.buttonColors(containerColor = c.card, contentColor = c.textPrimary),
                        modifier = Modifier.weight(1f)
                    ) { Text("Újragenerálás") }
                    Button(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, uiState.aiAnalysis ?: "")
                            }
                            context.startActivity(Intent.createChooser(send, "AI megosztása"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = c.green),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Megosztás", fontWeight = FontWeight.Bold, color = if (c.isDark) c.background else Color.White)
                    }
                }
            }
            else -> {
                Text("Nyomd meg a gombot az AI elemzéshez.", color = c.textSecondary)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onGenerate,
                    colors = ButtonDefaults.buttonColors(containerColor = c.green)
                ) {
                    Text("AI Elemzés", fontWeight = FontWeight.Bold, color = if (c.isDark) c.background else Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(24.dp)
        )
    }
}

private fun smartEmpty(kind: String, match: Match): String {
    val notStarted = !match.isLive && !isFinished(match.status)
    val kick = formatKickoff(match.date)
    return when (kind) {
        "events" -> if (notStarted)
            "A meccs még nem kezdődött el.\nKezdés: $kick\nGólok, lapok, cserék élőben jelennek meg."
        else "Nincs eseményadat ehhez a meccshez."
        "stats" -> if (notStarted)
            "Statisztika a kezdés után érkezik.\nKezdés: $kick"
        else "Az API nem adott statisztikát."
        "lineups" -> if (notStarted)
            "Felállás általában a kezdés előtt ~40 perccel jelenik meg.\nKezdés: $kick"
        else "Nincs felállás adat."
        "highlights" -> if (notStarted)
            "Highlightok a meccs után kerülnek fel."
        else "Nincs elérhető highlight videó."
        else -> "Nincs adat."
    }
}

private fun isFinished(status: String): Boolean {
    val s = status.lowercase()
    return s.contains("finished") || s == "ft" || s.contains("after")
}

private fun translateStatus(status: String): String {
    val s = status.lowercase()
    return when {
        s.contains("not started") || s.contains("to be announced") -> "Nem kezdődött el"
        s.contains("first half") -> "1. félidő"
        s.contains("second half") -> "2. félidő"
        s.contains("half time") || s == "ht" -> "Félidő"
        s.contains("finished") || s == "ft" -> "Vége"
        s.contains("extra time") -> "Hosszabbítás"
        s.contains("penalties") -> "Büntetők"
        s.contains("postponed") -> "Elhalasztva"
        s.contains("cancelled") || s.contains("canceled") -> "Törölve"
        s.contains("in progress") -> "Folyamatban"
        else -> status
    }
}

private fun formatKickoff(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "–"
    return try {
        OffsetDateTime.parse(dateString)
            .atZoneSameInstant(ZoneId.of("Europe/Budapest"))
            .format(DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm"))
    } catch (_: Exception) {
        dateString
    }
}
