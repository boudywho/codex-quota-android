package com.codex.quota.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.unit.ColorProvider
import com.codex.quota.domain.model.WidgetThemeMode

data class WidgetColors(
    val background: ColorProvider,
    val surface: ColorProvider,
    val textPrimary: ColorProvider,
    val textSecondary: ColorProvider,
    val textMuted: ColorProvider,
    val accent: ColorProvider,
    val accentBlue: ColorProvider,
    val error: ColorProvider,
    val progressTrack: ColorProvider
)

object WidgetThemeHelper {

    @Composable
    fun getColors(mode: WidgetThemeMode): WidgetColors {
        return when (mode) {
            WidgetThemeMode.SYSTEM_MATERIAL_YOU -> {
                WidgetColors(
                    background = GlanceTheme.colors.surface,
                    surface = GlanceTheme.colors.secondaryContainer,
                    textPrimary = GlanceTheme.colors.onSurface,
                    textSecondary = GlanceTheme.colors.onSecondaryContainer,
                    textMuted = GlanceTheme.colors.outline,
                    accent = GlanceTheme.colors.primary,
                    accentBlue = GlanceTheme.colors.secondary,
                    error = GlanceTheme.colors.error,
                    progressTrack = GlanceTheme.colors.outline
                )
            }
            WidgetThemeMode.DARK_OBSIDIAN -> {
                WidgetColors(
                    background = ColorProvider(Color(0xFF0B0F19)),
                    surface = ColorProvider(Color(0xFF1A2234)),
                    textPrimary = ColorProvider(Color(0xFFF8FAFC)),
                    textSecondary = ColorProvider(Color(0xFF94A3B8)),
                    textMuted = ColorProvider(Color(0xFF64748B)),
                    accent = ColorProvider(Color(0xFF10B981)),
                    accentBlue = ColorProvider(Color(0xFF38BDF8)),
                    error = ColorProvider(Color(0xFFEF4444)),
                    progressTrack = ColorProvider(Color(0xFF334155))
                )
            }
        }
    }
}
