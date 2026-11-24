package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.*
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomThemeStore350Test {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun ticket() = NativeDataReplacement.continuationForCurrentData(context)
    private fun theme(name: String = "My theme"): CustomThemeDefinition {
        val key = newCustomThemeKey()
        val draft = CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, name).copy(opacityPercent = 80)
        val resolved = requireNotNull(resolveCustomTheme(draft, key))
        return requireNotNull(resolved.suggestedDraft.toDefinition(key))
    }

    @Test fun saveApplyEditAndDeleteUseOneDurableStoreAndPublishAfterCommit() = runBlocking {
        val definition = theme()
        val published = mutableListOf<JSONObject>()
        val store = TrainPilotSettingsStore(context, publishTheme = {
            published.add(TrainPilotSettingsStore(it).current().themeJson())
        })
        val initial = store.current().themeKey
        store.saveCustomThemeForDataset(definition, ticket())
        assertEquals(initial, store.current().themeKey)
        assertTrue(published.isEmpty())
        assertTrue(TrainPilotSettingsStore(context).current().customThemes().contains(definition))

        store.applyCustomThemeForDataset(definition.key, ticket())
        assertEquals(definition.key, published.last().getString("key"))
        assertEquals("contrast", published.last().getString("family"))
        assertEquals(definition.key, TrainPilotSettingsStore(context).current().resolvedThemePalette().key)
        assertEquals(1, published.size)

        val edited = requireNotNull(definition.draft.copy(name = "Renamed").toDefinition(definition.key))
        store.saveCustomThemeForDataset(edited, ticket())
        assertEquals(edited, store.current().customThemes().first { it.key == definition.key })
        assertEquals(2, published.size)
        assertEquals("Renamed", store.current().resolvedThemePalette().nameEn)

        store.deleteCustomThemeForDataset(definition.key, ticket())
        assertEquals(TrainPilotPalettes.getById(1).key, store.current().themeKey)
        assertEquals(TrainPilotPalettes.getById(1).key, published.last().getString("key"))
        assertFalse(store.current().customThemes().any { it.key == definition.key })
        assertEquals(3, published.size)
    }

    @Test fun portableRestoreKeepsStableKeysAndMissingCustomThemeFallsBack() = runBlocking {
        val definition = theme("Backup")
        val store = TrainPilotSettingsStore(context, publishTheme = {})
        val payload = JSONObject().put("settings", JSONObject().put("themeKey", definition.key)
            .put("customThemes350", JSONArray(CustomThemeCodec.encode(listOf(definition)))))
        store.importPortableSettings(payload, replaceTrainingModesOnMissing = true)
        assertEquals(definition.key, store.current().themeKey)
        assertEquals(listOf(definition), store.current().customThemes())
        assertEquals(definition.key, store.current().themeJson().getString("key"))
        store.importPortableSettings(JSONObject().put("settings", JSONObject().put("themeKey", definition.key)
            .put("customThemes350", JSONArray())), replaceTrainingModesOnMissing = true)
        assertTrue(store.current().customThemes().isEmpty())
        assertEquals("graphite_gold", store.current().themeKey)
    }

    @Test fun codecSkipsBrokenEntriesAndThemePayloadNeverUsesCorruptCustomKey() {
        val definition = theme()
        val raw = JSONArray(CustomThemeCodec.encode(listOf(definition))).put(JSONObject().put("version", 1))
        assertEquals(listOf(definition), CustomThemeCodec.decode(raw.toString()))
        assertEquals(emptyList<CustomThemeDefinition>(), CustomThemeCodec.decode("broken"))
        assertEquals("graphite_gold", TrainPilotUserSettings(themeKey = definition.key,
            customThemesJson = "broken").themeJson().getString("key"))
    }
    @Test fun glassClarityIsFamilyWideDurableAndPortable() = runBlocking {
        val store = TrainPilotSettingsStore(context, publishTheme = {})
        val p = TrainPilotPalettes.getById(35)
        store.importPortableSettings(JSONObject().put("settings", JSONObject()
            .put("themeKey", p.key).put("glassClarity350", 12)), replaceTrainingModesOnMissing = true)
        assertEquals(p.key, store.current().themeKey)
        assertEquals(12, store.current().glassClarity)
        assertEquals(12, store.current().resolvedThemePalette().glassClarity)
        assertEquals("contrast", store.current().themeJson().getString("family"))
        store.setGlassClarityForDataset(93, ticket())
        assertEquals(93, TrainPilotSettingsStore(context).current().glassClarity)
        assertEquals(93, store.current().resolvedThemePalette().glassClarity)
        store.importPortableSettings(JSONObject().put("settings", JSONObject()
            .put("themeKey", TrainPilotPalettes.getById(1).key)), replaceTrainingModesOnMissing = false)
        assertEquals(93, store.current().glassClarity)
        assertEquals(TrainPilotPalettes.getById(1), store.current().resolvedThemePalette())
        store.importPortableSettings(JSONObject().put("settings", JSONObject()
            .put("themeKey", TrainPilotPalettes.getById(40).key)), replaceTrainingModesOnMissing = false)
        assertEquals(93, store.current().resolvedThemePalette().glassClarity)
    }

}
