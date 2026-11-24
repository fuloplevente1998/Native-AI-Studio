package com.example.ui.theme

import org.json.JSONArray
import org.json.JSONObject

/** Versioned portable records; broken entries are ignored without discarding valid siblings. */
internal object CustomThemeCodec {
    fun encode(themes: Collection<CustomThemeDefinition>): String = JSONArray().apply {
        themes.forEach { theme ->
            val draft = theme.draft
            put(JSONObject().put("version", 1).put("key", theme.key).put("name", draft.name)
                .put("style", draft.style.name).put("opacity", draft.opacityPercent)
                .put("colors", JSONObject().apply {
                    CustomThemeRole.entries.forEach { put(it.name, draft.colors.getValue(it)) }
                }))
        }
    }.toString()

    fun decode(raw: String): List<CustomThemeDefinition> {
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            runCatching {
                if (obj.optInt("version", -1) != 1 || obj.opt("name") !is String ||
                    obj.opt("key") !is String || obj.opt("style") !is String) return@runCatching null
                val opacity = obj.opt("opacity") as? Number ?: return@runCatching null
                val percent = opacity.toInt()
                if (opacity.toDouble() != percent.toDouble()) return@runCatching null
                val colors = obj.optJSONObject("colors") ?: return@runCatching null
                val style = CustomThemeStyle.valueOf(obj.getString("style"))
                val draft = CustomThemeDraft(obj.getString("name"), style,
                    CustomThemeRole.entries.associateWith { colors.optString(it.name, "") }, percent)
                draft.toDefinition(obj.getString("key"))
            }.getOrNull()
        }.distinctBy { it.key }
    }
}
