package com.footballai.analyzer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Legacy names – theme-aware via LocalAppColors where possible,
// fallback to dark palette for non-composable contexts.
val TextPrimary: Color get() = TextPrimaryDark
val TextSecondary: Color get() = TextSecondaryDark
