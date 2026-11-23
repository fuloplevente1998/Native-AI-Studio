package com.repforge.app.ui.coach

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.*
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.security.MessageDigest
import java.time.*

/** Explicit previews for applying durable Coach recommendations. */
internal class NativeCoachVariation(private val context: Context, private val db: TrainPilotDatabase) {
    data class Preview(val target: CoachTarget, val exerciseId: String, val source: ProgramWithDays,
        val draft: NativeProgramEditor.Draft, val next: NativeCoachExerciseAdvice.Variation?,
        val recipe: String, val proof: String, val language: String,
        val weight: WeightChange? = null, val applyToCurrent: Boolean = false)
    data class WeightChange(val exerciseName: String, val before: Double, val after: Double, val loadType: String)

    suspend fun preview(target: CoachTarget, exerciseId: String, now: Instant = Instant.now(),
        applyToCurrent: Boolean = false): Preview =
        NativeDataReplacement.mutate(context) {
            NativeProgramEditor.assertNoActiveWorkout(context)
            val settings = TrainPilotSettingsStore(context).current()
            require(settings.activeProgramId == target.programId)
            val source = requireNotNull(db.programDao().getProgramWithDaysById(target.programId))
            require(!applyToCurrent || !source.program.builtin)
            val readiness = readiness(now)
            val repository = NativeCoachAdviceRepository(db)
            val row = repository.load(target, settings, readiness, now).single { it.id == exerciseId }
            require(isApplicable(row, readiness))
            val advice = row.advice
            val library = requireNotNull(db.exerciseLibraryDao().getExerciseById(exerciseId))
            val original = NativeProgramEditor.from(source, settings.language)
            if (advice.action != "variation") {
                require(applyToCurrent)
                val day = original.days.single { it.id == target.dayId }
                val entry = day.entries.single { NativeProgramEditor.exerciseId(it) == exerciseId }
                val effective = NativeProgramPrescription.effective(source.program, entry, library, emptyMap(),
                    library.defaultRestSeconds)
                val before = effective.getDouble("weight")
                val loadType = effective.getString("loadType")
                require(loadType == row.loadType && loadType != "bodyweight")
                require(if (advice.action == "increase") advice.weight > before else advice.weight < before)
                val draft = withDayWeight(original, target.dayId, exerciseId, advice.weight)
                val weight = WeightChange(row.name, before, advice.weight, loadType)
                val proof = fingerprint(source.toString(), settings.profileJson, settings.language, row.sourceFingerprint, target.dayId, weight.toString())
                return@mutate Preview(target, exerciseId, source, draft, null, "", proof, settings.language, weight, applyToCurrent = true)
            }
            val profile = NativeTrainingProfile.verified(JSONObject(settings.profileJson))
            require(profile.has("gear") && profile.has("minutes"))
            require(NativeProfileProgramPreview.variationRejection(library, profile).isEmpty())
            var next = requireNotNull(advice.variation)
            var proposed = requireNotNull(db.exerciseLibraryDao().getExerciseById(next.exerciseId))
            val reason = NativeProfileProgramPreview.variationRejection(proposed, profile)
            require(reason != "avoid-area")
            if (reason.isNotEmpty()) {
                next = NativeCoachExerciseAdvice.Variation(exerciseId, "paused", 1)
                proposed = library
            }
            if (next.name == "weighted") {
                val gear = profile.getJSONArray("gear")
                require((0 until gear.length()).any { gear.optString(it) in setOf("dumbbell", "dumbbells", "barbell") })
            }
            val recipe = recipe(next, JSONObject(proposed.rawJson).put("id", proposed.id).put("repUnit", proposed.repUnit).put("loadType", proposed.loadType), profile)
            val draft = original.copy(days = original.days.map { day ->
                if (applyToCurrent && day.id != target.dayId) return@map day
                val replaced = day.entries.map { encoded ->
                    if (NativeProgramEditor.exerciseId(encoded) != exerciseId) encoded else {
                        val raw = JSONTokener(encoded).nextValue() as? JSONObject
                        if (raw == null && !applyToCurrent) JSONObject.quote(next.exerciseId) else {
                            val item = (raw ?: JSONObject()).put("id", next.exerciseId)
                            for (key in recipe.keys()) if (key != "id") item.put(key, recipe.get(key))
                            item.toString()
                        }
                    }
                }
                require(replaced.map(NativeProgramEditor::exerciseId).distinct().size == replaced.size)
                day.copy(entries = replaced)
            })
            val program = JSONObject(source.program.rawJson)
            val prescriptions = program.optJSONObject("prescriptions") ?: JSONObject()
            val rx = prescriptions.optJSONObject(exerciseId) ?: JSONObject()
            val lib = JSONObject(library.rawJson).put("id", exerciseId).put("loadType", library.loadType)
                .put("repUnit", library.repUnit).put("custom", library.custom)
            val dayItems = JSONArray(source.days.first { it.dayId == target.dayId }.exercisesJson)
            for (i in 0 until dayItems.length()) {
                val entry = dayItems.optJSONObject(i) ?: continue
                if (entry.optString("id") == exerciseId) for (key in entry.keys()) if (key != "id") rx.put(key, entry.get(key))
            }
            val evidence = repository.recent(exerciseId, target.programId, lib, rx, program, now)
            val proof = fingerprint(source.toString(), settings.profileJson, settings.language,
                row.sourceFingerprint, proposed.toString(), evidence.joinToString { it.first.toString() }, recipe.toString())
            Preview(target, exerciseId, source, draft, next, recipe.toString(), proof, settings.language, applyToCurrent = applyToCurrent)
        }

