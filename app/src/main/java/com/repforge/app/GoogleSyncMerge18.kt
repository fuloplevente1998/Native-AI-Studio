package com.repforge.app

import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.shared.LegacySyncIdentity18
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import kotlin.math.max
import com.repforge.app.GoogleSyncRepository.Companion.SNAPSHOT_NAME_REGEX

// =========================================================================
// #18 Multi-Device Cloud Merge Engine (Exact Port of www/index.html #18)
// =========================================================================

internal fun selectSnapshots18(files: JSONArray): Pair<List<JSONObject>, List<JSONObject>> {
    val groups = LinkedHashMap<String, MutableList<JSONObject>>()
    for (i in 0 until files.length()) {
        val f = files.optJSONObject(i) ?: continue
        val m = SNAPSHOT_NAME_REGEX.find(f.optString("name", "")) ?: continue
        val deviceId = m.groupValues[1]
        groups.getOrPut(deviceId) { ArrayList() }.add(f)
    }
    val current = ArrayList<JSONObject>()
    val oldest = ArrayList<JSONObject>()
    val recent = ArrayList<JSONObject>()
    for (rows in groups.values) {
        rows.sortBy { it.optString("createdTime", "") }
        if (rows.isEmpty()) continue
        current.add(rows.last())
        val older = rows.subList(0, rows.size - 1)
        if (older.isNotEmpty()) {
            oldest.add(older.first())
            for (j in older.size - 1 downTo 1) {
                recent.add(older[j])
            }
        }
    }
    current.sortBy { it.optString("createdTime", "") }
    val archive = ArrayList<JSONObject>()
    val seen = current.map { it.optString("id") }.toMutableSet()
    for (f in oldest + recent) {
        val id = f.optString("id")
        if (id.isEmpty() || !seen.add(id)) continue
        archive.add(f)
        if (archive.size >= 48) break
    }
    return current to archive
}

internal fun GoogleSyncRepository.mergeSync18(
    local: JSONObject,
    remotes: List<JSONObject>,
    base: JSONObject?,
    archives: List<JSONObject>,
    deletedHistoryIds: Set<String>,
    deletedHistoryKeys: Set<String>,
    preferLocalOnConflict: Boolean
): JSONObject {
    val out = JSONObject(local.toString())
    fun readWeightMarkers(data: JSONObject?): List<NativeWeightTombstones18.Marker> =
        if (data == null || !data.has(NativeWeightTombstones18.KEY)) emptyList()
        else NativeWeightTombstones18.decode(data.get(NativeWeightTombstones18.KEY))
    val mergedWeightMarkers = NativeWeightTombstones18.union(readWeightMarkers(local), readWeightMarkers(base),
        *remotes.map(::readWeightMarkers).toTypedArray(), *archives.map(::readWeightMarkers).toTypedArray())
    out.put(NativeWeightTombstones18.KEY, NativeWeightTombstones18.encode(mergedWeightMarkers))
    out.put(
        "history",
        mergeHistoryRows18(
            local.optJSONArray("history"),
            remotes.map { it.optJSONArray("history") },
            base?.optJSONArray("history"),
            archives.map { it.optJSONArray("history") },
            deletedHistoryIds,
            deletedHistoryKeys,
            preferLocalOnConflict
        )
    )
    out.put(
        "weights",
        mergeWeightRows18(
            local.optJSONArray("weights"),
            remotes.map { it.optJSONArray("weights") },
            base?.optJSONArray("weights"),
            archives.map { it.optJSONArray("weights") },
            preferLocalOnConflict, NativeWeightTombstones18.Index(mergedWeightMarkers)
        )
    )
    out.put("loosePhotos", mergeLoosePhotoRows18(
        local.optJSONArray("loosePhotos"),
        remotes.map { it.optJSONArray("loosePhotos") },
        base?.optJSONArray("loosePhotos"),
        archives.map { it.optJSONArray("loosePhotos") }
    ))
    out.put(
        "scheduled",
        mergeScheduledRows18(
            local.optJSONArray("scheduled"),
            remotes.map { it.optJSONArray("scheduled") },
            preferLocalOnConflict
        )
    )
    val plannerAndGoalsKeys = setOf("plan", "plannerSettings", NativeCalendarBackup.STUDIO_SETTINGS_KEY,
        LegacyWebViewMigrator.USER_GOALS_KEY)
    for (key in listOf("settings", "exercises", "programs", "activeProgramId", "exerciseFavorites") + plannerAndGoalsKeys) {
        var selected = local.opt(key)
        for (remote in remotes) {
            val x = remote.opt(key) ?: continue
            if (key in plannerAndGoalsKeys) {
                if (remote.isNull(key)) continue
                if (selected == null || selected == JSONObject.NULL) {
                    selected = x
                    continue
                }
            }
            if (LegacySyncIdentity18.canonical(selected) == LegacySyncIdentity18.canonical(x)) continue
            val baseVal = base?.opt(key)
            if (baseVal != null && LegacySyncIdentity18.canonical(x) == LegacySyncIdentity18.canonical(baseVal)) continue
            selected = if (baseVal != null && LegacySyncIdentity18.canonical(selected) == LegacySyncIdentity18.canonical(baseVal)) {
                x
            } else if (preferLocalOnConflict) {
                selected
            } else {
                x
            }
        }
        if (key == LegacyWebViewMigrator.USER_GOALS_KEY && selected is JSONObject) {
            selected = LegacyWebViewMigrator.overlayGoals(local.optJSONObject(key), selected)
        }
        if (selected != null) out.put(key, selected)
    }
    return out
}

