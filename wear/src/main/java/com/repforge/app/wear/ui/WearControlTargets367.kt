package com.repforge.app.wear

internal const val WEAR_REST_STEP367 = 15
internal const val WEAR_REST_MAX367 = 600

/** UI target only: not a wire command and not permission to mutate the timer. */
internal data class WearPauseTarget367(
    val workoutId: String, val generation: Long, val ownerEpoch: Long,
    val pauseRevision: Long, val timerOwner: String, val baseRevision: Long,
    val paused: Boolean = false,
)
internal data class WearRestTarget367(
    val workout: WearPauseTarget367, val expectedDeadline: Long,
    val exerciseId: String, val setId: String,
    val timerKey: String = "", val phaseNumber: Long = 0, val duration: Int = 0,
    val phaseDurationMs: Long = 0, val restRevision: Long = 0,
)
internal fun wearPauseTarget367(workout: WearWorkout, allowPaused: Boolean = false): WearPauseTarget367? =
    if (workout.workoutId.isBlank() || (!allowPaused && workout.workoutPaused) || workout.controlIntent367 != null || workout.ownershipProtocol193 != 1 ||
        workout.timerOwner !in setOf("watch", "phone") || WearOwnership193.pending(workout) || WearUndo188.blocked(workout)) null
    else WearPauseTarget367(workout.workoutId, workout.generation, workout.ownerEpoch193,
        workout.pauseRevision, workout.timerOwner, workout.revision, workout.workoutPaused)

internal fun wearRestTarget367(workout: WearWorkout, now: Long): WearRestTarget367? {
    val target = wearPauseTarget367(workout) ?: return null
    if (workout.timerOwner == "phone") {
        val a = workout.automation ?: return null
        if (workout.nativeExecutionProtocol != 2 || (!a.restOnly && a.restPhaseDurationMs367 <= 0) || !a.enabled || a.owner != "phone" ||
            a.phase != "rest" || a.paused || a.interrupted || a.awaitingPhone || a.key.isBlank() || a.deadlineAt <= now) return null
        return WearRestTarget367(target, a.deadlineAt, "", "", a.key, a.phaseNumber, a.restSeconds,
            a.restPhaseDurationMs367, a.restChangeRevision367)
    }
    val automatic = workout.automation
    if (automatic?.enabled == true) {
        if (automatic.owner != "watch" || automatic.phase != "rest" ||
            automatic.paused || automatic.interrupted || automatic.awaitingPhone || automatic.key.isBlank() ||
            automatic.targetId.isBlank()) return null
        val ex = workout.exercises.getOrNull(automatic.targetExercise) ?: return null
        return WearRestTarget367(target, automatic.anchorMs + automatic.remainingMs, ex.id,
            automatic.targetId, automatic.key, automatic.phaseNumber, automatic.restSeconds,
            automatic.restPhaseDurationMs367, automatic.restChangeRevision367)
    }
    if (workout.restEndAt <= now) return null
    val exercise = workout.exercise ?: return null
    val set = workout.currentSet ?: return null
    if (exercise.id.isBlank() || set.setId.isBlank()) return null
    return WearRestTarget367(target, workout.restEndAt, exercise.id, set.setId)
}

internal fun wearRestAdjustedSeconds367(remaining: Int, delta: Int): Int? =
    if (remaining !in 1..WEAR_REST_MAX367 || delta !in listOf(-WEAR_REST_STEP367, WEAR_REST_STEP367)) null
    else (remaining + delta).coerceIn(0, WEAR_REST_MAX367)
