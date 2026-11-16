package com.periodtracker.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.periodtracker.domain.Appearance
object JournalDimensions { val gutter = 20.dp; val gap = 12.dp; val radius = 20.dp }
private val Light = lightColorScheme(primary = Color(0xFF7155A5), onPrimary = Color.White,
 primaryContainer = Color(0xFFE9E2F4), onPrimaryContainer = Color(0xFF302047),
 background = Color(0xFFFAF9F7), surface = Color(0xFFFFFDFC), onSurface = Color(0xFF292533),
 onBackground = Color(0xFF292533), secondary = Color(0xFF655979), tertiary = Color(0xFF536866))
private val Dark = darkColorScheme(primary = Color(0xFFB9A5DF), onPrimary = Color(0xFF231638),
 primaryContainer = Color(0xFF443457), onPrimaryContainer = Color(0xFFF0E6FF),
 background = Color(0xFF19171E), surface = Color(0xFF27232E), onSurface = Color(0xFFF7F4FA),
 onBackground = Color(0xFFF7F4FA), secondary = Color(0xFFC8BBD8), tertiary = Color(0xFFACCBC5))
@Composable fun PeriodTrackerTheme(appearance: Appearance = Appearance.SYSTEM, content: @Composable () -> Unit) {
 val dark = appearance == Appearance.DARK || (appearance == Appearance.SYSTEM && isSystemInDarkTheme())
 MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography, content = content)
}
