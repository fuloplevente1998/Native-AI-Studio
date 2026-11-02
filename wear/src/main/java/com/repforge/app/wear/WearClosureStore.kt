package com.repforge.app.wear

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local closure survives offline delivery. "saved" only follows the phone's receipt. */
data class WearClosure(
    val commandId: String,
    val workoutId: String,
    val action: String,
    val status: String,
    val message: String,
    val durationSeconds: Int,
    val exerciseCount: Int,
    val completedSets: Int,
    val started: String = "",
    val needsFeedback: Boolean = false
)

/** Only a matching terminal receipt proves closure; rejections must not look like endless delivery. */
internal fun wearClosureReceipt(local: JSONObject, receipt: JSONObject): JSONObject? {
    if (receipt.optInt("schema") != 1 || receipt.optLong("sequence") <= 0 ||
        listOf("commandId", "workoutId", "action").any { local.optString(it).isBlank() || local.optString(it) != receipt.optString(it) }) return null
    // results191 already passed the outbox's full sequence/identity validation.
    // The closure command was persisted before enqueue assigned its sequence.
    val status = receipt.optString("status")
    val expected = if (local.optString("action") == "discardWorkout") "discarded" else "saved"
    if (status !in setOf("saved", "discarded", "error", "superseded", "processed", "applied")) return null
    return JSONObject(local.toString()).put("status", if (status == expected) status else "error")
        .put("message", if (status == expected) "" else receipt.optString("message").ifBlank {
            if (status == "superseded") "A telefon ezt a lezárást már nem aktuálisnak jelölte. Ellenőrizd az állapotot, majd próbáld újra."
            else "A telefon nem igazolta a lezárást. Az órán megőrzött kérés újrapróbálható."
        })
}

internal fun wearClosureDelivery(local: WearClosure?, delivery: WearSaveSyncState?): WearClosure? {
    if (local == null || local.status != "pending") return local
    if (delivery != null && delivery.workoutId != local.workoutId) return local
    if (delivery == null || delivery.phase in setOf(WearSavePhase.ERROR, WearSavePhase.SYNCED))
        return local.copy(status = "error", message = when (delivery?.reason) {
            "timeout" -> "Nem érkezett telefonos visszaigazolás. A lezárási kérés újraküldhető."
            "transport" -> "A lezárási kérés elküldése nem sikerült. Próbáld újra."
            else -> "A lezárás még nincs visszaigazolva. Az órán megőrzött kérés újrapróbálható."
        })
    return local
}

object WearClosureStore {
    private const val PREFS = "trainpilot_wear_closure"

    @Synchronized fun persist(context: Context,workoutId: String): Boolean {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val raw=prefs.getString("result",null) ?: return false
        if(JSONObject(raw).optString("workoutId")!=workoutId)return false
        return prefs.edit().putString("result",raw).putString("closed",prefs.getString("closed","[]")).commit()
    }

    @Synchronized
    fun close(context: Context, workout: WearWorkout, command: JSONObject): WearClosure {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val closed = JSONArray(prefs.getString("closed", "[]"))
        closed.put(workout.workoutId)
        while (closed.length() > 128) closed.remove(0)
        val result = JSONObject()
            .put("commandId", command.optString("commandId"))
            .put("workoutId", workout.workoutId)
            .put("action", command.optString("action"))
            .put("status", "pending")
            .put("durationSeconds", workout.elapsedSeconds())
            .put("exerciseCount", workout.exercises.count { ex -> ex.sets.any { it.done } })
            .put("completedSets", workout.exercises.sumOf { ex -> ex.sets.count { it.done } })
            .put("started", workout.started)
            .put("needsFeedback", command.optString("action") == "finishWorkout" && !command.has("feedback"))
            .put("command", command)
        val beforeClosed=prefs.getString("closed","[]");val beforeResult=prefs.getString("result",null)
        if(!prefs.edit().putString("closed", closed.toString()).putString("result", result.toString()).commit()) {
            prefs.edit().putString("closed",beforeClosed).putString("result",beforeResult).commit()
            return parse(result.put("status","error").put("message","A helyi mentés nem sikerült."))
        }
        return parse(result)
    }

    fun isClosed(context: Context, workoutId: String): Boolean {
        val closed = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("closed", "[]"))
        return (0 until closed.length()).any { closed.optString(it) == workoutId }
    }
    @Synchronized fun rememberClosed193(context: Context,workoutId: String) {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val closed=JSONArray(prefs.getString("closed","[]"))
        if((0 until closed.length()).none { closed.optString(it)==workoutId })closed.put(workoutId)
        while(closed.length()>128)closed.remove(0)
        if(!prefs.edit().putString("closed",closed.toString()).commit())error("Workout closure could not be saved")
    }

    @Synchronized
    fun acceptResult(context: Context, result: JSONObject?) {
        result ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val local = prefs.getString("result", null)?.let(::JSONObject) ?: return
        if (local.optString("commandId") != result.optString("commandId") ||
            local.optString("workoutId") != result.optString("workoutId")) return
        local.put("status", result.optString("status", "pending"))
        local.put("message", result.optString("message"))
        if (result.optBoolean("feedbackAnswered")) local.put("needsFeedback", false)
        prefs.edit().putString("result", local.toString()).apply()
    }

    fun load(context: Context): WearClosure? {
        val local=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString("result",null)?.let(::JSONObject) ?: return null
        // The outbox receipt and command removal commit atomically. Reconstruct
        // closure status from that proof after a crash between preference stores.
        val receipts=try { JSONArray(context.getSharedPreferences("trainpilot_wear_outbox",Context.MODE_PRIVATE).getString("results191","[]")) }
            catch(_:Exception) { return parse(local) }
        for(i in 0 until receipts.length())receipts.optJSONObject(i)?.let {
            wearClosureReceipt(local, it)?.let { reconciled ->
                local.put("status", reconciled.getString("status")).put("message", reconciled.optString("message"))
            }
        }
        return parse(local)
    }

    @Synchronized
    fun answer(context: Context, workoutId: String, feedback: JSONObject) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val local = prefs.getString("result", null)?.let(::JSONObject) ?: return
        if (local.optString("workoutId") != workoutId) return
        local.put("needsFeedback", false)
        // A later explicit retry of the terminal command carries the answer too.
        local.optJSONObject("command")?.put("feedback", feedback)
        prefs.edit().putString("result", local.toString()).commit()
    }

    @Synchronized
    fun retry(context: Context): JSONObject? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val local = prefs.getString("result", null)?.let(::JSONObject) ?: return null
        val command = local.optJSONObject("command") ?: return null
        val id = java.util.UUID.randomUUID().toString()
        command.put("commandId", id).put("createdAt", System.currentTimeMillis())
        local.put("commandId", id).put("status", "pending").put("message", "").put("command", command)
        prefs.edit().putString("result", local.toString()).commit()
        return command
    }

    @Synchronized
    fun dismiss(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("result").apply()
    }

    private fun parse(value: JSONObject) = WearClosure(
        commandId = value.optString("commandId"), workoutId = value.optString("workoutId"),
        action = value.optString("action"), status = value.optString("status", "pending"),
        message = value.optString("message"), durationSeconds = value.optInt("durationSeconds"),
        exerciseCount = value.optInt("exerciseCount"), completedSets = value.optInt("completedSets"),
        started = value.optString("started"), needsFeedback = value.optBoolean("needsFeedback")
    )
}
