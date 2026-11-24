package com.example

import com.example.ui.theme.PaletteCategory
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.nativeStudioColors
import com.example.ui.theme.readablePrimaryLabel
import com.example.ui.theme.selectionColors
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

class ThemeSelection349Test {
    @Test fun allBuiltInThemesHaveReadableSharedSelection() {
        val palettes = TrainPilotPalettes.allPalettes
        assertEquals((1..32).toList(), palettes.map { it.id })
        for (category in listOf(PaletteCategory.MATTE, PaletteCategory.CONTRAST,
            PaletteCategory.GLOW, PaletteCategory.LIGHT)) {
            assertEquals(8, palettes.count { it.subGroup == category })
        }
        for (palette in palettes) {
            val selection = palette.selectionColors()
            assertEquals(1f, selection.surface.alpha)
            for (surface in listOf(palette.background, palette.surfaceCard,
                palette.surfaceElevated, selection.surface)) {
                assertTrue("Theme ${palette.id}: selection foreground",
                    NativeThemeTokens.contrast(selection.foreground, surface) >= 4.5f)
                assertTrue("Theme ${palette.id}: selection border",
                    NativeThemeTokens.contrast(selection.border, surface) >= 3f)
            }
            assertTrue("Theme ${palette.id}: filled button label",
                NativeThemeTokens.contrast(palette.readablePrimaryLabel(), palette.primary) >= 4.5f)
            val native = nativeStudioColors(palette)
            assertEquals(palette.surfaceCard, native.card)
            assertEquals(palette.surfaceElevated, native.card2)
            assertEquals(selection.surface, native.selectedSurface)
            assertEquals(selection.foreground, native.accentText)
            assertEquals(selection.border, native.selectedBorder)
        }
    }

    @Test fun approvedReplacementsPreserveSavedKeys() {
        val replacements = mapOf(9 to "copper_forge", 13 to "titanium_lime", 16 to "espresso_bronze")
        for ((id, key) in replacements) {
            val palette = TrainPilotPalettes.getById(id)
            assertEquals(key, palette.key)
            assertEquals(palette, TrainPilotPalettes.getByKey(key))
            assertEquals(PaletteCategory.CONTRAST, palette.subGroup)
        }
        assertEquals("graphite_gold", TrainPilotPalettes.getById(1).key)
    }
}
