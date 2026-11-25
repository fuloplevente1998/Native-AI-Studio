package com.example

import androidx.compose.material3.ButtonColors
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.GlassOptics
import com.example.ui.theme.LiquidGlassPalettes
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.withGlassClarity
import com.repforge.app.ui.theme.NativeThemeTokens
import com.repforge.app.ui.theme.glassPrimaryActionColors
import com.repforge.app.ui.theme.useGlassPrimaryAction
import org.junit.Assert.*
import org.junit.Test

class GlassControlsG3Test {
    @Test fun auditedPrimaryActionsUseReadableForegroundAndPreserveDisabledColors() {
        for (seed in LiquidGlassPalettes.all) for (clarity in listOf(0, 50, 65, 100)) {
            val p = seed.withGlassClarity(clarity)
            val original = ButtonColors(p.primary, p.onPrimary, p.surface, p.textSecondary)
            val glass = glassPrimaryActionColors(p, true, original)
            assertTrue(useGlassPrimaryAction(p, true, original))
            assertEquals(Color.Transparent, glass.containerColor)
            assertTrue(NativeThemeTokens.contrast(glass.contentColor, GlassOptics.lensWorstCase) >= 4.5f)
            assertEquals(original.disabledContainerColor, glass.disabledContainerColor)
            assertEquals(original.disabledContentColor, glass.disabledContentColor)
            assertSame(original, glassPrimaryActionColors(p, false, original))
            val danger = ButtonColors(p.danger, Color.Black, p.surface, p.textSecondary)
            assertSame(danger, glassPrimaryActionColors(p, true, danger))
            val explicit = ButtonColors(p.primary, p.textPrimary, p.surface, p.textSecondary)
            if (p.textPrimary != p.onPrimary) assertSame(explicit, glassPrimaryActionColors(p, true, explicit))
        }
    }

    @Test fun nonGlassPrimaryActionsRetainTheirExactColors() {
        for (p in TrainPilotPalettes.allPalettes) {
            val original = ButtonColors(p.primary, p.onPrimary, p.surface, p.textSecondary)
            assertFalse(useGlassPrimaryAction(p, true, original))
            assertSame(original, glassPrimaryActionColors(p, true, original))
        }
    }
}
