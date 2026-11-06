package com.repforge.app

import com.example.model.StudioProgramPrescriptionDraft
import com.repforge.app.data.db.ProgramWithDays
import org.json.JSONObject
import org.json.JSONTokener

/** Adapts visible Studio edits to the existing atomic native editor, preserving unknown fields. */
internal object StudioProgramSave {
    fun draft(opened: ProgramWithDays, language: String, name: String,
        changes: List<StudioProgramPrescriptionDraft>): NativeProgramEditor.Draft {
        require(changes.map { it.dayKey to it.exerciseId }.distinct().size == changes.size)
        val edits = changes.groupBy { change ->
            opened.days.single { it.compositeId == change.dayKey || it.dayId == change.dayKey }.dayId
        }
        val original = NativeProgramEditor.from(opened, language)
        return original.copy(name = name.trim(), days = original.days.map { day ->
            val pending = edits[day.id].orEmpty().associateBy { it.exerciseId }
            check(pending.keys.all { id -> day.entries.any { NativeProgramEditor.exerciseId(it) == id } })
            day.copy(entries = day.entries.map { entry ->
                val id = NativeProgramEditor.exerciseId(entry)
                val edit = pending[id] ?: return@map entry
                require(edit.sets in 1..10 && edit.reps in 1..50 && edit.restSec in 15..300 &&
                    edit.weightKg.isFinite() && edit.weightKg in 0f..10000f)
                val value = JSONTokener(entry).nextValue()
                val row = if (value is JSONObject) JSONObject(value.toString()) else JSONObject().put("id", id)
                row.put("sets", edit.sets).put("reps", edit.reps.toString())
                    .put("weight", edit.weightKg.toDouble()).put("rest", edit.restSec).toString()
            })
        })
    }
}
