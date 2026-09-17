package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ThemePrimary,
    onPrimary = ThemeOnPrimary,
    primaryContainer = ThemePrimaryContainer,
    onPrimaryContainer = ThemeOnPrimaryContainer,
    secondary = ThemePrimary,
    onSecondary = ThemeOnPrimary,
    secondaryContainer = ThemeSurfaceVariant,
    onSecondaryContainer = ThemeTextPrimary,
    tertiary = ThemePrimary,
    onTertiary = ThemeOnPrimary,
    background = ThemeBackground,
    onBackground = ThemeTextPrimary,
    surface = ThemeSurface,
    onSurface = ThemeTextPrimary,
    surfaceVariant = ThemeSurfaceVariant,
    onSurfaceVariant = ThemeTextSecondary,
    outline = ThemeOutline,
    outlineVariant = ThemeOutlineVariant,
    error = ThemeError,
    onError = ThemeOnPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

