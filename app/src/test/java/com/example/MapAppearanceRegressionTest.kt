package com.example

import com.repforge.app.data.store.MapAppearance
import org.junit.Assert.*
import org.junit.Test

class MapAppearanceRegressionTest {
    @Test fun absentLegacyPreferenceFollowsBothAppThemes() {
        val mode = MapAppearance.fromStored(null, null)
        assertEquals(MapAppearance.FOLLOW_APP, mode)
        assertTrue(mode.isDark(true)); assertFalse(mode.isDark(false))
    }
    @Test fun explicitLegacyChoicesRemainExplicitWhileUnwrittenDefaultDoesNot() {
        assertEquals(MapAppearance.DARK, MapAppearance.fromStored(null, true))
        assertEquals(MapAppearance.LIGHT, MapAppearance.fromStored(null, false))
        assertFalse(MapAppearance.LIGHT.isDark(true)); assertTrue(MapAppearance.DARK.isDark(false))
    }
    @Test fun returnToAppThemeOverridesAnOlderBoolean() {
        assertEquals(MapAppearance.FOLLOW_APP, MapAppearance.fromStored("FOLLOW_APP", false))
    }
}