private fun mergeHistoryVariant18(a: JSONObject, b: JSONObject): JSONObject? {
    if (LegacySyncIdentity18.workoutIdentity(a) != LegacySyncIdentity18.workoutIdentity(b)) return null
    val merged = JSONObject(a.toString())
    val incoming = JSONObject(b.toString())
    val keys = incoming.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        val v = incoming.opt(k)
        if (k == "id" || k == "photos" || v == null || v === JSONObject.NULL || v.toString().isEmpty()) continue
        if (k == "feedback" && merged.optJSONObject("feedback") != null && v is JSONObject) {
            val at = merged.getJSONObject("feedback").optLong("updatedAt", 0L)
            val bt = v.optLong("updatedAt", 0L)
            if (bt < at) continue
        }
        if (k == "health240" && merged.optJSONObject("health240") != null && v is JSONObject) {
            val at = runCatching { Instant.parse(merged.getJSONObject("health240").optString("syncedAt")).toEpochMilli() }.getOrDefault(0L)
            val bt = runCatching { Instant.parse(v.optString("syncedAt")).toEpochMilli() }.getOrDefault(0L)
            if (bt < at) continue
        }
        merged.put(k, v)
    }
    val aPhotos = a.optJSONArray("photos")
    val bPhotos = b.optJSONArray("photos")
    if (aPhotos != null || bPhotos != null) {
        val byId = LinkedHashMap<String, JSONObject>()
        for (arr in listOfNotNull(aPhotos, bPhotos)) {
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                val pid = p.optString("id")
                if (pid.isEmpty()) continue
                val old = byId[pid]
                if (old == null) {
                    byId[pid] = JSONObject(p.toString())
                } else {
                    val pUpd = p.optLong("updatedAt", 0L)
                    val oUpd = old.optLong("updatedAt", 0L)
                    val newer = if (pUpd >= oUpd) p else old
                    val combined = JSONObject(old.toString())
                    val pKeys = newer.keys()
                    while (pKeys.hasNext()) {
                        val pk = pKeys.next()
                        combined.put(pk, newer.get(pk))
                    }
                    val driveId = p.optString("driveFileId").ifEmpty { old.optString("driveFileId") }
                    if (driveId.isNotEmpty()) combined.put("driveFileId", driveId)
                    val delAt = listOf(p.optString("deletedAt"), old.optString("deletedAt"))
                        .filter { it.isNotEmpty() && it != "null" && it != "false" && it != "0" }
                        .maxOrNull()
                    if (delAt != null) combined.put("deletedAt", delAt)
                    combined.put("updatedAt", max(pUpd, oUpd))
                    byId[pid] = combined
                }
            }
        }
        val sortedPhotos = byId.values.sortedBy { it.optString("createdAt", "") }
        val photosOut = JSONArray()
        sortedPhotos.forEach { photosOut.put(it) }
        merged.put("photos", photosOut)
    }
    val realId = listOf(a.optString("id"), b.optString("id"))
        .firstOrNull { it.isNotEmpty() && !LegacySyncIdentity18.isSyntheticId("history", it) }
    merged.put("id", realId ?: LegacySyncIdentity18.legacyHistoryId(merged))
    return merged
}