    /** Revalidate the exact preview before publishing a single atomic change. */
    suspend fun save(opened: Preview): String = NativeDataReplacement.mutate(context) {
        db.withTransaction {
            val fresh = preview(opened.target, opened.exerciseId, applyToCurrent = opened.applyToCurrent)
            check(fresh.proof == opened.proof && fresh.draft == opened.draft &&
                fresh.next == opened.next && fresh.weight == opened.weight)
            val id = NativeProgramEditor.save(context, db, fresh.source, fresh.draft, fresh.language, clone = !fresh.applyToCurrent)
            val saved = requireNotNull(db.programDao().getProgramWithDaysById(id))
            val raw = JSONObject(saved.program.rawJson)
            if (fresh.applyToCurrent) {
                val original = fresh.source.program
                raw.put("generated", original.generated).put("sourceType", original.sourceType)
                db.programDao().saveProgramWithDays(saved.program.copy(generated = original.generated,
                    sourceType = original.sourceType, rawJson = raw.toString()), saved.days)
                return@withTransaction id
            }
            fresh.next?.let { next ->
                val recipes = raw.optJSONObject("prescriptions") ?: JSONObject()
                recipes.put(next.exerciseId, JSONObject(fresh.recipe))
                raw.put("prescriptions", recipes)
            }
            raw.put("generated", true).put("sourceType", "coach").put("parentProgramId", fresh.source.program.id)
            raw.optJSONObject("history128")?.optJSONObject("entries")?.remove(fresh.exerciseId)
            // REPLACE deletes the parent row and cascades to program_days. Republish the
            // existing days with the metadata in the same transaction instead of losing them.
            db.programDao().saveProgramWithDays(saved.program.copy(generated = true, sourceType = "coach", rawJson = raw.toString()), saved.days)
            id
        }
    }

    private suspend fun readiness(now: Instant): NativeCoachReadiness.Result =
        NativeSelectedCoachReadings.load(context, db, now, ZoneId.systemDefault()).readiness

    private fun fingerprint(vararg fields: String): String =
        MessageDigest.getInstance("SHA-256").digest(fields.joinToString("\n").toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        internal fun isApplicable(row: CoachExerciseAdvice, readiness: NativeCoachReadiness.Result?): Boolean {
            if (readiness?.pain == true || readiness?.mode in setOf("light", "reduced")) return false
            val advice = row.advice
            if (advice.action == "variation") return advice.variation != null && advice.historyCount >= 3
            return advice.action in setOf("increase", "reduce") && row.loadType != "bodyweight" &&
                advice.weight.isFinite() && advice.weight in .25..10_000.0 && advice.historyCount > 0 &&
                (advice.reason in setOf("stable", "declining") || advice.reason.startsWith("mode197:")) &&
                row.plannedWeight?.let { if (advice.action == "increase") advice.weight > it else advice.weight < it } == true
        }

        internal fun withDayWeight(original: NativeProgramEditor.Draft, dayId: String, exerciseId: String,
            weight: Double): NativeProgramEditor.Draft {
            require(weight.isFinite() && weight in .25..10_000.0)
            val day = original.days.single { it.id == dayId }
            require(day.entries.count { NativeProgramEditor.exerciseId(it) == exerciseId } == 1)
            return original.copy(days = original.days.map {
                if (it.id != dayId) it else it.copy(entries = it.entries.map { encoded ->
                    if (NativeProgramEditor.exerciseId(encoded) != exerciseId) encoded else {
                        val entry = JSONTokener(encoded).nextValue() as? JSONObject ?: JSONObject().put("id", exerciseId)
                        entry.put("weight", weight).put("trial", false).toString()
                    }
                })
            })
        }

        internal fun recipe(next: NativeCoachExerciseAdvice.Variation, library: JSONObject, profile: JSONObject): JSONObject {
            return NativeTrainingPrescription.create(JSONObject(library.toString()).put("id", next.exerciseId)
                .put("loadType", library.optString("loadType", "bodyweight")), profile)
                .put("variation", next.name).put("progressionLevel", next.level)
                .also { if (next.name == "weighted") it.put("loadType", "single_dumbbell") }
        }
    }
}
