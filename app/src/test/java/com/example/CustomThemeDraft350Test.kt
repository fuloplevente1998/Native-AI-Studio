package com.example

import com.example.ui.theme.CustomThemeDraft
import com.example.ui.theme.CustomThemeRole
import com.example.ui.theme.CustomThemeStyle
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.isCustomThemeKey
import com.example.ui.theme.newCustomThemeKey
import com.example.ui.theme.normalizeCustomThemeHex
import org.junit.Assert.*
import org.junit.Test

class CustomThemeDraft350Test {
    @Test fun strictRgbInputDoesNotAcceptPartialOrArgbValues() {
        assertEquals("#A1B2C3", normalizeCustomThemeHex(" #a1b2c3 "))
        for (value in listOf("", "#123", "#12345678", "123456", "#ZZ1234")) {
            assertNull(value, normalizeCustomThemeHex(value))
        }
    }

    @Test fun allFourStylesStartWithFiveValidRoles() {
        for (style in CustomThemeStyle.entries) {
            val draft = CustomThemeDraft.fromStyle(style, "Theme")
            assertTrue(draft.isInputValid)
            assertEquals(CustomThemeRole.entries.toSet(), draft.colors.keys)
        }
        assertEquals(32, TrainPilotPalettes.allPalettes.size)
        for (role in CustomThemeRole.entries) {
            assertTrue(role.quickColors().isNotEmpty())
            assertTrue(role.quickColors().all { normalizeCustomThemeHex(it) == it })
        }
    }

    @Test fun savedDefinitionRetainsStableKeyAcrossEdits() {
        val key = newCustomThemeKey()
        val draft = CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, " My theme ")
        val saved = requireNotNull(draft.toDefinition(key))
        assertEquals("My theme", saved.draft.name)
        assertEquals(key, saved.key)
        assertTrue(isCustomThemeKey(key))
        assertNull(TrainPilotPalettes.getByKey(key))
        val edited = requireNotNull(saved.draft.copy(name = "New name")
            .withColor(CustomThemeRole.PRIMARY, "#ab1234").toDefinition(saved.key))
        assertEquals(key, edited.key)
        assertEquals("#AB1234", edited.draft.colors[CustomThemeRole.PRIMARY])
        assertNull(draft.toDefinition("graphite_gold"))
        assertNotEquals(key, newCustomThemeKey())
    }

    @Test fun incompleteDraftAndUnsupportedOpacityCannotBecomeDefinition() {
        val draft = CustomThemeDraft.fromStyle(CustomThemeStyle.MATTE, "Theme")
        val key = newCustomThemeKey()
        assertNull(draft.copy(name = " ").toDefinition(key))
        assertNull(draft.withColor(CustomThemeRole.CARD, "#123").toDefinition(key))
        assertNull(draft.copy(colors = emptyMap()).toDefinition(key))
        for (opacity in listOf(0, 69, 91, 99, 101)) {
            assertNull(draft.copy(opacityPercent = opacity).toDefinition(key))
        }
        for (opacity in listOf(70, 80, 90, 100)) {
            assertNotNull(draft.copy(opacityPercent = opacity).toDefinition(key))
        }
    }
}
