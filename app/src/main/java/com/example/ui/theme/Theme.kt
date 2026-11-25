package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotExtendedColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalTrainPilotPalette = staticCompositionLocalOf {
    TrainPilotPalettes.allPalettes.first()
}

/** One mapping for Studio and embedded native editors, including light selection colors. */
internal fun nativeStudioColors(palette: TrainPilotPalette): TrainPilotExtendedColors {
    val selection = palette.selectionColors()
    // 2811/G4: native (rf) cards in Liquid Glass are translucent tiles; legacy palettes: the exact colours.
    return TrainPilotExtendedColors(bg = palette.background, card = palette.glassTile(palette.surfaceCard),
        card2 = palette.glassTile(palette.surfaceElevated, elevated = true),
        line = palette.cardBorder, text = palette.textPrimary, muted = palette.textSecondary,
        accent = palette.primary, accent2 = palette.secondary, onAccent = palette.readablePrimaryLabel(),
        accentText = selection.foreground,
        selectedSurface = selection.surface,
        selectedBorder = selection.border,
        dangerText = palette.danger, success = palette.success, emphasisBorder = palette.sectionBorder)
}

@Composable
fun TrainPilotTheme(
    palette: TrainPilotPalette = TrainPilotPalettes.allPalettes.first(),
    fontScaleMultiplier: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.readablePrimaryLabel(),
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = palette.primaryBright,
            secondary = palette.secondary,
            onSecondary = palette.background,
            secondaryContainer = palette.surfaceElevated,
            onSecondaryContainer = palette.textPrimary,
            tertiary = palette.tertiary,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.cardBorder.copy(alpha = 0.6f),
            error = palette.danger
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.readablePrimaryLabel(),
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.secondary,
            onSecondary = palette.surface,
            secondaryContainer = palette.surfaceElevated,
            onSecondaryContainer = palette.textPrimary,
            tertiary = palette.tertiary,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.cardBorder.copy(alpha = 0.6f),
            error = palette.danger
        )
    }

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = (currentDensity.fontScale * fontScaleMultiplier).coerceIn(0.85f, 2.5f)
    )

    val nativeColors = remember(palette) { nativeStudioColors(palette) }
    CompositionLocalProvider(
        LocalTrainPilotPalette provides palette,
        LocalTrainPilotColors provides nativeColors,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
