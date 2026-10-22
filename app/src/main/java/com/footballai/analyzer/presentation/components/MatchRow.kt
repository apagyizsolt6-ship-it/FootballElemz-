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
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    val isLive = match.isLive
    val finished = isFinished(match.status)

    val rowBg = when {
        isLive -> c.liveRow
        finished -> if (c.isDark) c.surface.copy(alpha = 0.45f) else c.surface
        else -> c.background
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .then(
                if (isLive) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(c.redLive.copy(alpha = 0.55f), c.green.copy(alpha = 0.25f))
                        ),
                        shape = RoundedCornerShape(0.dp)
                    )
                } else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time / live badge
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLive -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(c.redLive)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ÉLŐ",
                                color = androidx.compose.ui.graphics.Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = (match.minute?.toString() ?: "?") + "'",
                            color = c.redLive,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                finished -> {
                    Text(
                        text = "Vége",
                        color = c.green,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                else -> {
                    Text(
                        text = formatBudapestTime(match.date),
                        color = c.blueAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.width(6.dp))

        // Home
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = match.homeTeamLogo,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(c.surface),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = match.homeTeamName,
                color = c.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
        }

        // Score
        val scoreColor = when {
            isLive -> c.redLive
            finished -> c.green
            else -> c.textSecondary
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isLive) c.redLive.copy(alpha = 0.12f)
                    else if (finished) c.green.copy(alpha = 0.12f)
                    else c.surface
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = match.score.replace("-", "–"),
                color = scoreColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        // Away
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = match.awayTeamName,
                color = c.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Spacer(Modifier.width(6.dp))
            AsyncImage(
                model = match.awayTeamLogo,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(c.surface),
                contentScale = ContentScale.Fit
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
        val odt = OffsetDateTime.parse(dateString)
        odt.atZoneSameInstant(ZoneId.of("Europe/Budapest"))
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        "--:--"
    }
}
