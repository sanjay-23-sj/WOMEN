package com.sanx.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

// SanX uses the system default sans-serif which maps to Google Sans / Roboto on most
// modern Android phones, maintaining lightweight APK size without bundling custom fonts.
val SanXFontFamily = FontFamily.Default

val SanXTypography = Typography(
    // App brand header
    displayLarge = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
        color = SanXTextPrimary
    ),
    // Section headings
    headlineMedium = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp,
        color = SanXTextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        color = SanXTextPrimary
    ),
    // Card titles
    titleMedium = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = SanXTextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
        color = SanXTextSecondary
    ),
    // Body text
    bodyLarge = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        color = SanXTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
        color = SanXTextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = SanXTextDisabled
    ),
    // Labels and tags
    labelLarge = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.6.sp,
        color = SanXTextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
        color = SanXTextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = SanXFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.0.sp,
        color = SanXTextDisabled
    )
)
