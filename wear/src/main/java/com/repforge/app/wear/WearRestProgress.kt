package com.repforge.app.wear

/** Each second consumes the same fraction of this rest phase, including pauses. */
internal fun wearRestProgress(workout: WearWorkout, remainingSeconds: Int): Float {
    val automatic=workout.automation
    val duration=if(automatic?.enabled==true&&automatic.phase=="rest")
        automatic.restPhaseDurationMs367.takeIf { it>0 }?.let { it/1000f } ?: automatic.restSeconds.toFloat()
        else workout.restSeconds.toFloat()
    return (remainingSeconds/duration.coerceAtLeast(1f)).coerceIn(0f,1f)
}
