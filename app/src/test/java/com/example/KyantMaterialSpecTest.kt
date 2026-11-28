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

    @Test fun fusedChromeCoatMatchesLayeredPaintWithoutAnotherDrawPass() {
        for (seed in LiquidGlassPalettes.all) for (step in 0..100) {
            val p = seed.withGlassClarity(step)
            for (role in listOf(GlassMaterialRole.Navigation, GlassMaterialRole.Input, GlassMaterialRole.Panel)) {
                val base = GlassOptics.TINT.copy(alpha = KyantMaterialSpec.forRole(role, step).tintAlpha)
                val coat = liquidChromeTint(p).copy(alpha = com.example.ui.theme.fixedGlassChromeShadeAlpha(step))
                val fused = com.example.ui.theme.kyantSurfaceCoat(p, role, null, coat)
                for (under in listOf(Color.Black, GlassOptics.lensWorstCase, p.background)) {
                    val expected = coat.compositeOver(base.compositeOver(under))
                    val actual = fused.compositeOver(under)
                    assertEquals(expected.red, actual.red, .012f)
                    assertEquals(expected.green, actual.green, .012f)
                    assertEquals(expected.blue, actual.blue, .012f)
                    assertTrue(NativeThemeTokens.contrast(p.textPrimary, actual) >= 4.5f)
                }
                assertEquals(base, com.example.ui.theme.kyantSurfaceCoat(p, role, null, null))
            }
        }
    }

    @Test fun fixedChromeKeepsColorBlurAndSuppressesScrollingGhostsAtEveryClarity() {
        for (seed in LiquidGlassPalettes.all) for (step in 0..100) {
            val p = seed.withGlassClarity(step)
            val shade = com.example.ui.theme.fixedGlassChromeShadeAlpha(step)
            val blur = com.example.ui.theme.fixedGlassChromeMinimumBlurDp(step)
            val tint = liquidChromeTint(p)
            assertTrue(shade in 0.819f..0.881f)
            assertTrue(blur in 22f..26f)
            assertTrue("colored midtone: ${p.key} step $step", tint.luminance() in 0.075f..0.110f)
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

    @Test fun globalHeaderPaintDoesNotDependOnNavigationRoute() {
        // Device feedback: all nine navigation buttons previously triggered a full-header
        // 24% tint pulse. Keep the static feather and optical material, but never key its
        // paint on the selected tab or subscreen. Selection animations belong to the controls.
        val header = java.io.File("src/main/java/com/example/ui/components/common/TopHeaderAndNav.kt").readText()
        val background = header.substringAfter("Box(Modifier.matchParentSize()")
            .substringBefore(".testTag(\"top_header_glass_background\")")
        assertTrue(background.contains("BlendMode.DstIn"))
        assertTrue(background.contains("glassHeaderMaterial(palette"))
        assertFalse(background.contains("currentTab"))
        assertFalse(background.contains("currentSubScreen"))
        assertFalse(header.contains("glassNavigationFade"))
        for (p in LiquidGlassPalettes.all) for (step in 0..100) {
            val palette = p.withGlassClarity(step)
            val spec = KyantMaterialSpec.forRole(GlassMaterialRole.Navigation, step)
            val paint = com.example.ui.theme.kyantSurfaceCoat(palette, GlassMaterialRole.Navigation, null, null)
            assertEquals(GlassOptics.TINT.copy(alpha = spec.tintAlpha), paint)
            // The optical input is scaled BEFORE the tint. lensWorstCase is already
            // a final composite, so applying paint to it again double-counts the coat.
            val worst = maxOf(
                GlassContrast.blurOnlyWorst(paint.alpha, spec.saturation, spec.colorScale),
                KyantMaterialSpec.sceneWorst(paint.alpha, spec.saturation, spec.sceneScale))
            assertTrue(worst <= GlassOptics.SURFACE_LUMA_MAX)
            val composite = GlassContrast.grey(worst)
            assertTrue(NativeThemeTokens.contrast(palette.textPrimary, composite) >= 4.5f)
            assertTrue(NativeThemeTokens.contrast(palette.textSecondary, composite) >= 4.5f)
        }
    }

    @Test fun pageLightFallsOffContinuouslyWithoutAHeaderCutAndPrismAvoidsDenseRoles() {
        var previous = 1f
        for (i in 0..100) {
            val value = com.example.ui.theme.GlassScene.topShadeAlpha(i / 100f)
            assertTrue(value <= previous)
            assertTrue(value in 0f..0.45f)
            if (i > 0) assertTrue(previous - value <= .015f)
            previous = value
        }
        assertEquals(.45f, com.example.ui.theme.GlassScene.topShadeAlpha(0f), .001f)
        assertEquals(0f, com.example.ui.theme.GlassScene.topShadeAlpha(.32f), .001f)
        assertFalse(com.example.ui.theme.glassPrismaticRimRole(GlassMaterialRole.Tile))
        assertFalse(com.example.ui.theme.glassPrismaticRimRole(GlassMaterialRole.Performance))
        for (role in listOf(GlassMaterialRole.Navigation, GlassMaterialRole.Panel,
            GlassMaterialRole.Selection, GlassMaterialRole.Button, GlassMaterialRole.Input, GlassMaterialRole.Overlay)) {
            assertTrue(com.example.ui.theme.glassPrismaticRimRole(role))
        }
    }

    @Test fun distinctRolesAndConservativeContrastBoundForSceneAndCapturedContent() {
        for (step in 0..100) {
            fun role(role: GlassMaterialRole) = KyantMaterialSpec.forRole(role, step)
            assertTrue(role(GlassMaterialRole.Navigation).tintAlpha < role(GlassMaterialRole.Panel).tintAlpha)
            assertTrue(role(GlassMaterialRole.Overlay).tintAlpha > role(GlassMaterialRole.Button).tintAlpha)
            assertTrue(role(GlassMaterialRole.Selection).lensAmountDp > role(GlassMaterialRole.Navigation).lensAmountDp)
            assertTrue(role(GlassMaterialRole.Panel).lensAmountDp > role(GlassMaterialRole.Navigation).lensAmountDp)
            assertTrue(role(GlassMaterialRole.Input).lensAmountDp < role(GlassMaterialRole.Button).lensAmountDp)
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