private fun mergeHistoryRows18(
    localRows: JSONArray?,
    remoteSets: List<JSONArray?>,
    baseRows: JSONArray?,
    archiveSets: List<JSONArray?>,
    deletedIds: Set<String>,
    deletedKeys: Set<String>,
    preferLocal: Boolean
): JSONArray {
    val items = ArrayList<JSONObject>()
    fun isDeleted(x: JSONObject): Boolean {
        val id = x.optString("id")
        val legacy = LegacySyncIdentity18.legacyHistoryId(x)
        val key = x.optString("syncId").ifEmpty { x.optString("key").ifEmpty { x.optString("started") } }
        return (id.isNotEmpty() && id in deletedIds) ||
            (legacy in deletedIds) ||
            (key.isNotEmpty() && key in deletedKeys)
    }

    fun add(src: JSONObject?, archiveOnly: Boolean) {
        if (src == null) return
        val x = JSONObject(src.toString())
        if (isDeleted(x)) return
        val xId = x.optString("id")
        if (archiveOnly && xId.isNotEmpty()) return
        if (xId.isNotEmpty()) {
            val byId = items.indexOfFirst { it.optString("id") == xId }
            if (byId >= 0) {
                if (!archiveOnly) {
                    val mergedVariant = mergeHistoryVariant18(items[byId], x)
                    items[byId] = mergedVariant ?: if (preferLocal) items[byId] else x
                }
                return
            }
        }
        val sameIdx = items.indexOfFirst {
            LegacySyncIdentity18.workoutIdentity(it) == LegacySyncIdentity18.workoutIdentity(x)
        }
        if (sameIdx >= 0) {
            items[sameIdx] = mergeHistoryVariant18(items[sameIdx], x) ?: items[sameIdx]
            return
        }
        items.add(x)
    }

    if (localRows != null) for (i in 0 until localRows.length()) add(localRows.optJSONObject(i), false)
    for (rows in remoteSets) if (rows != null) for (i in 0 until rows.length()) add(rows.optJSONObject(i), false)
    if (baseRows != null) for (i in 0 until baseRows.length()) add(baseRows.optJSONObject(i), true)
    for (rows in archiveSets) if (rows != null) for (i in 0 until rows.length()) add(rows.optJSONObject(i), true)

    // Ensure unique legacy IDs
    val used = HashMap<String, String>()
    for (item in items) {
        val id = item.optString("id")
        if (id.isNotEmpty() && !LegacySyncIdentity18.isSyntheticId("history", id)) {
            used[id] = LegacySyncIdentity18.canonical(item)
        }
    }
    for (item in items) {
        val existingId = item.optString("id")
        if (existingId.isNotEmpty() && !LegacySyncIdentity18.isSyntheticId("history", existingId)) continue
        val root = LegacySyncIdentity18.legacyHistoryId(item)
        var id = root
        var n = 1
        val body = LegacySyncIdentity18.workoutIdentity(item)
        while (used.containsKey(id) && used[id] != body) {
            id = "$root-${++n}"
        }
        item.put("id", id)
        used[id] = body
    }
    items.sortByDescending { it.optString("started", "") }
    val out = JSONArray()
    items.forEach { out.put(it) }
    return out
}

private fun mergeWeightRows18(
    localRows: JSONArray?,
    remoteSets: List<JSONArray?>,
    baseRows: JSONArray?,
    archiveSets: List<JSONArray?>,
    preferLocal: Boolean,
    deletions: NativeWeightTombstones18.Index
): JSONArray {
    val items = ArrayList<JSONObject>()
    fun add(src: JSONObject?, archiveOnly: Boolean) {
        if (src == null) return
        if (deletions.deleted(src)) return
        val x = JSONObject(src.toString())
        val xId = x.optString("id")
        if (archiveOnly && xId.isNotEmpty()) return
        if (xId.isNotEmpty()) {
            val byId = items.indexOfFirst { it.optString("id") == xId }
            if (byId >= 0) {
                val chosen = if (!archiveOnly && !preferLocal) x else items[byId]
                items[byId] = mergeWeightPhotoMetadata(chosen, if (chosen === x) items[byId] else x)
                return
            }
        }
        val sameIdx = items.indexOfFirst {
            LegacySyncIdentity18.weightIdentity(it) == LegacySyncIdentity18.weightIdentity(x)
        }
        if (sameIdx >= 0) {
            val real = listOf(items[sameIdx].optString("id"), xId)
                .firstOrNull { it.isNotEmpty() && !LegacySyncIdentity18.isSyntheticId("weights", it) }
            val merged = if (preferLocal) items[sameIdx] else x
            mergeWeightPhotoMetadata(merged, if (merged === x) items[sameIdx] else x)
            merged.put("id", real ?: LegacySyncIdentity18.legacyWeightId(merged))
            items[sameIdx] = merged
            return
        }
        if (xId.isEmpty() || LegacySyncIdentity18.isSyntheticId("weights", xId)) {
            x.put("id", LegacySyncIdentity18.legacyWeightId(x))
        }
        items.add(x)
    }
    if (localRows != null) for (i in 0 until localRows.length()) add(localRows.optJSONObject(i), false)
    for (rows in remoteSets) if (rows != null) for (i in 0 until rows.length()) add(rows.optJSONObject(i), false)
    if (baseRows != null) for (i in 0 until baseRows.length()) add(baseRows.optJSONObject(i), true)
    for (rows in archiveSets) if (rows != null) for (i in 0 until rows.length()) add(rows.optJSONObject(i), true)
    items.sortByDescending { it.optString("date", "") }
    val out = JSONArray()
    items.forEach { out.put(it) }
    return out
}

