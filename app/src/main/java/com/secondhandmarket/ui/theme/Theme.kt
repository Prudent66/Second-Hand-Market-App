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
    primary = AquaSoft,
    onPrimary = Color(0xFF00332E),
    primaryContainer = Color(0xFF0B4F5E),
    onPrimaryContainer = Color(0xFFBDF3EE),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF002733),
    secondaryContainer = Color(0xFF0B3A4A),
    onSecondaryContainer = Color(0xFFC4E9F7),
    tertiary = Color(0xFF67E8F9),
    onTertiary = Color(0xFF00252C),
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightTextSecondary,
    outline = NightOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5C1108)
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3ECF1),
    onPrimaryContainer = Color(0xFF06303F),
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDF5F0),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = CyanAccent,
    onTertiary = Color.White,
    background = CloudBackground,
    onBackground = InkPrimary,
    surface = CloudSurface,
    onSurface = InkPrimary,
    surfaceVariant = CloudSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = CloudOutline,
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
            window.navigationBarColor = colorScheme.surface.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
