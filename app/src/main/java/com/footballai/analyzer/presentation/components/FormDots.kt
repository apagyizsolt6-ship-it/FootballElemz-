package com.footballai.analyzer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.footballai.analyzer.domain.model.FormMatch
import com.footballai.analyzer.ui.theme.LocalAppColors

/**
 * W/D/L pöttyök az utolsó meccsekből a megadott csapat nézőpontjából.
 */
@Composable
fun FormDots(teamName: String, matches: List<FormMatch>, max: Int = 5) {
    val c = LocalAppColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        matches.take(max).forEach { fm ->
            val result = formResult(teamName, fm)
            val color = when (result) {
                'W' -> c.green
                'D' -> Color(0xFFFFC107)
                'L' -> c.redLive
                else -> c.textSecondary.copy(alpha = 0.3f)
            }
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        }
    }
}

private fun formResult(teamName: String, fm: FormMatch): Char {
    val score = fm.score.replace("–", "-").replace(" ", "")
    val parts = score.split("-")
    if (parts.size != 2) return '?'
    val homeGoals = parts[0].toIntOrNull() ?: return '?'
    val awayGoals = parts[1].toIntOrNull() ?: return '?'
    val isHome = fm.homeName.equals(teamName, ignoreCase = true)
    val isAway = fm.awayName.equals(teamName, ignoreCase = true)
    if (!isHome && !isAway) return '?'
    val gf = if (isHome) homeGoals else awayGoals
    val ga = if (isHome) awayGoals else homeGoals
    return when {
        gf > ga -> 'W'
        gf == ga -> 'D'
        else -> 'L'
    }
}
