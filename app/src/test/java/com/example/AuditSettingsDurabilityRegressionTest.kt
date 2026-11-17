package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditSettingsDurabilityRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun store() = TrainPilotSettingsStore(context, publishTheme = {})

    @Test fun settingsSurviveNewStoreAndPartialTimerEdits() = runBlocking {
        val first = store()
        first.resetAllSettingsForEraseLocal()
        first.setFontScaleMultiplier(1.28f)
        first.setCompactPhoneMode(true)
        first.setHealthBackupConsent(true)
        first.setTimerDefaults(45, 20, 8, true, true, false)
        first.updateTimerPreferences(restSeconds = 35, sound = false)
        val saved = store().current()
        assertEquals(1.28f, saved.fontScaleMultiplier, 0.001f)
        assertTrue(saved.compactPhoneMode)
        assertTrue(saved.healthBackupConsent)
        assertEquals(45, saved.workSeconds)
        assertEquals(35, saved.restSeconds)
        assertEquals(8, saved.rounds)
        assertFalse(saved.soundEnabled)
        assertTrue(saved.vibrationEnabled)
        assertFalse(saved.intervalAutoAdvance)
    }

    @Test fun legacyAndInvalidImportsPreserveExistingValuesAndBoundsAreEnforced() = runBlocking {
        val store = store()
        store.resetAllSettingsForEraseLocal()
        assertEquals(1f, store.current().fontScaleMultiplier, 0f)
        assertFalse(store.current().compactPhoneMode)
        store.setFontScaleMultiplier(1.15f)
        store.setCompactPhoneMode(true)
        store.setHealthBackupConsent(true)
        store.importPortableSettings(JSONObject().put("settings", JSONObject().put("language", "en")))
        store.importPortableSettings(JSONObject().put("settings", JSONObject()
            .put("fontScaleMultiplier", "NaN").put("compactPhoneMode", "false")))
        var restored = this@AuditSettingsDurabilityRegressionTest.store().current()
        assertEquals(1.15f, restored.fontScaleMultiplier, 0.001f)
        assertTrue(restored.compactPhoneMode)
        assertTrue(restored.healthBackupConsent)
        store.importPortableSettings(JSONObject().put("settings", JSONObject()
            .put("fontScaleMultiplier", 99).put("compactPhoneMode", false)))
        restored = this@AuditSettingsDurabilityRegressionTest.store().current()
        assertEquals(2f, restored.fontScaleMultiplier, 0f)
        assertFalse(restored.compactPhoneMode)
        store.setFontScaleMultiplier(Float.NaN)
        assertEquals(1f, this@AuditSettingsDurabilityRegressionTest.store().current().fontScaleMultiplier, 0f)
    }
}
