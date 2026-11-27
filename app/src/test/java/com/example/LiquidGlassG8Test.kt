package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

class LiquidGlassG8Test {
    @Test fun sharedControlPaintIsBoundedAndPreservesLegacy() {
        val roles = listOf(GlassMaterialRole.Navigation, GlassMaterialRole.Button, GlassMaterialRole.Selection)
        val base = GlassOptics.lensWorstCase
        for (seed in LiquidGlassPalettes.all) for (step in listOf(0, 50, 65, 100)) {
            val p = seed.withGlassClarity(step)
            for (role in roles) {
                val paint = p.glassControlFill(p.primary, role)
                assertEquals(glassControlPaint(p, step, role), paint)
                val surface = paint.compositeOver(base)
                assertTrue(paint.alpha < 0.4f)
                assertTrue(surface.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
                assertTrue(NativeThemeTokens.contrast(p.textPrimary, surface) >= 4.5f)
                assertTrue(NativeThemeTokens.contrast(p.textSecondary, surface) >= 4.5f)
            }
        }
        val custom = CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, "G8 custom").let {
            requireNotNull(resolveCustomTheme(it, newCustomThemeKey())).palette
        }
        for (p in TrainPilotPalettes.allPalettes + custom) for (role in roles) {
            for (paint in listOf(p.primary, p.surface, p.danger, Color.Transparent))
                assertEquals(paint, p.glassControlFill(paint, role))
        }
    }
}
