package com.repforge.app

import org.json.JSONObject

/** Provider-specific snapshots in the existing portable health240 document; never mix sources. */
internal object NativeWorkoutHealthSource {
    val providers = setOf(HealthJournalStore.HC, HealthJournalStore.SAMSUNG)
    fun requireProvider(provider: String): String = provider.also { require(it in providers) }
    fun snapshot(document: JSONObject?, provider: String): JSONObject? {
        requireProvider(provider)
        if (document == null) return null
        document.optJSONObject("providers")?.optJSONObject(provider)?.let { return it }
        // Original flat backups were Health Connect unless they explicitly named Samsung.
        return document.takeIf { it.optString("provider", HealthJournalStore.HC) == provider &&
            (it.has("windowStart") || it.has("start")) }
    }
    fun attach(existing: JSONObject?, provider: String, response: JSONObject): JSONObject {
        requireProvider(provider)
        val old = existing?.let { JSONObject(it.toString()) } ?: JSONObject()
        val snapshots = old.optJSONObject("providers") ?: JSONObject()
        for (source in providers) snapshot(existing, source)?.let { snapshots.put(source, JSONObject(it.toString())) }
        val measured = JSONObject(response.toString()).put("provider", provider)
        snapshots.put(provider, measured)
        // Preserve the historical flat HC view for older consumers and backups.
        val out = if (provider == HealthJournalStore.HC) JSONObject(measured.toString()) else old
        return out.put("providers", snapshots)
    }
    suspend fun read(workout: JSONObject, provider: String,
        healthConnect: suspend (NativeWorkoutActiveWindows.Window) -> JSONObject,
        samsung: suspend (NativeWorkoutActiveWindows.Window) -> JSONObject): JSONObject {
        requireProvider(provider)
        return NativeWorkoutActiveWindows.health(workout) { window ->
            if (provider == HealthJournalStore.SAMSUNG) samsung(window) else healthConnect(window)
        }.put("provider", provider)
    }
    fun label(provider: String) = if (requireProvider(provider) == HealthJournalStore.SAMSUNG) "Samsung Health" else "Health Connect"
}
