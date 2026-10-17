package com.footballai.analyzer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    val isLive = match.isLive
    val rowBg = if (isLive) c.liveRow else c.background

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(42.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLive -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(c.redLive)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = (match.minute?.toString() ?: "?") + "'",
                            color = c.redLive,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                isFinished(match.status) -> {
                    Text(text = "Vége", color = c.textSecondary, fontSize = 11.sp)
                }
                else -> {
                    Text(
                        text = formatBudapestTime(match.date),
                        color = c.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        TeamCell(
            name = match.homeTeamName,
            logo = match.homeTeamLogo,
            modifier = Modifier.weight(1f),
            alignEnd = true,
            bold = isWinner(match, home = true)
        )

        Box(
            modifier = Modifier.width(52.dp),
            contentAlignment = Alignment.Center
        ) {
            val scoreColor = when {
                isLive -> c.green
                isFinished(match.status) -> c.textPrimary
                else -> c.textSecondary
            }
            Text(
                text = formatScore(match.score),
                color = scoreColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        TeamCell(
            name = match.awayTeamName,
            logo = match.awayTeamLogo,
            modifier = Modifier.weight(1f),
            alignEnd = false,
            bold = isWinner(match, home = false)
        )
    }
}

@Composable
private fun TeamCell(
    name: String,
    logo: String?,
    modifier: Modifier,
    alignEnd: Boolean,
    bold: Boolean
) {
    val c = LocalAppColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!alignEnd) {
            TeamLogo(logo)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = name,
            color = if (bold) c.green else c.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
        )
        if (alignEnd) {
            Spacer(modifier = Modifier.width(4.dp))
            TeamLogo(logo)
        }
    }
}

@Composable
private fun TeamLogo(url: String?) {
    val c = LocalAppColors.current
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(c.card)
        )
    }
}

private fun formatBudapestTime(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "--:--"
    return try {
        val odt = OffsetDateTime.parse(dateString)
        val budapest = odt.atZoneSameInstant(ZoneId.of("Europe/Budapest"))
        budapest.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        try {
            dateString.substring(11, 16)
        } catch (_: Exception) {
            "--:--"
        }
    }
}

private fun formatScore(score: String): String {
    val s = score.replace(" ", "")
    return if (s.contains("-") || s.contains("–")) s.replace("–", " - ").replace("-", " - ")
    else score
}

private fun isFinished(status: String): Boolean {
    return status.contains("Finished", ignoreCase = true) ||
        status.contains("Vége", ignoreCase = true) ||
        status.equals("FT", ignoreCase = true)
}

private fun isWinner(match: Match, home: Boolean): Boolean {
    if (!isFinished(match.status) && !match.isLive) return false
    val parts = match.score.replace(" ", "").split("-", "–")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return false
    val a = parts.getOrNull(1)?.toIntOrNull() ?: return false
    return if (home) h > a else a > h
}
