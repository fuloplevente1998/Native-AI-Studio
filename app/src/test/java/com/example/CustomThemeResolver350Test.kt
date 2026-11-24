package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

class CustomThemeResolver350Test {
    @Test fun extremesAndMixedSurfacesResolveToReadablePalette() {
        val samples = listOf("#000000", "#FFFFFF", "#777777", "#FF0000", "#0000FF")
        val key = newCustomThemeKey()
        for (style in CustomThemeStyle.entries) {
            for (background in samples) for (card in samples) for (primary in samples) {
                for (opacity in listOf(70, 90, 100)) {
                    val draft = CustomThemeDraft.fromStyle(style, "Test").copy(opacityPercent = opacity)
                        .withColor(CustomThemeRole.BACKGROUND, background)
                        .withColor(CustomThemeRole.CARD, card)
                        .withColor(CustomThemeRole.PRIMARY, primary)
                    val resolved = requireNotNull(resolveCustomTheme(draft, key))
                    val p = resolved.palette
                    assertEquals(key, p.key)
                    assertEquals(-1, p.id)
                    assertEquals(TrainPilotPalettes.getById(style.basePaletteId).subGroup, p.subGroup)
                    val surfaces = listOf(p.background, p.surfaceCard, p.surfaceElevated,
                        p.tintedSurface(.12f), p.tintedSurface(.16f), p.tintedSurface(.22f))
                    for (surface in surfaces) {
                        for (text in listOf(p.textPrimary, p.textSecondary, p.primary, p.secondary,
                            p.success, p.danger, p.warning, p.heartAccent, p.sleepAccent,
                            p.stepsAccent, p.energyAccent)) {
                            assertTrue("Style $style, colors $background/$card/$primary, opacity $opacity",
                                NativeThemeTokens.contrast(text, surface) >= 4.5f)
                        }
                    }
                    assertTrue(NativeThemeTokens.contrast(p.onPrimary, p.primary) >= 4.5f)
                    for (surface in listOf(p.background, p.surfaceCard, p.surfaceElevated)) {
                        assertTrue(NativeThemeTokens.contrast(p.cardBorder, surface) >= 3f)
                    }
                    assertTrue(requireNotNull(resolveCustomTheme(resolved.suggestedDraft, key))
                        .correctedRoles.isEmpty())
                }
            }
        }
    }

    @Test fun oldOpacityRecordsStayReadableButRenderAsOpaqueCustomThemes() {
        for (style in CustomThemeStyle.entries) {
            val key = newCustomThemeKey()
            val draft = CustomThemeDraft.fromStyle(style, "Legacy").copy(opacityPercent = 70)
            val resolved = requireNotNull(resolveCustomTheme(draft, key))
            val opaque = requireNotNull(resolveCustomTheme(draft.copy(opacityPercent = 100), key))
            assertEquals(opaque.palette, resolved.palette)
            assertNull(resolved.palette.customPageEnd)
            assertNull(resolved.palette.customCardPaint)
            assertEquals(1f, resolved.palette.cardPaint.alpha)
            assertEquals(100, resolved.suggestedDraft.opacityPercent)
            assertEquals(100, requireNotNull(draft.toDefinition(key)).draft.opacityPercent)
        }
    }

    @Test fun invalidInputIsNotSilentlyReplacedAndCorrectionsAreExplicit() {
        val key = newCustomThemeKey()
        val draft = CustomThemeDraft.fromStyle(CustomThemeStyle.MATTE, "Dark")
        assertNull(resolveCustomTheme(draft.withColor(CustomThemeRole.PRIMARY, "#123"), key))
        assertNull(resolveCustomTheme(draft, "graphite_gold"))
        val whitePage = draft.withColor(CustomThemeRole.BACKGROUND, "#FFFFFF")
        val resolved = requireNotNull(resolveCustomTheme(whitePage, key))
        assertTrue(CustomThemeRole.BACKGROUND in resolved.correctedRoles)
        assertEquals("#FFFFFF", whitePage.colors[CustomThemeRole.BACKGROUND])
        assertNotEquals(Color.White, resolved.palette.background)
        assertEquals(32, TrainPilotPalettes.allPalettes.size)
    }
}
