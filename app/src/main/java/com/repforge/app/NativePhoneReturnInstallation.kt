package com.repforge.app

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/** Complete the local half of an acknowledged native ownership return. Never forge ownership. */
internal object NativePhoneReturnInstallation {
    fun requiresInstallation(frame: JSONObject?): Boolean {
        val handoff = frame?.optJSONObject("handoff193") ?: return false
        if (!frame.optBoolean("active") || frame.optString("timerOwner") != "phone" ||
            handoff.optString("source") != "watch" || handoff.optString("target") != "phone" ||
            handoff.optString("state") != "committed") return false
        val marker = frame.optJSONObject("ownershipImport193")
        return marker == null || marker.optString("id") != handoff.optString("id") ||
            marker.optLong("epoch") != frame.optLong("ownerEpoch193") || marker.optString("state") != "installed" ||
            frame.optInt("nativeEditorProjection") != 1
    }

    suspend fun complete(
        context: Context,
        arm: suspend (JSONObject) -> Unit = { frame ->
            ContextCompat.startForegroundService(context, Intent(context, IntervalTimerService::class.java).apply {
                action = IntervalTimerService.ARM
                putExtra("workoutId", frame.getString("workoutId"))
                putExtra("generation", frame.getLong("generation"))
                putExtra("language", frame.optString("language", "hu"))
            })
            withTimeout(5_000) {
                while (!IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation"))) delay(50)
            }
        }
    ): JSONObject = withContext(Dispatchers.Main.immediate) {
        val current = checkNotNull(ActiveWorkoutStore.current(context))
        check(NativeWatchHandoffReceipt.confirmed(current, PhoneOwnership193.journal(context).read(),
            current.getString("workoutId"), "phone")) { "Még nincs igazolt órás visszaadás; az edzés zárolva marad." }
        if (!requiresInstallation(current) && !PhoneWorkoutExecution.store(context).commandsSuspended())
            return@withContext NativeWorkoutEditing.project(context, PhoneWorkoutExecution.display(context))
        val frame = PhoneOwnership193.prepareResume(context).getJSONObject("snapshot")
        // ARM creates only the foreground lifetime. installResume restores the paused timer;
        // explicit user Resume remains necessary. A failed ARM keeps the durable fence closed.
        if (frame.optJSONObject("transition")?.optJSONObject("plan") != null &&
            !IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation"))) arm(frame)
        PhoneOwnership193.installResume(context, frame.getJSONObject("ownershipImport193").getString("id"),
            frame.getLong("ownerEpoch193"))
        NativeWorkoutEditing.project(context, PhoneWorkoutExecution.display(context)).also { frame ->
            // Local readiness remains durable if Data Layer is temporarily unavailable.
            runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(context, frame) }
        }
    }
}
