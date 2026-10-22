package com.footballai.analyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val card: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val green: Color,
    val redLive: Color,
    val orangeLive: Color,
    val blueAccent: Color,
    val purpleAccent: Color,
    val cyanAccent: Color,
    val liveRow: Color,
    val pinGold: Color,
    val isDark: Boolean
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        background = DarkBackground,
        card = DarkCard,
        surface = DarkSurface,
        textPrimary = TextPrimaryDark,
        textSecondary = TextSecondaryDark,
        green = GreenPrimary,
        redLive = RedLive,
        orangeLive = OrangeLive,
        blueAccent = BlueAccent,
        purpleAccent = PurpleAccent,
        cyanAccent = CyanAccent,
        liveRow = LiveRowDark,
        pinGold = PinGold,
        isDark = true
    )
}

private val DarkScheme = darkColorScheme(
    primary = GreenPrimary,
    secondary = BlueAccent,
    tertiary = PurpleAccent,
    background = DarkBackground,
    surface = DarkCard,
    onPrimary = DarkBackground,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = RedLive
)

private val LightScheme = lightColorScheme(
    primary = GreenDark,
    secondary = BlueAccent,
    tertiary = PurpleAccent,
    background = LightBackground,
    surface = LightCard,
    onPrimary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = RedLive
)

@Composable
fun FociElemzoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        AppColors(
            background = DarkBackground,
            card = DarkCard,
            surface = DarkSurface,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            green = GreenPrimary,
            redLive = RedLive,
            orangeLive = OrangeLive,
            blueAccent = BlueAccent,
            purpleAccent = PurpleAccent,
            cyanAccent = CyanAccent,
            liveRow = LiveRowDark,
            pinGold = PinGold,
            isDark = true
        )
    } else {
        AppColors(
            background = LightBackground,
            card = LightCard,
            surface = LightSurface,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            green = GreenDark,
            redLive = RedLive,
            orangeLive = OrangeLive,
            blueAccent = BlueAccent,
            purpleAccent = PurpleAccent,
            cyanAccent = CyanAccent,
            liveRow = LiveRowLight,
            pinGold = PinGold,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            content = content
        )
    }
}
