package com.repforge.app.wear

import android.content.Context

/** Local input protection only: never enters workout snapshots, timer state or the outbox. */
object WearTouchLock189 {
    private const val PREFS="trainpilot_wear_touch_lock189"
    fun identity(workout: WearWorkout?)=workout?.let { it.workoutId+"\n"+it.started }.orEmpty()
    fun locked(context: Context,workout: WearWorkout?): Boolean {
        val key=identity(workout)
        return key.isNotBlank()&&context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString("workout",null)==key
    }
    fun set(context: Context,workout: WearWorkout,locked: Boolean): Boolean {
        val edit=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
        return (if(locked)edit.putString("workout",identity(workout)) else edit.remove("workout")).commit()
    }
    fun retain(context: Context,workout: WearWorkout?) {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        if(prefs.contains("workout")&&prefs.getString("workout",null)!=identity(workout))prefs.edit().clear().commit()
    }
}

/** One uninterrupted three-second hold unlocks immediately; drags/multi-touch cancel. */
internal class WearUnlockGesture189 {
    companion object { const val HOLD_MS=3000L }
    private var downAt: Long?=null
    val holding get()=downAt!=null
    fun down(now: Long) { downAt=now }
    fun tryUnlock(now: Long): Boolean {
        val started=downAt ?: return false
        if(now-started<HOLD_MS)return false
        cancel()
        return true
    }
    fun up(now: Long): Boolean {
        val unlock=tryUnlock(now)
        cancel()
        return unlock
    }
    fun cancel() { downAt=null }
    fun progress(now: Long)=downAt?.let { ((now-it).toFloat()/HOLD_MS).coerceIn(0f,1f) } ?: 0f
}

/** #367 3.5: how far (dp) a finger may drift off the unlock button while it keeps holding. */
internal const val WEAR_UNLOCK_DRIFT_DP=16f

/** A hold survives finger roll and drift; it ends only when the finger really leaves the button area. */
internal fun wearUnlockHoldInside(x: Float,y: Float,width: Float,height: Float,margin: Float)=
    x in -margin..width+margin&&y in -margin..height+margin

/** Whole seconds still to hold (3, 2, 1, 0), shown while the lock is pressed. */
internal fun wearUnlockSecondsLeft(elapsedMs: Long): Int {
    val left=WearUnlockGesture189.HOLD_MS-elapsedMs.coerceAtLeast(0L)
    return ((left+999L)/1000L).toInt().coerceIn(0,(WearUnlockGesture189.HOLD_MS/1000L).toInt())
}

internal data class WearTouchCopy189(val lock: String,val locked: String,val hold: String,
    val work: String,val rest: String,val paused: String,val ready: String,val saved: String,val sec: String) {
    companion object {
        fun language(language: String)=when(language.substringBefore('-').substringBefore('_')) {
            "en"->WearTouchCopy189("Touch lock","Locked","Press and hold the lock for 3 seconds without letting go","Work","Rest","Paused","Ready","Recorded","s")
            "de"->WearTouchCopy189("Berührungssperre","Gesperrt","Schloss 3 Sekunden ununterbrochen gedrückt halten","Arbeit","Pause","Pausiert","Bereit","Erfasst","Sek.")
            "ro"->WearTouchCopy189("Blocare atingere","Blocat","Ține lacătul apăsat 3 secunde, fără să-l lași","Lucru","Pauză","În pauză","Pregătit","Înregistrat","s")
            else->WearTouchCopy189("Érintészár","Zárolva","Tartsd lenyomva a lakatot megszakítás nélkül 3 mp-ig","Munka","Pihenő","Szünet","Készen áll","Rögzítve","mp")
        }
    }
}
