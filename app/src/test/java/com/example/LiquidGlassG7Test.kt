package com.example

import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LiquidGlassG7Test {
    @Test fun nestedNavigationPaintIsTransparentAndBoundedAtEveryClarity() {
        val roles = listOf(GlassMaterialRole.Navigation, GlassMaterialRole.Button, GlassMaterialRole.Selection)
        val brightest = GlassOptics.lensWorstCase
        for (step in 0..100) {
            for (role in roles) {
                val paint = glassHeaderControlPaint(step, role)
                assertTrue("$role/$step", paint.alpha in 0f..0.37f)
                assertTrue("$role/$step", paint.compositeOver(brightest).luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
            }
            assertTrue(glassHeaderControlPaint(step, GlassMaterialRole.Selection).alpha >
                glassHeaderControlPaint(step, GlassMaterialRole.Button).alpha)
        }
        for (seed in LiquidGlassPalettes.all) for (step in listOf(0, 50, 65, 100)) {
            val p = seed.withGlassClarity(step)
            val track = glassHeaderControlPaint(step, GlassMaterialRole.Navigation).compositeOver(brightest)
            val selection = glassHeaderControlPaint(step, GlassMaterialRole.Selection).compositeOver(track)
            for (surface in listOf(track, selection)) for (ink in listOf(p.textPrimary, p.textSecondary, p.selectionColors().foreground)) {
                assertTrue("${p.key}/$step", NativeThemeTokens.contrast(ink, surface) >= 4.5f)
            }
        }
    }

    @Test fun headerUsesOneContentLensOverASharedRootScene() {
        fun source(path: String) = File("src/main/java/com/example/$path").readText()
        val header = source("ui/components/common/TopHeaderAndNav.kt")
        val scaffold = source("ui/theme/GlassAwareScaffold.kt")
        val content = source("ui/theme/GlassContentBackdrop.kt")
        val pill = source("ui/components/common/GlassSelectionPill.kt")
        assertEquals(1, Regex("\\.glassHeaderMaterial\\(").findAll(header).count())
        assertTrue(pill.contains("glassHeaderControl(palette, shape, GlassMaterialRole.Selection)"))
        assertFalse(pill.contains(".glassHeaderMaterial("))
        assertTrue(scaffold.contains("containerColor = if (palette.isLiquidGlass) Color.Transparent else palette.background"))
        assertTrue(scaffold.contains("content(bodyPadding, capture,"))
        assertFalse(content.contains("drawRect(palette.surface)"))
        assertTrue(content.contains("drawGlassSceneCompose(palette, scene, origin.value, sceneWidth, sceneHeight)"))
    }
}
