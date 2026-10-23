package com.footballai.analyzer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.ui.theme.LocalAppColors
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MatchRow(
    match: Match,
    onClick: () -> Unit,
    favoriteTeams: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    aiPreview: String? = null
) {
    val c = LocalAppColors.current
    val isLive = match.isLive
    val finished = isFinished(match.status)
    val homeFav = match.homeTeamName in favoriteTeams
    val awayFav = match.awayTeamName in favoriteTeams

    val rowBg = when {
        isLive -> c.liveRow
        finished -> if (c.isDark) c.surface.copy(alpha = 0.45f) else c.surface
        else -> c.background
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(rowBg)
            .then(
                if (isLive) {
                    Modifier.border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(c.redLive.copy(alpha = 0.55f), c.green.copy(alpha = 0.25f))
                        ),
                        RoundedCornerShape(0.dp)
                    )
                } else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                when {
                    isLive -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(c.redLive)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text("ÉLŐ", color = androidx.compose.ui.graphics.Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text((match.minute?.toString() ?: "?") + "'", color = c.redLive, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    finished -> Text("Vége", color = c.green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    else -> Text(formatBudapestTime(match.date), color = c.blueAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Home + star
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onToggleFavorite(match.homeTeamName) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        null,
                        tint = if (homeFav) c.pinGold else c.textSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                AsyncImage(
                    model = match.homeTeamLogo,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(c.surface),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    match.homeTeamName,
                    color = c.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = if (homeFav) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            val scoreColor = when {
                isLive -> c.redLive
                finished -> c.green
                else -> c.textSecondary
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isLive -> c.redLive.copy(alpha = 0.12f)
                            finished -> c.green.copy(alpha = 0.12f)
                            else -> c.surface
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(match.score.replace("-", "–"), color = scoreColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Away + star
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    match.awayTeamName,
                    color = c.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = if (awayFav) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.width(4.dp))
                AsyncImage(
                    model = match.awayTeamLogo,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(c.surface),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { onToggleFavorite(match.awayTeamName) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        null,
                        tint = if (awayFav) c.pinGold else c.textSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (!aiPreview.isNullOrBlank()) {
            Text(
                text = aiPreview.take(120) + if (aiPreview.length > 120) "…" else "",
                color = c.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 48.dp, top = 2.dp, end = 8.dp)
            )
        }
    }
}

private fun isFinished(status: String): Boolean {
    val s = status.lowercase()
    return s.contains("finished") || s == "ft" || s.contains("after")
}

private fun formatBudapestTime(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "--:--"
    return try {
        OffsetDateTime.parse(dateString)
            .atZoneSameInstant(ZoneId.of("Europe/Budapest"))
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        "--:--"
    }
}
