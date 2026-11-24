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
)
internal fun wearPauseTarget367(workout: WearWorkout, allowPaused: Boolean = false): WearPauseTarget367? =
    if (workout.workoutId.isBlank() || (!allowPaused && workout.workoutPaused) || workout.controlIntent367 != null || workout.ownershipProtocol193 != 1 ||
        workout.timerOwner !in setOf("watch", "phone") || WearOwnership193.pending(workout) || WearUndo188.blocked(workout)) null
    else WearPauseTarget367(workout.workoutId, workout.generation, workout.ownerEpoch193,
        workout.pauseRevision, workout.timerOwner, workout.revision, workout.workoutPaused)

internal fun wearRestTarget367(workout: WearWorkout, now: Long): WearRestTarget367? {
    val target = wearPauseTarget367(workout) ?: return null
    if (workout.restEndAt <= now || workout.automation?.enabled == true) return null
    val exercise = workout.exercise ?: return null
    val set = workout.currentSet ?: return null
    if (exercise.id.isBlank() || set.setId.isBlank()) return null
    return WearRestTarget367(target, workout.restEndAt, exercise.id, set.setId)
}

internal fun wearRestAdjustedSeconds367(remaining: Int, delta: Int): Int? =
    if (remaining !in 1..WEAR_REST_MAX367 || delta !in listOf(-WEAR_REST_STEP367, WEAR_REST_STEP367)) null
    else (remaining + delta).coerceIn(0, WEAR_REST_MAX367)
