package com.sanx.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Theme Color Palette Definition ──────────────────────────────────────────
data class ColorPalette(
    val sanXBlack: Color,
    val sanXSurface: Color,
    val sanXCard: Color,
    val sanXBorder: Color,
    val sanXCardHover: Color,
    val sanXTextPrimary: Color,
    val sanXTextSecondary: Color,
    val sanXTextDisabled: Color,
    val sanXTextHint: Color,
    val sanXSafe: Color,
    val sanXSafeDim: Color
)

val DarkColors = ColorPalette(
    sanXBlack = Color(0xFF0A0A0C),        // Matte volcanic black background
    sanXSurface = Color(0xFF16161E),      // Deep space grey surfaces
    sanXCard = Color(0xFF1E1E28),         // Sleek charcoal cards
    sanXBorder = Color(0xFF2A2A38),       // Charcoal-gray borders
    sanXCardHover = Color(0xFF252530),
    sanXTextPrimary = Color(0xFFF0F0F5),
    sanXTextSecondary = Color(0xFF8888A0),
    sanXTextDisabled = Color(0xFF4A4A5A),
    sanXTextHint = Color(0xFF5A5A6E),
    sanXSafe = Color(0xFFFF2B69),         // Soft rose-pink accent highlights (WOMEN brand)
    sanXSafeDim = Color(0x33FF2B69)       // Pink accent dim glow
)

val LightColors = ColorPalette(
    sanXBlack = Color(0xFFF6F6F9),        // Soft premium white/light-gray background
    sanXSurface = Color(0xFFFFFFFF),      // Pure white surfaces
    sanXCard = Color(0xFFEFEFF4),         // Soft light gray cards
    sanXBorder = Color(0xFFE2E2EC),       // Clean light gray dividers/borders
    sanXCardHover = Color(0xFFE5E5EB),
    sanXTextPrimary = Color(0xFF1C1C1E),  // Sleek dark gray text
    sanXTextSecondary = Color(0xFF6E6E73),// Subtle mid-tone gray text
    sanXTextDisabled = Color(0xFF9E9EAF),
    sanXTextHint = Color(0xFFB5B5C3),
    sanXSafe = Color(0xFFFF2B69),         // Dynamic rose-pink branding accents
    sanXSafeDim = Color(0x22FF2B69)       // Soft pink glow
)

// ─── Theme State Holder ───────────────────────────────────────────────────────
object AppTheme {
    var isDark by mutableStateOf(true)

    val colors: ColorPalette
        get() = if (isDark) DarkColors else LightColors
}

// ─── Material Theme Color Schemes ─────────────────────────────────────────────
private val SanXDarkColorScheme = darkColorScheme(
    primary          = Color(0xFFFF2B69),
    onPrimary        = Color(0xFF0A0A0C),
    primaryContainer = Color(0x33FF2B69),
    onPrimaryContainer = Color(0xFFFF2B69),

    secondary        = Color(0xFF5B9CF6),
    onSecondary      = Color(0xFF0A0A0C),
    secondaryContainer = Color(0xFF1A2540),
    onSecondaryContainer = Color(0xFF5B9CF6),

    error            = Color(0xFFFF4545),
    onError          = Color(0xFF0A0A0C),
    errorContainer   = Color(0x33FF4545),
    onErrorContainer = Color(0xFFFF4545),

    background       = Color(0xFF0A0A0C),
    onBackground     = Color(0xFFF0F0F5),

    surface          = Color(0xFF16161E),
    onSurface        = Color(0xFFF0F0F5),
    surfaceVariant   = Color(0xFF1E1E28),
    onSurfaceVariant = Color(0xFF8888A0),

    outline          = Color(0xFF2A2A38),
    outlineVariant   = Color(0x0DFFFFFF),

    inverseSurface        = Color(0xFFF0F0F5),
    inverseOnSurface      = Color(0xFF0A0A0C),
    inversePrimary        = Color(0xFF0A0A0C)
)

private val SanXLightColorScheme = lightColorScheme(
    primary          = Color(0xFFFF2B69),
    onPrimary        = Color.White,
    primaryContainer = Color(0x22FF2B69),
    onPrimaryContainer = Color(0xFFFF2B69),

    secondary        = Color(0xFF5B9CF6),
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFE2E2EC),
    onSecondaryContainer = Color(0xFF5B9CF6),

    error            = Color(0xFFFF4545),
    onError          = Color.White,
    errorContainer   = Color(0x33FF4545),
    onErrorContainer = Color(0xFFFF4545),

    background       = Color(0xFFF6F6F9),
    onBackground     = Color(0xFF1C1C1E),

    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF1C1C1E),
    surfaceVariant   = Color(0xFFEFEFF4),
    onSurfaceVariant = Color(0xFF6E6E73),

    outline          = Color(0xFFE2E2EC),
    outlineVariant   = Color(0x0D000000),

    inverseSurface        = Color(0xFF1C1C1E),
    inverseOnSurface      = Color.White,
    inversePrimary        = Color.White
)

@Composable
fun SanXTheme(content: @Composable () -> Unit) {
    val darkTheme = AppTheme.isDark
    val colorScheme = if (darkTheme) SanXDarkColorScheme else SanXLightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SanXTypography,
        content     = content
    )
}
