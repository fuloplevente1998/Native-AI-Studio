package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class KyantK2IntegrationTest {
    @Test fun semanticStatePaintPreservesContrastAndLegacy() {
        for (seed in LiquidGlassPalettes.all) for (step in 0..100) {
            val p = seed.withGlassClarity(step)
            for (accent in listOf(p.primary, p.success, p.danger, p.warning)) for (strong in listOf(false, true)) {
                val paint = p.glassStateTile(accent, Color.Black, strong)
                assertTrue(paint.alpha in 0.01f..0.45f)
                val composite = paint.compositeOver(GlassOptics.lensWorstCase)
                assertTrue(composite.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
                assertTrue(NativeThemeTokens.contrast(p.textPrimary, composite) >= 4.5f)
            }
        }
        for (p in TrainPilotPalettes.allPalettes) {
            assertEquals(p.surface, p.glassStateTile(p.primary, p.surface))
        }
    }

    @Test fun navigationUsesSingleHeaderLensAndNestedLightControls() {
        fun source(path: String) = File("src/main/java/com/example/$path").readText()
        val header = source("ui/components/common/TopHeaderAndNav.kt")
        val pill = source("ui/components/common/GlassSelectionPill.kt")
        assertTrue(header.contains("glassHeaderControl("))
        assertFalse(pill.contains(".glassHeaderMaterial("))
        assertTrue(pill.contains(".glassHeaderControl("))
        assertTrue(source("ui/theme/GlassAwareScaffold.kt").contains("content(bodyPadding, capture,"))
    }
}
