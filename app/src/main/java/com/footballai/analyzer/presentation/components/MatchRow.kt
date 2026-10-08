package com.footballai.analyzer.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import com.footballai.analyzer.ui.theme.GreenPrimary
import com.footballai.analyzer.ui.theme.RedLive
import com.footballai.analyzer.ui.theme.TextPrimary
import com.footballai.analyzer.ui.theme.TextSecondary
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MatchRow(
    match: Match,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Idő / státusz
            Text(
                text = statusText(match),
                color = if (match.isLive) RedLive else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (match.isLive) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Hazai
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = match.homeTeamName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                AsyncImage(
                    model = match.homeTeamLogo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // Eredmény
            Text(
                text = match.score.replace("-", " - "),
                color = if (match.isLive) GreenPrimary else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .width(56.dp)
                    .padding(horizontal = 4.dp),
                textAlign = TextAlign.Center
            )

            // Vendég
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                AsyncImage(
                    model = match.awayTeamLogo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = match.awayTeamName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f), thickness = 0.5.dp)
}

private fun statusText(match: Match): String {
    return when {
        match.isLive -> "${match.minute ?: ""}'"
        match.status.contains("Finished", ignoreCase = true) -> "Vége"
        match.status == "Not started" || match.status == "To be announced" -> {
            formatBudapestTime(match.date)
        }
        else -> match.status.take(6)
    }
}

/** UTC / offset → budapesti helyi idő (CEST/CET) */
private fun formatBudapestTime(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "-"
    return try {
        val odt = OffsetDateTime.parse(dateString)
        val budapest = odt.atZoneSameInstant(ZoneId.of("Europe/Budapest"))
        budapest.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        try {
            // Ha nincs offset a stringben
            val odt = OffsetDateTime.parse(dateString + "Z")
            val budapest = odt.atZoneSameInstant(ZoneId.of("Europe/Budapest"))
            budapest.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e2: Exception) {
            "-"
        }
    }
}
