package com.sanx.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand Theme Palette (Dynamic properties bound to AppTheme.colors State) ──
val SanXBlack: Color        get() = AppTheme.colors.sanXBlack
val SanXSurface: Color      get() = AppTheme.colors.sanXSurface
val SanXCard: Color         get() = AppTheme.colors.sanXCard
val SanXBorder: Color       get() = AppTheme.colors.sanXBorder
val SanXCardHover: Color    get() = AppTheme.colors.sanXCardHover

// ─── Text Colors ─────────────────────────────────────────────────────────────
val SanXTextPrimary: Color    get() = AppTheme.colors.sanXTextPrimary
val SanXTextSecondary: Color  get() = AppTheme.colors.sanXTextSecondary
val SanXTextDisabled: Color   get() = AppTheme.colors.sanXTextDisabled
val SanXTextHint: Color       get() = AppTheme.colors.sanXTextHint

// ─── Dynamic Brand Safe Highlight Accent ──────────────────────────────────────
val SanXSafe: Color           get() = AppTheme.colors.sanXSafe
val SanXSafeDim: Color        get() = AppTheme.colors.sanXSafeDim

// ─── Constant Status Accents ──────────────────────────────────────────────────
val SanXEmergency    = Color(0xFFFF4545)   // Safety Emergency Red (Static)
val SanXEmergencyDim = Color(0x33FF4545)
val SanXWarning      = Color(0xFFFF9500)   // Safety Warning Orange (Static)
val SanXWarningDim   = Color(0x33FF9500)
val SanXInfo         = Color(0xFF5B9CF6)   // Safety Info Blue (Static)
val SanXMesh         = Color(0xFFC77DFF)   // Mesh Network Purple (Static)
val SanXMeshDim      = Color(0x33C77DFF)

// ─── Utility ───────────────────────────────────────────────────────────────────
val SanXWhiteAlpha10 = Color(0x1AFFFFFF)
val SanXWhiteAlpha05 = Color(0x0DFFFFFF)
val Transparent      = Color(0x00000000)
