package com.repforge.shared

import org.json.JSONObject

/** A transient request is not an ownership transfer or a persisted start command. */
data class WearMeasurementRequest351(val workoutId: String, val revision: Long, val epoch: Long, val sentAt: Long) {
    fun fresh(now: Long): Boolean = now-sentAt in -2000L..15000L

    fun matches(id: String, currentRevision: Long, currentEpoch: Long, now: Long): Boolean =
        workoutId==id && currentRevision>=revision && currentEpoch==epoch && fresh(now)

    fun encode(): ByteArray = JSONObject().put("schema",1).put("workoutId",workoutId)
        .put("revision",revision).put("epoch",epoch).put("sentAt",sentAt)
        .toString().toByteArray(Charsets.UTF_8)

    companion object {
        const val PATH="/trainpilot/measurement-start-351"
        fun fromFrame(frame: JSONObject, now: Long): WearMeasurementRequest351? =
            if(frame.optBoolean("active") && !frame.optBoolean("workoutPaused") &&
                !frame.optBoolean("finalizationPending") && !frame.optBoolean("discardPending") &&
                frame.optString("timerOwner","phone")=="phone" && !WorkoutOwnership193.pending(frame))
                validated(frame.optString("workoutId"),frame.optLong("revision"),frame.optLong("ownerEpoch193"),now)
            else null

        fun parse(bytes: ByteArray): WearMeasurementRequest351? {
            if(bytes.isEmpty() || bytes.size>4096)return null
            return try {
                val json=JSONObject(bytes.toString(Charsets.UTF_8))
                if(json.optInt("schema")!=1)null else validated(json.optString("workoutId"),
                    json.optLong("revision"),json.optLong("epoch",-1),json.optLong("sentAt"))
            } catch (_: Exception) { null }
        }

        private fun validated(id: String, revision: Long, epoch: Long, sentAt: Long): WearMeasurementRequest351? =
            if(id.isNotBlank() && id.length<=512 && revision>0 && epoch>=0 && sentAt>0)
                WearMeasurementRequest351(id,revision,epoch,sentAt) else null
    }
}
