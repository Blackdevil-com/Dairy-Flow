package com.dairyflow.app.core.theme

import androidx.compose.ui.graphics.Color

// MILKMOO Inspired Design System Palette
val DairyPrimary = Color(0xFF5B50EF)          // Vibrant Indigo / Violet Primary
val DairyPrimaryLight = Color(0xFFEFEEFF)     // Soft Lavender Container
val DairyPrimaryDark = Color(0xFF3B2FC9)      // Deep Indigo
val DairyPrimaryHover = Color(0xFF4B3FE0)

val DairyBackground = Color(0xFFF8F8FC)        // Milk-white / crisp soft light background
val DairySurface = Color(0xFFFFFFFF)           // Pure White Surface
val DairySurfaceVariant = Color(0xFFF2F2FA)    // Subtle Card Background
val DairyBorder = Color(0xFFEAEAF4)            // Clean light border

val DairyTextPrimary = Color(0xFF191A32)       // Dark Navy primary text
val DairyTextSecondary = Color(0xFF77788C)     // Balanced secondary text
val DairyTextMuted = Color(0xFFA5A6BA)         // Subtle muted text

val DairySuccess = Color(0xFF10B981)           // Modern Emerald Green
val DairySuccessLight = Color(0xFFECFDF5)      // Light green pill container
val DairyWarning = Color(0xFFF59E0B)           // Warm Amber
val DairyWarningLight = Color(0xFFFFFBEB)      // Light amber pill container
val DairyCritical = Color(0xFFEF4444)          // Soft Red
val DairyCriticalLight = Color(0xFFFEF2F2)     // Light red pill container
val DairyBlue = Color(0xFF3B82F6)              // Vibrant sky blue
val DairyBlueLight = Color(0xFFEFF6FF)

// Backwards-Compatible Aliases for existing screens
val DeepTeal = DairyPrimary
val DarkTeal = DairyPrimaryDark
val TealMedium = DairyPrimaryHover
val TealContainer = DairyPrimaryLight
val TealContainerDark = Color(0xFFDCDAFE)
val TealLight = DairyPrimary

val GreenPositive = DairySuccess
val GreenContainer = DairySuccessLight
val AmberWarning = DairyWarning
val AmberContainer = DairyWarningLight
val RedCritical = DairyCritical
val RedContainer = DairyCriticalLight

val OffWhiteBackground = DairyBackground
val SurfaceWhite = DairySurface
val SurfaceVariant = DairySurfaceVariant
val OutlineVariant = DairyBorder
val TextPrimary = DairyTextPrimary
val TextSecondary = DairyTextSecondary
val TextMuted = DairyTextMuted
