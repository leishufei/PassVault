package com.example.passvault.ui.theme

import androidx.compose.ui.graphics.Color

// ── Canvas & Surface (Light) ────────────────────────────────────────────
val Canvas = Color(0xFFF7F5F2)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF0EEEB)

// ── Ink / Text (Light) ──────────────────────────────────────────────────
val InkPrimary = Color(0xFF1A1A1A)
val InkSecondary = Color(0xFF4A4744)
val InkMuted = Color(0xFF787470)
val InkFaint = Color(0xFFA8A29E)

// ── Accent (single structural color) ────────────────────────────────────
val Accent = Color(0xFF0075DE)
val AccentPressed = Color(0xFF005BAB)
val AccentSubtle = Color(0xFFEBF4FC)

// ── Semantic ────────────────────────────────────────────────────────────
val SemanticSuccess = Color(0xFF1A9E38)
val SemanticError = Color(0xFFD93025)
val SemanticWarning = Color(0xFFD97706)

// ── Structural / Chrome (Light) ─────────────────────────────────────────
val Hairline = Color(0xFFE6E3DF)
val DividerLight = Color(0xFFEDEAE6)

// ── Canvas & Surface (Dark) ─────────────────────────────────────────────
val CanvasDark = Color(0xFF0F0F0F)
val SurfaceDark = Color(0xFF1A1A1A)
val SurfaceVariantDark = Color(0xFF242424)

// ── Ink / Text (Dark) ───────────────────────────────────────────────────
val InkPrimaryDark = Color(0xFFEBEBEB)
val InkSecondaryDark = Color(0xFFA8A29E)
val InkMutedDark = Color(0xFF787470)
val InkFaintDark = Color(0xFF524F4B)

// ── Accent (Dark) ───────────────────────────────────────────────────────
val AccentDark = Color(0xFF4DA3E8)
val AccentPressedDark = Color(0xFF3B8DD4)
val AccentSubtleDark = Color(0xFF1A2A3D)

// ── Semantic (Dark) ─────────────────────────────────────────────────────
val SemanticSuccessDark = Color(0xFF3DB856)
val SemanticErrorDark = Color(0xFFE85A4F)
val SemanticWarningDark = Color(0xFFE8A030)

// ── Structural / Chrome (Dark) ──────────────────────────────────────────
val HairlineDark = Color(0xFF2E2E2E)
val DividerLightDark = Color(0xFF242424)

// ── Category Palette (decorative, desaturated) ──────────────────────────
val CategoryColors = listOf(
    Color(0xFF4A8FD4),
    Color(0xFF5B6AC7),
    Color(0xFF8B6BBF),
    Color(0xFFC93F4A),
    Color(0xFF2A9D99),
    Color(0xFFC96A2E),
    Color(0xFF3D9B56),
    Color(0xFFC75B8F),
    Color(0xFF7A5C3B)
)

fun safeCategoryColor(index: Int): Color =
    CategoryColors[Math.floorMod(index, CategoryColors.size)]

// ── Legacy Aliases (deprecated, for incremental migration) ──────────────
@Deprecated("Use Canvas", ReplaceWith("Canvas"))
val LightBackground = Canvas
@Deprecated("Use Surface", ReplaceWith("Surface"))
val LightSurface = Surface
@Deprecated("Use SurfaceVariant", ReplaceWith("SurfaceVariant"))
val LightSurfaceVariant = SurfaceVariant
@Deprecated("Use Surface", ReplaceWith("Surface"))
val CardBackground = Surface
@Deprecated("Use Hairline", ReplaceWith("Hairline"))
val CardBorder = Hairline
@Deprecated("Use Accent", ReplaceWith("Accent"))
val PrimaryBlue = Accent
@Deprecated("Use AccentPressed", ReplaceWith("AccentPressed"))
val PrimaryBlueLight = Accent
@Deprecated("Use AccentPressed", ReplaceWith("AccentPressed"))
val PrimaryBlueDark = AccentPressed
@Deprecated("Use InkPrimary", ReplaceWith("InkPrimary"))
val TextPrimary = InkPrimary
@Deprecated("Use InkSecondary", ReplaceWith("InkSecondary"))
val TextSecondary = InkSecondary
@Deprecated("Use InkMuted", ReplaceWith("InkMuted"))
val TextTertiary = InkMuted
@Deprecated("Use SemanticSuccess", ReplaceWith("SemanticSuccess"))
val SuccessGreen = SemanticSuccess
@Deprecated("Use SemanticError", ReplaceWith("SemanticError"))
val ErrorRed = SemanticError
@Deprecated("Use SemanticWarning", ReplaceWith("SemanticWarning"))
val WarningYellow = SemanticWarning
@Deprecated("Use Accent", ReplaceWith("Accent"))
val InfoBlue = Accent
@Deprecated("Use SemanticError", ReplaceWith("SemanticError"))
val AlertPulseRed = SemanticError
@Deprecated("Use SemanticSuccess", ReplaceWith("SemanticSuccess"))
val GlowGreen = SemanticSuccess
@Deprecated("Use Surface", ReplaceWith("Surface"))
val NavBarBackground = Surface
@Deprecated("Use AccentSubtle", ReplaceWith("AccentSubtle"))
val NavBarIndicator = AccentSubtle
@Deprecated("Use Surface", ReplaceWith("Surface"))
val DialogBackground = Surface
@Deprecated("Use Hairline", ReplaceWith("Hairline"))
val DividerColor = Hairline
@Deprecated("Use CategoryColors", ReplaceWith("CategoryColors"))
val BrandColors = CategoryColors
@Deprecated("Use safeCategoryColor", ReplaceWith("safeCategoryColor(index)"))
fun safeBrandColor(index: Int): Color = safeCategoryColor(index)

@Deprecated("Use Canvas", ReplaceWith("Canvas"))
val DarkBackground = Canvas
@Deprecated("Use Surface", ReplaceWith("Surface"))
val DarkSurface = Surface
@Deprecated("Use SurfaceVariant", ReplaceWith("SurfaceVariant"))
val DarkSurfaceVariant = SurfaceVariant
@Deprecated("Use Accent", ReplaceWith("Accent"))
val PrimaryPurple = Accent
@Deprecated("Use Accent", ReplaceWith("Accent"))
val PrimaryPurpleLight = Accent
@Deprecated("Use AccentPressed", ReplaceWith("AccentPressed"))
val PrimaryIndigo = AccentPressed
