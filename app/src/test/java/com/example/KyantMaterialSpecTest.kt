package com.example

import com.example.ui.theme.GlassContrast
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.GlassOptics
import com.example.ui.theme.KyantMaterialSpec
import org.junit.Assert.*
import org.junit.Test

class KyantMaterialSpecTest {
    @Test fun allRolesFollowTheSameClaritySliderWithoutExtraTileCapture() {
        for (role in GlassMaterialRole.entries) {
            val frost = KyantMaterialSpec.forRole(role, 0)
            val clear = KyantMaterialSpec.forRole(role, 100)
            assertSame(frost, KyantMaterialSpec.forRole(role, -1))
            assertSame(clear, KyantMaterialSpec.forRole(role, 101))
            for (step in 0..100) {
                val material = KyantMaterialSpec.forRole(role, step)
                assertSame(material, KyantMaterialSpec.forRole(role, step))
                assertTrue(material.tintAlpha in 0f..1f)
                assertTrue(material.blurDp >= 0f && material.lensAmountDp >= 0f)
                assertEquals(role !in listOf(GlassMaterialRole.Tile, GlassMaterialRole.Performance), material.usesBackdrop)
                assertEquals(role !in listOf(GlassMaterialRole.Tile, GlassMaterialRole.Performance), material.usesLens)
                if (step > 0) {
                    val before = KyantMaterialSpec.forRole(role, step - 1)
                    assertTrue(material.tintAlpha < before.tintAlpha)
                    assertTrue(material.blurDp <= before.blurDp)
                    assertTrue(material.lensAmountDp >= before.lensAmountDp)
                    assertTrue(material.highlightAlpha >= before.highlightAlpha)
                }
            }
        }
    }

    @Test fun fixedChromeKeepsBlurAndSuppressesBrightScrollingContentAtEveryClarity() {
        for (step in 0..100) {
            val shade = com.example.ui.theme.fixedGlassChromeShadeAlpha(step)
            val blur = com.example.ui.theme.fixedGlassChromeMinimumBlurDp(step)
            assertTrue(shade in 0.49f..0.61f)
            assertTrue(blur in 12f..16f)
            // Bound the contrast of a captured bright glyph after the shade, not the
            // real foreground text (which is drawn after this coat and stays bright).
            val bright = GlassContrast.grey(GlassOptics.SURFACE_LUMA_MAX)
            val luma = GlassContrast.luminance(bright.red * (1f - shade),
                bright.green * (1f - shade), bright.blue * (1f - shade))
            assertTrue("step $step: underlap ghost luma $luma", luma <= .04f)
            assertTrue((1.05f / (luma + .05f)) >= 4.5f)
        }
    }

    @Test fun navigationFadeCoatIsBoundedAndPreservesContrastThroughoutReveal() {
        var previous = 1f
        for (frame in 100 downTo 0) {
            val alpha = com.example.ui.theme.navigationHeaderCoatAlpha(frame / 100f)
            assertTrue(alpha <= previous + .0001f)
            assertTrue(alpha in 0.299f..0.901f)
            previous = alpha
            // Both the guarded optical layer and the neutral coat remain within the contrast ceiling.
            val brightest = GlassOptics.lensWorstCase
            val coat = GlassContrast.grey(.065f)
            val composite = GlassContrast.luminance(
                coat.red * alpha + brightest.red * (1f - alpha),
                coat.green * alpha + brightest.green * (1f - alpha),
                coat.blue * alpha + brightest.blue * (1f - alpha))
            assertTrue(composite <= GlassOptics.SURFACE_LUMA_MAX + .001f)
        }
        assertEquals(.3f, com.example.ui.theme.navigationHeaderCoatAlpha(-1f), .001f)
        assertEquals(.9f, com.example.ui.theme.navigationHeaderCoatAlpha(2f), .001f)
    }

    @Test fun distinctRolesAndConservativeContrastBoundForSceneAndCapturedContent() {
        for (step in 0..100) {
            fun role(role: GlassMaterialRole) = KyantMaterialSpec.forRole(role, step)
            assertTrue(role(GlassMaterialRole.Navigation).tintAlpha < role(GlassMaterialRole.Panel).tintAlpha)
            assertTrue(role(GlassMaterialRole.Overlay).tintAlpha > role(GlassMaterialRole.Button).tintAlpha)
            assertTrue(role(GlassMaterialRole.Selection).lensAmountDp > role(GlassMaterialRole.Navigation).lensAmountDp)
            assertFalse(role(GlassMaterialRole.Tile).shadow)
            assertFalse(role(GlassMaterialRole.Performance).shadow)
            assertFalse(role(GlassMaterialRole.Performance).usesBackdrop)
            assertFalse(role(GlassMaterialRole.Performance).usesLens)
            for (r in GlassMaterialRole.entries.filter { it !in listOf(GlassMaterialRole.Tile, GlassMaterialRole.Performance) }) {
                val material = role(r)
                assertTrue(material.colorScale in 0f..1f)
                assertTrue(material.sceneScale in material.colorScale..1f)
                assertTrue(GlassContrast.blurOnlyWorst(material.tintAlpha, material.saturation, material.colorScale)
                    <= GlassOptics.SURFACE_LUMA_MAX)
                assertTrue(KyantMaterialSpec.sceneWorst(material.tintAlpha, material.saturation, material.sceneScale)
                    <= GlassOptics.SURFACE_LUMA_MAX)
            }
        }
    }
}
