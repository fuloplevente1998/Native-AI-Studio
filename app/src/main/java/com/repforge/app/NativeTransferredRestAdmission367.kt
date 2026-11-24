package com.repforge.app

import com.repforge.shared.IntervalTimeline193
import org.json.JSONObject

/** Validates a watch-owned transferred rest without applying a phone-side timer. */
internal object NativeTransferredRestAdmission367 {
    fun valid(incoming: JSONObject, command: JSONObject, basePause: Long): Boolean = runCatching {
        require(!incoming.optBoolean("workoutPaused") && incoming.optLong("pauseRevision")==basePause &&
            incoming.optLong("restEndAt")==0L)
        val proof=command.getJSONObject("transferRestProof367")
        val checkpoint=incoming.optJSONObject("timerCheckpoint193")
        val timer=checkpoint?.getJSONObject("timer") ?: proof.getJSONObject("terminalTimer")
        require(timer.optString("workoutId")==incoming.optString("workoutId") &&
            timer.optString("key")==proof.getString("sourceTimerKey"))
        val after=IntervalTimerService.validate(timer)
        val position=proof.getLong("position")
        val phase=after.at(position)
        require(position==phase.elapsedMs && position==timer.getLong("elapsedMs") &&
            phase.phase==timer.getString("phase") && phase.step==timer.getInt("stepIndex") &&
            phase.round==timer.getInt("round") && phase.remainingMs==timer.getLong("remainingMs") &&
            phase.totalMs==timer.getLong("totalMs") && phase.completed==timer.getInt("completed") &&
            phase.stepElapsedMs==timer.getLong("stepElapsedMs"))
        val key=proof.getString("restKey")
        val map=IntervalTimeline193.readRestOverrides(timer)
        require(map.containsKey(key))
        val previous=map.toMutableMap()
        if(proof.isNull("priorOverride"))previous.remove(key)
        else previous[key]=proof.getLong("priorOverride")
        val oldTimer=JSONObject(timer.toString()).put("restOverridesMs367",JSONObject(previous))
        val old=IntervalTimerService.validate(oldTimer)
        val before=old.at(position)
        require(old.restKey(before)==key && before.remainingMs==proof.getLong("beforeRemaining") &&
            before.remainingMs==command.getLong("remainingBefore") &&
            old.phaseDurationMs(before)==proof.getLong("beforePhaseDuration") &&
            old.adjustedRestOverrides(position,command.getInt("delta"))==map)
        val row=timer.getJSONArray("steps").getJSONObject(before.step)
        require(row.optString("exerciseId")==command.optString("exerciseId") &&
            row.optString("setId")==command.optString("setId") &&
            row.optInt("rest")==command.optInt("configuredRest",-1))
        val ledger=timer.getJSONObject("ledger")
        require(ledger.getLong("positionMs")==position &&
            ledger.getLong("workMs")>=0 && ledger.getLong("restMs")>=0 &&
            ledger.getLong("workMs")+ledger.getLong("restMs")<=position)
        val a=incoming.getJSONObject("automation")
        require(a.optString("owner")=="watch" && a.optString("key")==command.optString("timerKey") &&
            !a.optBoolean("paused") && !a.optBoolean("interrupted") &&
            timer.getLong("restChangeRevision367")==proof.getLong("restRevision") &&
            a.optLong("restChangeRevision367")==proof.getLong("restRevision"))
        val basePhase=command.getLong("phaseNumber")
        if(checkpoint!=null) {
            require(phase.phase!="complete" && a.optString("phase")==phase.phase &&
                a.optLong("remainingMs")==phase.remainingMs &&
                a.optLong("phaseNumber")==basePhase+if(phase.token!=before.token)1 else 0)
        } else require(phase.phase=="complete" && a.optLong("phaseNumber")>=basePhase+1)
        true
    }.getOrDefault(false)
}
