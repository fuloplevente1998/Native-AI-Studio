package com.repforge.app.wear

import java.util.Locale
import org.json.JSONObject
import kotlin.math.min

/**
 * #367 phase 2 (3.1): one primary workout action in one fixed slot. Strength, timed, automatic
 * and outdoor screens share this decision, so the first, a middle and the last exercise look and
 * behave the same. Previous/next/finish-early/discard stay in the touch control overlay.
 */
internal enum class WearFooterKind { RECORD, SKIP_REST, RESUME_AUTO, NEXT, FINISH, WAIT_SYNC, WAIT_PHONE }

internal data class WearFooterState(val kind: WearFooterKind, val enabled: Boolean)

internal fun wearFooterState(
    exerciseComplete: Boolean,
    lastExercise: Boolean,
    completedSets: Int,
    restRemaining: Int,
    canRecord: Boolean,
    syncBlocked: Boolean = false,
    awaitingPhone: Boolean = false,
    autoPausedWithCommand: Boolean = false,
): WearFooterState = when {
    syncBlocked -> WearFooterState(WearFooterKind.WAIT_SYNC, false)
    awaitingPhone -> WearFooterState(WearFooterKind.WAIT_PHONE, false)
    autoPausedWithCommand -> WearFooterState(WearFooterKind.RESUME_AUTO, true)
    restRemaining > 0 -> WearFooterState(WearFooterKind.SKIP_REST, true)
    exerciseComplete && !lastExercise -> WearFooterState(WearFooterKind.NEXT, true)
    // Finishing is never offered before a set is recorded and never while a sync conflict is open.
    exerciseComplete -> WearFooterState(WearFooterKind.FINISH, completedSets > 0)
    else -> WearFooterState(WearFooterKind.RECORD, canRecord)
}

/** Previous performance is an extra line: it yields before the heart rate or the action do. */
internal const val WEAR_PREVIOUS_ROW_MIN_BODY_DP = 78f

internal fun wearShowPreviousRow(bodyHeightDp: Float, fontScale: Float): Boolean =
    bodyHeightDp >= WEAR_PREVIOUS_ROW_MIN_BODY_DP * min(fontScale.coerceAtLeast(1f), 1.35f)

/**
 * 3.3: heart rate of the running workout. A live sample is shown normally; the last real sample
 * of this workout stays visible, dimmed, for [WEAR_PULSE_FRESH_MS] (paused or reconnecting
 * measurement). Anything else is missing: the UI then shows "–", never 0 or an old value.
 */
internal data class WearWorkoutPulse(val bpm: Int?, val live: Boolean)

internal fun wearWorkoutPulse(data: JSONObject?, workoutId: String?, now: Long): WearWorkoutPulse {
    val live = wearHomeLivePulse(data, workoutId, true, now)
    if (live != null) return WearWorkoutPulse(live, true)
    if (data == null || workoutId.isNullOrBlank() || data.optString("workoutId") != workoutId ||
        data.optString("source") != "wear_health_services" ||
        data.optString("state") in listOf("ended", "error") ||
        data.optString("heartRateAvailability") in listOf("acquiring", "unavailable")) return WearWorkoutPulse(null, false)
    val sample = data.optLong("lastSampleAt", 0L)
    if (sample <= 0L || now - sample !in -WEAR_PULSE_SKEW_MS..WEAR_PULSE_FRESH_MS) return WearWorkoutPulse(null, false)
    val pulse = if (data.has("heartRate") && !data.isNull("heartRate")) data.optDouble("heartRate") else Double.NaN
    return WearWorkoutPulse(pulse.takeIf { it.isFinite() && it in 20.0..250.0 }?.toInt(), false)
}

/** Outdoor/steps line without the heart rate (that has its own fixed row). */
internal fun wearLiveMovementRow(data: JSONObject?, workoutId: String, language: String = Locale.getDefault().language): String? {
    if (data == null || workoutId.isBlank() || data.optString("workoutId") != workoutId ||
        data.optString("source") != "wear_health_services") return null
    fun value(key: String, unit: String): String? {
        val number = if (data.has(key) && !data.isNull(key)) data.optDouble(key) else Double.NaN
        return if (!number.isFinite() || number < 0.0) null else "${trimWearNumber(number)} $unit"
    }
    return listOfNotNull(value("steps", wearText("lépés", language)), value("distanceMeters", "m"))
        .takeIf { it.isNotEmpty() }?.joinToString(" · ")?.let { wearLiveMovementLabel(it, language) }
}

/** 3.6: the stopwatch face never changes size or place between start, pause, resume and stop. */
internal const val WEAR_TIMER_SP = 28f
internal const val WEAR_TIMER_LINE_SP = 32f
internal const val WEAR_TIMER_BOX_DP = 44f
internal const val WEAR_TIMER_GLYPH_DP = 20f
internal const val WEAR_TIMER_MAX_FONT_SCALE = 1.15f

internal enum class WearTimerGlyph { PLAY, PAUSE, DONE }

internal fun wearTimerGlyph(done: Boolean, running: Boolean): WearTimerGlyph = when {
    done -> WearTimerGlyph.DONE
    running -> WearTimerGlyph.PAUSE
    else -> WearTimerGlyph.PLAY
}

/** The value to give `.sp`: always [WEAR_TIMER_SP] times at most [WEAR_TIMER_MAX_FONT_SCALE]. */
internal fun wearTimerFontSp(fontScale: Float): Float =
    WEAR_TIMER_SP * min(fontScale, WEAR_TIMER_MAX_FONT_SCALE) / fontScale
