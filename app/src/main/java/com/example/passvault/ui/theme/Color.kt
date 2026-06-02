package com.example.passvault.ui.theme

import androidx.compose.ui.graphics.Color

// ── Background System (Light Mode) ──────────────────────────────────────
val LightBackground = Color(0xFFF0F9FF)          // 极浅灰蓝背景
val LightSurface = Color(0xFFFFFFFF)              // 白色卡片/表面
val LightSurfaceVariant = Color(0xFFE0F2FE)       // 浅蓝变体 (搜索框/输入框)

// ── Card System ─────────────────────────────────────────────────────────
val CardBackground = Color(0xFFFFFFFF)             // 白色卡片
val CardBorder = Color(0xFFE0F2FE)                 // 浅蓝边框

// ── Primary Colors ──────────────────────────────────────────────────────
val PrimaryBlue = Color(0xFF0369A1)                // Security Blue 主色
val PrimaryBlueLight = Color(0xFF0EA5E9)           // 辅助亮蓝
val PrimaryBlueDark = Color(0xFF075985)            // 深蓝 (按下态)

// ── Text Colors ─────────────────────────────────────────────────────────
val TextPrimary = Color(0xFF0C4A6E)                // 深海蓝 (主文本)
val TextSecondary = Color(0xFF64748B)              // 灰蓝 (次级文本)
val TextTertiary = Color(0xFF94A3B8)               // 浅灰蓝 (提示文本)

// ── Semantic Colors ─────────────────────────────────────────────────────
val SuccessGreen = Color(0xFF22C55E)               // Protected Green
val ErrorRed = Color(0xFFEF4444)                   // 错误/危险红
val WarningYellow = Color(0xFFF59E0B)              // 警告黄
val InfoBlue = Color(0xFF0EA5E9)                   // 信息蓝

// ── Animation-specific Colors ───────────────────────────────────────────
val AlertPulseRed = Color(0xFFDC2626)              // 弱密码脉冲警报
val GlowGreen = Color(0xFF4ADE80)                  // 复制成功微光 (Glow)

// ── Navigation ──────────────────────────────────────────────────────────
val NavBarBackground = Color(0xFFFFFFFF)            // 导航栏白色
val NavBarIndicator = Color(0x1A0369A1)             // 选中指示器 (PrimaryBlue 10%)

// ── Dialog / Overlay ────────────────────────────────────────────────────
val DialogBackground = Color(0xFFFFFFFF)            // 对话框白色
val DividerColor = Color(0xFFE2E8F0)               // 分割线

// ── Brand Colors (for category icons) ───────────────────────────────────
val BrandColors = listOf(
    Color(0xFF4285F4),
    Color(0xFF1877F2),
    Color(0xFF6E40C9),
    Color(0xFFE50914),
    Color(0xFF1DA1F2),
    Color(0xFF1677FF),
    Color(0xFFCC0000),
    Color(0xFF00B900),
    Color(0xFFFF6900)
)

/** Safe index into BrandColors — handles negative values from ARGB-overflow ints */
fun safeBrandColor(index: Int): Color =
    BrandColors[Math.floorMod(index, BrandColors.size)]

// ── Legacy Aliases (backward compat during transition) ──────────────────
// These map old names to new values to minimize import breakage
val DarkBackground = LightBackground
val DarkSurface = LightSurface
val DarkSurfaceVariant = LightSurfaceVariant
val PrimaryPurple = PrimaryBlue
val PrimaryPurpleLight = PrimaryBlueLight
val PrimaryIndigo = PrimaryBlueDark
