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
import androidx.compose.ui.graphics.Color
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
import com.footballai.analyzer.ui.theme.LocalAppColors
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    matchId: Long,
    onBack: () -> Unit,
    viewModel: MatchDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val c = LocalAppColors.current
    val tabs = listOf("Áttekintés", "Statisztika", "Felállás", "Highlightok", "AI")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Meccs részletei", fontWeight = FontWeight.Bold, color = c.textPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Vissza",
                            tint = c.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.background)
            )
        },
        containerColor = c.background
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = c.green)
                }
            }
            uiState.error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.error ?: "", color = c.textSecondary)
                }
            }
            uiState.match != null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
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
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) c.green else c.card)
                                    .clickable { viewModel.selectTab(index) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (selected) {
                                        if (c.isDark) c.background else Color.White
                                    } else c.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    when (uiState.selectedTab) {
                        0 -> OverviewTab(
                            match = uiState.match!!,
                            onAiClick = { viewModel.generateAiAnalysis(fromAuto = false) }
                        )
                        1 -> StatsTab(uiState.extra)
                        2 -> LineupsTab(uiState.extra)
                        3 -> HighlightsTab(uiState.extra)
                        4 -> AiTab(
                            uiState = uiState,
                            onGenerate = { viewModel.generateAiAnalysis() },
                            onRegenerate = { viewModel.regenerateAi() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(match: Match, onAiClick: () -> Unit) {
    val c = LocalAppColors.current
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
            Text(match.leagueName, color = c.textSecondary, fontSize = 13.sp)
            Text(
                text = if (match.isLive) {
                    "ÉLŐ " + (match.minute?.toString() ?: "") + "'"
                } else {
                    translateStatus(match.status)
                },
                color = if (match.isLive) c.redLive else c.textSecondary,
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = match.homeTeamLogo,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    match.homeTeamName,
                    color = c.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }

            Text(
                match.score.replace("-", " - "),
                color = c.green,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = match.awayTeamLogo,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    match.awayTeamName,
                    color = c.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Kezdés: " + formatKickoff(match.date),
            color = c.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onAiClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = c.green,
                contentColor = if (c.isDark) c.background else Color.White
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
    val c = LocalAppColors.current
    if (extra.statistics.isEmpty()) {
        EmptyMessage("Nincs elérhető statisztika.\nA meccs kezdete után jelennek meg az adatok.")
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
        val keys = (home?.stats?.keys.orEmpty() + away?.stats?.keys.orEmpty()).toSet().toList()

        if (home != null && away != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(home.teamName, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(away.teamName, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            keys.forEach { key ->
                val hv = home.stats[key] ?: "-"
                val av = away.stats[key] ?: "-"
                StatBarRow(label = key, homeValue = hv, awayValue = av)
            }
        } else {
            extra.statistics.forEach { team ->
                Text(team.teamName, color = c.green, fontWeight = FontWeight.Bold)
                team.stats.forEach { (k, v) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(k, color = c.textSecondary)
                        Text(v, color = c.textPrimary)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatBarRow(label: String, homeValue: String, awayValue: String) {
    val c = LocalAppColors.current
    val hNum = homeValue.replace("%", "").replace(",", ".").toFloatOrNull()
    val aNum = awayValue.replace("%", "").replace(",", ".").toFloatOrNull()
    val total = if (hNum != null && aNum != null && (hNum + aNum) > 0f) hNum + aNum else null
    val hRatio = if (total != null) (hNum!! / total).coerceIn(0f, 1f) else 0.5f

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            label,
            color = c.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(homeValue, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(awayValue, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        if (total != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.surface)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(hRatio)
                        .height(6.dp)
                        .background(c.green)
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(c.redLive.copy(alpha = 0.7f))
                )
            }
        } else {
            HorizontalDivider(color = c.card, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun LineupsTab(extra: MatchExtraData) {
    val c = LocalAppColors.current
    if (extra.lineups.isEmpty()) {
        EmptyMessage("Nincs elérhető felállás.\nÁltalában a kezdés előtt ~40 perccel jelenik meg.")
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        extra.lineups.forEach { team ->
            Text(
                team.teamName + if (team.formation != null) " (" + team.formation + ")" else "",
                color = c.green,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text("Kezdő", color = c.textSecondary, fontSize = 12.sp)
            team.starters.forEach { p ->
                val num = p.number?.toString() ?: "-"
                val pos = p.position ?: ""
                Text("$num  ${p.name}  $pos", color = c.textPrimary, fontSize = 13.sp)
            }
            if (team.substitutes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Csere", color = c.textSecondary, fontSize = 12.sp)
                team.substitutes.forEach { p ->
                    val num = p.number?.toString() ?: "-"
                    Text("$num  ${p.name}", color = c.textPrimary, fontSize = 13.sp)
                }
            }
            HorizontalDivider(color = c.card)
        }
    }
}

@Composable
private fun HighlightsTab(extra: MatchExtraData) {
    val c = LocalAppColors.current
    if (extra.highlights.isEmpty()) {
        EmptyMessage("Nincs elérhető highlight.\nVideók a meccs után kerülnek fel.")
        return
    }

    val uriHandler = LocalUriHandler.current
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
                    .background(c.card)
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
                Text(h.title, color = c.textPrimary, fontSize = 14.sp)
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
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        when {
            uiState.isAiLoading -> {
                Box(
                    Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = c.green)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (uiState.autoAiEligible) {
                                "Automatikus AI elemzés..."
                            } else {
                                "AI elemzés..."
                            },
                            color = c.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            uiState.aiError != null && uiState.aiAnalysis == null -> {
                Text(uiState.aiError ?: "", color = c.redLive, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onGenerate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.green,
                        contentColor = if (c.isDark) c.background else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Újrapróbálás", fontWeight = FontWeight.Bold)
                }
            }
            uiState.aiAnalysis != null -> {
                if (uiState.autoAiEligible) {
                    Text(
                        "Top liga / kedvenc – automatikus elemzés",
                        color = c.textSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    uiState.aiAnalysis ?: "",
                    color = c.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRegenerate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.card,
                        contentColor = c.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Újragenerálás")
                }
            }
            else -> {
                Text(
                    text = if (uiState.autoAiEligible) {
                        "Az AI elemzés automatikusan elindul (Top liga / kedvenc)."
                    } else {
                        "Nyomd meg a gombot az AI elemzés generálásához."
                    },
                    color = c.textSecondary
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onGenerate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.green,
                        contentColor = if (c.isDark) c.background else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("AI Elemzés", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(24.dp)
        )
    }
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
    if (dateString.isNullOrBlank()) return "-"
    return try {
        val odt = OffsetDateTime.parse(dateString)
        val bp = odt.atZoneSameInstant(ZoneId.of("Europe/Budapest"))
        bp.format(DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm"))
    } catch (_: Exception) {
        dateString
    }
}