private fun mergeWeightPhotoMetadata(primary: JSONObject, secondary: JSONObject): JSONObject {
   val photos = mergePhotoRows18(primary.optJSONArray("photos"), secondary.optJSONArray("photos"))
   if (photos.length() > 0) primary.put("photos", photos) else primary.remove("photos")
   return primary
}

private fun mergeLoosePhotoRows18(
   localRows: JSONArray?,
   remoteSets: List<JSONArray?>,
   baseRows: JSONArray?,
   archiveSets: List<JSONArray?>
): JSONArray {
   val rows = linkedMapOf<String, JSONObject>()
   fun add(source: JSONArray?) {
       if (source == null) return
       for (index in 0 until source.length()) {
           val incoming = source.optJSONObject(index) ?: throw IllegalArgumentException("Invalid loose photo.")
           val id = incoming.optString("id")
           require(id.isNotBlank()) { "Missing loose photo ID." }
           val existing = rows[id]
           rows[id] = if (existing == null) JSONObject(incoming.toString())
               else mergePhotoRows18(JSONArray().put(existing), JSONArray().put(incoming)).getJSONObject(0)
       }
   }
   add(localRows)
   remoteSets.forEach(::add)
   add(baseRows)
   archiveSets.forEach(::add)
   return JSONArray(rows.values.toList())
}

private fun mergePhotoRows18(first: JSONArray?, second: JSONArray?): JSONArray {
   val rows = linkedMapOf<String, JSONObject>()
   fun add(source: JSONArray?) {
       if (source == null) return
       for (index in 0 until source.length()) {
           val incoming = source.optJSONObject(index) ?: throw IllegalArgumentException("Invalid photo metadata.")
           val id = incoming.optString("id")
           require(id.isNotBlank()) { "Missing photo ID." }
           val existing = rows[id]
           if (existing == null) {
               rows[id] = JSONObject(incoming.toString())
           } else {
               val merged = JSONObject(existing.toString())
               val keys = incoming.keys()
               while (keys.hasNext()) {
                   val key = keys.next()
                   val value = incoming.get(key)
                   if (key == "driveFileId") {
                       if (!incoming.isNull(key) && value.toString().isNotBlank()) merged.put(key, value)
                   } else if (!merged.has(key) || merged.opt(key).toString().isBlank()) {
                       merged.put(key, value)
                   }
               }
               rows[id] = merged
           }
       }
   }
   add(first)
   add(second)
   return JSONArray(rows.values.toList())
}

private fun mergeScheduledRows18(
    localRows: JSONArray?,
    remoteSets: List<JSONArray?>,
    preferLocal: Boolean
): JSONArray {
    val map = LinkedHashMap<String, JSONObject>()
    if (localRows != null) {
        for (i in 0 until localRows.length()) {
            val x = localRows.optJSONObject(i) ?: continue
            val key = x.optString("id").ifEmpty { x.optString("scheduleId").ifEmpty { x.optString("start") } }
            if (key.isNotEmpty()) map[key] = JSONObject(x.toString())
        }
    }
    for (rows in remoteSets) {
        if (rows == null) continue
        for (i in 0 until rows.length()) {
            val x = rows.optJSONObject(i) ?: continue
            val key = x.optString("id").ifEmpty { x.optString("scheduleId").ifEmpty { x.optString("start") } }
            if (key.isEmpty()) continue
            val old = map[key]
            if (old == null) {
                map[key] = JSONObject(x.toString())
            } else {
                val oldUpd = old.optLong("updatedAt", 0L)
                val newUpd = x.optLong("updatedAt", 0L)
                map[key] = when {
                    oldUpd != newUpd -> if (oldUpd > newUpd) old else JSONObject(x.toString())
                    preferLocal -> old
                    else -> JSONObject(x.toString())
                }
            }
        }
    }
    val sorted = map.values.sortedByDescending { it.optString("start", "") }
    val out = JSONArray()
    sorted.forEach { out.put(it) }
    // Never let an older Drive snapshot resurrect the fixed 2026-11 calendar incident.
    return NativeCalendarCleanup202611.cleanScheduledJson(out, preserveCancellations = true)
}
