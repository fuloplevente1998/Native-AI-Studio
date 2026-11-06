package com.repforge.app

import com.repforge.shared.WorkoutOwnership193
import org.json.JSONObject
import java.time.Instant

/** A finish request never grants the watch authority to replace a phone-owned draft. */
internal object NativeWatchFinishAdmission {
    private fun sameSession(first: JSONObject, second: JSONObject): Boolean = runCatching {
        first.getString("workoutId").isNotBlank() && first.getString("workoutId") == second.getString("workoutId") &&
            Instant.parse(first.getString("started")) == Instant.parse(second.getString("started")) &&
            first.optLong("ownerEpoch193", -1) >= 0 &&
            first.optLong("ownerEpoch193", -1) == second.optLong("ownerEpoch193", -1) &&
            first.optLong("generation") == second.optLong("generation")
    }.getOrDefault(false)

    fun isCurrentWatchSnapshot(input: JSONObject, current: JSONObject?): Boolean =
        current != null && sameSession(input, current) &&
            input.optInt("ownershipProtocol193") == 1 && current.optInt("ownershipProtocol193") == 1 &&
            WorkoutOwnership193.owns(input, "watch") && WorkoutOwnership193.owns(current, "watch") &&
            !current.optBoolean("discardPending")

    fun canFinalizeSuspendedPhoneFrame(input: JSONObject, current: JSONObject?, execution: JSONObject): Boolean =
        isCurrentWatchSnapshot(input, current) && execution.optBoolean("active") &&
            current != null &&
            execution.optString("workoutId") == current.optString("workoutId") &&
            input.optLong("generation", -1) > 0 && input.optLong("generation") == execution.optLong("generation", -1)

    fun isPhoneOwnedRequest(command: JSONObject, current: JSONObject?): Boolean {
        val snapshot = command.optJSONObject("finalSnapshot") ?: return false
        return current != null && command.optString("action") == "finishWorkout" && command.optInt("schema") == 1 &&
            command.optBoolean("confirmed") && command.optString("workoutId") == current.optString("workoutId") &&
            command.optLong("ownerEpoch193", -1) == current.optLong("ownerEpoch193", -1) &&
            snapshot.optInt("ownershipProtocol193") == 1 && current.optInt("ownershipProtocol193") == 1 &&
            sameSession(snapshot, current) && WorkoutOwnership193.owns(snapshot, "phone") &&
            WorkoutOwnership193.owns(current, "phone") && !current.optBoolean("discardPending") &&
            snapshot.optLong("generation", -1) > 0 && snapshot.optLong("generation") == current.optLong("generation", -1)
    }

    /** A lost delivery ACK may be replayed from Room truth, without reimporting saved sets. */
    fun isSavedRetry(command: JSONObject, stored: JSONObject, current: JSONObject?, execution: JSONObject?): Boolean {
        val snapshot = command.optJSONObject("finalSnapshot") ?: return false
        val id = command.optString("workoutId")
        fun stillOpen(frame: JSONObject?) = frame != null && frame.optString("workoutId") == id &&
            (frame.optBoolean("active") || frame.optBoolean("finalizationPending"))
        return command.optString("action") == "finishWorkout" && command.optInt("schema") == 1 &&
            command.optBoolean("confirmed") && command.optLong("sequence") > 0 &&
            id == stored.optString("workoutId") && command.optLong("ownerEpoch193", -1) == stored.optLong("ownerEpoch193", -1) &&
            sameSession(snapshot, stored) &&
            stored.optString("finished").isNotBlank() && !stillOpen(current) && !stillOpen(execution)
    }
}
