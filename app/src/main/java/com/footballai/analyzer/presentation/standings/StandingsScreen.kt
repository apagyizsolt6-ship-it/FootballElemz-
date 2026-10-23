package com.footballai.analyzer.presentation.standings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.footballai.analyzer.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(
    onBack: () -> Unit,
    viewModel: StandingsViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val c = LocalAppColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(ui.title, fontWeight = FontWeight.Bold, color = c.textPrimary, maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = c.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.background)
            )
        },
        containerColor = c.background
    ) { padding ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.green)
            }
            ui.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(ui.error ?: "", color = c.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
            }
            ui.groups.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nincs tabella adat.", color = c.textSecondary)
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ui.groups.forEach { group ->
                    item {
                        Text(group.name, color = c.green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("#", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                            Text("Csapat", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("M", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("Gy", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("D", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("V", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("G贸l", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                            Text("P", color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                        }
                    }
                    items(group.rows) { row ->
                        val highlighted = row.teamId != null && row.teamId in ui.highlightTeamIds
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (highlighted) c.green.copy(alpha = 0.15f) else c.card)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${row.position}", color = c.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), fontSize = 13.sp)
                            AsyncImage(
                                model = row.teamLogo,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp).clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(row.teamName, color = c.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${row.played}", color = c.textSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontSize = 12.sp)
                            Text("${row.wins}", color = c.textSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontSize = 12.sp)
                            Text("${row.draws}", color = c.textSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontSize = 12.sp)
                            Text("${row.losses}", color = c.textSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontSize = 12.sp)
                            Text("${row.goalsFor}:${row.goalsAgainst}", color = c.textSecondary, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, fontSize = 11.sp)
                            Text("${row.points}", color = c.green, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End, fontSize = 13.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
