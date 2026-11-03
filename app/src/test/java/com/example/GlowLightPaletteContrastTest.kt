package com.example

import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.TrainPilotPalettes
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

class GlowLightPaletteContrastTest {
    @Test fun glowAndLightTextStaysReadableOnAllBaseAndSelectedSurfaces() {
        for (palette in TrainPilotPalettes.allPalettes.filter { it.id in 17..32 }) {
            val surfaces = listOf(palette.background, palette.surfaceCard, palette.surfaceElevated,
                palette.tintedSurface(0.12f), palette.tintedSurface(0.16f), palette.tintedSurface(0.22f))
            for (foreground in listOf(palette.primary, palette.secondary, palette.textPrimary, palette.textSecondary)) {
                for (surface in surfaces) assertTrue("Palette ${palette.id}: text contrast",
                    NativeThemeTokens.contrast(foreground, surface) >= 4.5f)
            }
            assertNotEquals(palette.background, palette.surfaceCard)
            assertNotEquals(palette.surfaceCard, palette.surfaceElevated)
            assertTrue(palette.background.luminance() < palette.surfaceCard.luminance())
            if (palette.hasGlow) assertTrue(palette.background.luminance() < 0.03f)
            else assertTrue(palette.background.luminance() > 0.6f && palette.surfaceCard.luminance() > 0.9f)
        }
    }
}
