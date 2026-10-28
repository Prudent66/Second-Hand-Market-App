package com.secondhandmarket.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = Color(0xFF00295D),
    primaryContainer = Color(0xFF0D4478),
    onPrimaryContainer = Color(0xFFBDD9FF),
    secondary = SecondaryYellow,
    onSecondary = Color(0xFF3B2E00),
    background = DarkBackground,
    onBackground = Color(0xFFE4E9F0),
    surface = DarkSurface,
    onSurface = Color(0xFFE4E9F0),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA6B2C1),
    outline = DarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5C1108)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E5FF),
    onPrimaryContainer = Color(0xFF00295D),
    secondary = Color(0xFFB98A00),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF17202B),
    surface = LightSurface,
    onSurface = Color(0xFF17202B),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF5A6675),
    outline = LightOutline,
    error = Color(0xFFB3261E),
    onError = Color.White
)

/**
 * App theme with a fixed brand palette. Dynamic (wallpaper-derived) colors are
 * disabled on purpose so the marketplace keeps one consistent look on every
 * device instead of inheriting muddy grays from the user's wallpaper.
 */
@Composable
fun SecondHandMarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
