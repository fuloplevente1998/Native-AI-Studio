package com.example

import com.example.ui.theme.GlassContrast
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.GlassOptics
import com.example.ui.theme.KyantMaterialSpec
import com.example.ui.theme.LiquidGlassPalettes
import com.example.ui.theme.withGlassClarity
import com.example.ui.theme.liquidChromeTint
import com.repforge.app.ui.theme.NativeThemeTokens
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
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

    @Test fun fixedChromeKeepsColorBlurAndSuppressesScrollingGhostsAtEveryClarity() {
        for (seed in LiquidGlassPalettes.all) for (step in 0..100) {
            val p = seed.withGlassClarity(step)
            val shade = com.example.ui.theme.fixedGlassChromeShadeAlpha(step)
            val blur = com.example.ui.theme.fixedGlassChromeMinimumBlurDp(step)
            val tint = liquidChromeTint(p)
            assertTrue(shade in .819f...881f)
            assertTrue(blur in 12f..16f)
            assertTrue("colored midtone: ${p.key} step $step", tint.luminance() in .075f...110f)
            val coat = tint.copy(alpha = shade)
            val dark = coat.compositeOver(Color.Black)
            val bright = coat.compositeOver(GlassOptics.lensWorstCase)
            // Bound *difference* between dark and bright captured content rather than
            // rewarding a near-black header. Keep the actual foreground readable.
            assertTrue("black crush: ${p.key} $step", dark.luminance() >= .045f)
            assertTrue("ghost: ${p.key} $step", NativeThemeTokens.contrast(bright, dark) <= 1.5f)
            assertTrue(NativeThemeTokens.contrast(p.textPrimary, bright) >= 4.5f)
            assertTrue(NativeThemeTokens.contrast(p.textSecondary, bright) >= 4.5f)
        }
        assertTrue(com.example.ui.theme.fixedGlassChromeShadeAlpha(0) >
            com.example.ui.theme.fixedGlassChromeShadeAlpha(100))
        val ice = liquidChromeTint(LiquidGlassPalettes.all.first { it.key == "glass_ice" })
        val amber = liquidChromeTint(LiquidGlassPalettes.all.first { it.key == "glass_amber" })
        assertTrue(ice.blue > ice.red)
        assertTrue(amber.red > amber.blue)
    }

    @Test fun navigationFadeCoatIsBoundedAndPreservesContrastThroughoutReveal() {
        var previous = 1f
        for (frame in 100 downTo 0) {
            val alpha = com.example.ui.theme.navigationHeaderCoatAlpha(frame / 100f)
            assertTrue(alpha <= previous + .0001f)
            assertTrue(alpha in .649f...881f)
            previous = alpha
            for (p in LiquidGlassPalettes.all) {
                val coat = liquidChromeTint(p).copy(alpha = alpha)
                val composite = coat.compositeOver(GlassOptics.lensWorstCase)
                assertTrue(composite.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
                assertTrue(NativeThemeTokens.contrast(p.textPrimary, composite) >= 4.5f)
                assertTrue(NativeThemeTokens.contrast(p.textSecondary, composite) >= 4.5f)
            }
        }
        assertEquals(.65f, com.example.ui.theme.navigationHeaderCoatAlpha(-1f), .001f)
        assertEquals(.88f, com.example.ui.theme.navigationHeaderCoatAlpha(2f), .001f)
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
