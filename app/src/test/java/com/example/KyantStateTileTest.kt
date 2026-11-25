package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

class KyantStateTileTest {
    @Test fun allClarityStepsKeepSemanticPaintTranslucentAndBounded() {
        for (seed in LiquidGlassPalettes.all) for (step in 0..100) {
            val p = seed.withGlassClarity(step)
            for (accent in listOf(p.primary, p.success, p.danger, p.warning)) {
                val weak = p.kyantStateTile(accent, accent)
                val strong = p.kyantStateTile(accent, accent, strong = true)
                assertTrue(strong.alpha > weak.alpha && strong.alpha < 1f)
                for (fill in listOf(weak, strong)) {
                    assertTrue(maxOf(fill.red, fill.green, fill.blue) <= GlassOptics.LENS_PEAK + .004f)
                    val over = fill.compositeOver(GlassOptics.lensWorstCase)
                    assertTrue(NativeThemeTokens.contrast(p.textPrimary, over) >= 4.5f)
                }
            }
        }
    }

    @Test fun nonGlassThemesKeepExactlyTheirOriginalPaint() {
        val custom = requireNotNull(resolveCustomTheme(
            CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, "Kyant"), newCustomThemeKey())).palette
        for (p in TrainPilotPalettes.allPalettes + custom) {
            for (legacy in listOf(p.surface, p.primary, p.danger, Color.Transparent))
                assertEquals(legacy, p.kyantStateTile(p.primary, legacy, strong = true))
        }
    }
}
