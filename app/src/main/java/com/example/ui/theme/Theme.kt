package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkEmeraldColorScheme =
  darkColorScheme(
    primary = PosGreenAction,
    onPrimary = PosBackground,
    primaryContainer = PosPanelSecondary,
    onPrimaryContainer = PosGreenActive,
    secondary = PosGreenPrimary,
    onSecondary = PosTextPrimary,
    secondaryContainer = PosPanel,
    onSecondaryContainer = PosGreenActive,
    tertiary = PosInfo,
    onTertiary = PosBackground,
    tertiaryContainer = PosPanelSecondary,
    onTertiaryContainer = PosInfo,
    background = PosBackground,
    onBackground = PosTextPrimary,
    surface = PosPanel,
    onSurface = PosTextPrimary,
    surfaceVariant = PosPanelSecondary,
    onSurfaceVariant = PosTextSecondary,
    outline = PosBorder,
    outlineVariant = PosBorderLight,
    error = PosError,
    onError = PosBackground,
    errorContainer = PosErrorLight,
    onErrorContainer = PosError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkEmeraldColorScheme,
    typography = Typography,
    content = content
  )
}


