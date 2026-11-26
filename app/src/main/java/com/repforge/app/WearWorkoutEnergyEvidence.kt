package com.repforge.app

import org.json.JSONObject
import java.time.Instant

/** A live DataItem is not a durable final measurement. Keep the existing transport receipt
 * fence and legacy flat preview compatibility; never sum competing watch summaries. */
internal object WearWorkoutEnergyEvidence {
    fun live(frame: JSONObject, packet: JSONObject?, expectedNode: String?, now: Instant,
        allowLegacyPreview: Boolean = true): JSONObject? {
        packet ?: return null
        val id = frame.optString("workoutId")
        val stamp = packet.optLong("updatedAt")
        val start = runCatching { Instant.parse(frame.getString("started")).toEpochMilli() }.getOrNull() ?: return null
        if (!WearLiveHealthStore.valid(packet) || id.isBlank() || packet.optString("workoutId") != id ||
            stamp < start || stamp > now.toEpochMilli() || now.toEpochMilli() - stamp > 45_000 ||
            frame.optBoolean("nativeHealthWindowStale") || frame.optBoolean("discardPending")) return null
        val modern = packet.has("summary") || packet.has("ownerEpoch351") || packet.has("sourceNode351")
        if (!modern) return packet.takeIf { allowLegacyPreview }
        val receipt = PhoneMeasurementReconnect351.freshReceipt(packet,id,frame.optLong("ownerEpoch193",-1),
            expectedNode,now.toEpochMilli()) ?: return null
        if (receipt.optString("state") !in setOf("active","paused","ending","ended")) return null
        val summary = receipt.optJSONObject("summary") ?: return null
        if (summary.optString("workoutId",id) != id) return null
        val sampleStamp = summary.optLong("updatedAt",stamp)
        if (sampleStamp < start || sampleStamp > stamp + 5_000 || now.toEpochMilli() - sampleStamp > 45_000) return null
        return JSONObject(summary.toString()).put("workoutId",id).apply {
            if (receipt.optString("state") != "ended") put("partial",true)
            put("livePreview2815",true)
        }
    }

    /** Active duration is a real Health Services measurement, not raw calorie coverage.
     * An early stop/gap is enough to downgrade evidence; duration alone cannot prove full energy. */
    fun incompleteDuration(data: JSONObject, expectedMs: Long): Boolean {
        val duration = (data.opt("activeDurationSeconds") as? Number)?.toDouble() ?: return false
        return duration.isFinite() && duration >= 0 && duration * 1000 + 5_000 < expectedMs
    }

    fun accept(previous: JSONObject?, next: JSONObject, aliases: Set<String>): Boolean {
        val id = next.optString("workoutId")
        if (id.isBlank() || id !in aliases || (next.has("schema") && next.optInt("schema") != 1)) return false
        if (previous == null || previous.optString("workoutId").let { it.isNotBlank() && it !in aliases }) return true
        val oldWatch = previous.optString("watchId")
        val newWatch = next.optString("watchId")
        if (oldWatch.isNotBlank() && oldWatch != newWatch) return false
        val oldRevision = previous.optLong("revision")
        val newRevision = next.optLong("revision")
        if (oldRevision > 0 && newRevision <= oldRevision &&
            !(newRevision == oldRevision && previous.optBoolean("livePreview2815") && !next.optBoolean("livePreview2815"))) return false
        if (previous.optLong("updatedAt") > next.optLong("updatedAt")) return false
        val oldMetrics = NativeWorkoutHealth.metrics(previous,true)
        val newMetrics = NativeWorkoutHealth.metrics(next,true)
        // Do not replace an already measured energy or pulse with a status-only packet.
        if (!newMetrics.keys.containsAll(oldMetrics.keys)) return false
        return true
    }

    fun attach(frame: JSONObject, next: JSONObject?, aliases: Set<String>): Boolean {
        if (next == null || !accept(NativeSavedWearHealth.fromRaw(frame),next,aliases)) return false
        frame.put("wear",JSONObject(next.toString()))
        return true
    }

    /** Engine projections do not own this metadata. Keep it across same-generation draft
     * updates; never carry it across a different workout or a stale edited window. */
    fun preserveDraft(previous: JSONObject, next: JSONObject) {
        val id = next.optString("workoutId")
        if (id.isBlank() || id != previous.optString("workoutId") ||
            next.optLong("generation") != previous.optLong("generation") ||
            next.optBoolean("nativeHealthWindowStale") || previous.optBoolean("nativeHealthWindowStale")) return
        val old = NativeSavedWearHealth.fromRaw(previous) ?: return
        if (old.optString("workoutId") != id) return
        val incoming = NativeSavedWearHealth.fromRaw(next)
        if (incoming == null || !accept(old,incoming,setOf(id)))
            next.put("wear",JSONObject(old.toString()))
    }
}
