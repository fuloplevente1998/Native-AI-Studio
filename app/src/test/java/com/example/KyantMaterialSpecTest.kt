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
                assertEquals(role != GlassMaterialRole.Tile, material.usesBackdrop)
                assertEquals(role != GlassMaterialRole.Tile, material.usesLens)
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

    @Test fun distinctRolesAndConservativeContrastBoundForSceneAndCapturedContent() {
        for (step in 0..100) {
            fun role(role: GlassMaterialRole) = KyantMaterialSpec.forRole(role, step)
            assertTrue(role(GlassMaterialRole.Navigation).tintAlpha < role(GlassMaterialRole.Panel).tintAlpha)
            assertTrue(role(GlassMaterialRole.Overlay).tintAlpha > role(GlassMaterialRole.Button).tintAlpha)
            assertTrue(role(GlassMaterialRole.Selection).lensAmountDp > role(GlassMaterialRole.Navigation).lensAmountDp)
            assertFalse(role(GlassMaterialRole.Tile).shadow)
            for (r in GlassMaterialRole.entries.filter { it != GlassMaterialRole.Tile }) {
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
