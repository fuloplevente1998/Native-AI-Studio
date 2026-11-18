package com.example

import com.example.model.ExerciseItem
import com.example.model.ProgramDayModel
import com.example.model.StudioStarterPlan
import com.example.model.TrainingProgramModel
import com.example.model.suggestGentlerStarterPlan
import com.repforge.app.NativePlannerPolicy
import com.repforge.app.NativeProfileHistory128
import com.repforge.app.NativeProgramPrescription
import com.repforge.app.data.db.NativeBuiltinCatalog
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.WorkoutHistoryEntity
import com.repforge.app.healthreadings.ReadingState
import com.repforge.app.healthreadings.RecoveryDayReading
import com.repforge.app.healthreadings.RecoverySource
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StarterTemplateAuditRegressionTest {
    private val catalog = NativeBuiltinCatalog.exercises.map {
        ExerciseItem(it.id, it.nameHu, defaultSets = it.defaultSets, defaultWeightKg = it.defaultWeight.toFloat(),
            defaultRestSec = it.defaultRestSeconds, loadType = it.loadType, repUnit = it.repUnit,
            measurementType = it.measurementType)
    }
    private val templates = NativeBuiltinCatalog.programs.let { (programs, days) ->
        val byId = catalog.associateBy { it.id }
        programs.map { program ->
            TrainingProgramModel(program.id, program.nameHu, program.nameEn, "", "", emptyList(), false, false,
                days.filter { it.programId == program.id }.sortedBy { it.sortOrder }.map { day ->
                    val ids = JSONArray(day.exercisesJson)
                    ProgramDayModel(day.dayId, day.dayId, day.nameHu, day.nameEn, 60,
                        (0 until ids.length()).map { byId.getValue(ids.getString(it)) })
                })
        }
    }

    @Test fun gymRecommendationUsesExistingFullBodyAndExclusionAdaptsTheSameTemplate() {
        val gym = StudioStarterPlan(setOf("machines"), true, "Edzőterem", "Kezdő", 60, emptySet(), emptySet())
        assertEquals("gym-fullbody", gym.recommend(templates, "Izomépítés", 3)?.id)
        val excluded = StudioStarterPlan(setOf("machines"), true, "Edzőterem", "Kezdő", 60, emptySet(),
            setOf("leg-press", "lat-pulldown"))
        assertNull(excluded.recommend(templates, "Izomépítés", 3))
        val draft = requireNotNull(excluded.buildFromTemplates(catalog, templates, "Izomépítés", 3))
        assertEquals("gym-fullbody", draft.sourceId)
        val day = draft.days.single()
        assertTrue(day.any { it.id == "machine-chest-press" })
        assertTrue(day.any { NativePlannerPolicy.exerciseMetadata(it.id).optString("movementPattern") in
            setOf("horizontal-pull", "vertical-pull") })
        assertTrue(day.none { it.id in setOf("leg-press", "lat-pulldown") })
        assertTrue(day.all { it.defaultWeightKg == 0f && excluded.eligible(it) })
    }

    @Test fun outdoorAndMixedProfilesUseDifferentAppropriateTemplates() {
        val outdoor = StudioStarterPlan(setOf("machines", "pullupbar", "low-bar"), true, "Szabadtér / Street workout",
            "Középhaladó", 60, emptySet(), emptySet())
        val draft = requireNotNull(outdoor.buildFromTemplates(catalog, templates, "Izomépítés", 3))
        assertEquals("calisthenics-ab", draft.sourceId)
        assertTrue(draft.days.all { day -> day.any { it.id in setOf("bodyweight-squat", "prisoner-squat") } })
        assertTrue(draft.days.flatten().none { "machines" in NativePlannerPolicy.equipmentNeeds(it.id).orEmpty() })
        val mixed = StudioStarterPlan(setOf("machines"), true, "Vegyes (Otthon + Terem)", "Kezdő",
            60, emptySet(), emptySet())
        assertEquals("gym-fullbody", mixed.buildFromTemplates(catalog, templates, "Izomépítés", 3)?.sourceId)
    }

    @Test fun strengthChoiceRespectsWeeklyDaysAndRack() {
        val gear = setOf("machines", "barbell", "rack", "lifting-bench", "bench", "incline-bench")
        val ready = StudioStarterPlan(gear, true, "Edzőterem", "Haladó", 90, emptySet(), emptySet())
        assertEquals("gym-strength-abc", ready.buildFromTemplates(catalog, templates, "Erőnövelés", 3)?.sourceId)
        assertNotEquals("gym-strength-abc", ready.buildFromTemplates(catalog, templates, "Erőnövelés", 2)?.sourceId)
        val limited = StudioStarterPlan(gear - "rack", true, "Edzőterem", "Haladó", 90, emptySet(), emptySet())
        assertFalse(limited.eligible(catalog.first { it.id == "barbell-back-squat" }))
        assertFalse(limited.eligible(catalog.first { it.id == "bench-press" }))
    }

    @Test fun aBandAloneOrAHighPullupBarDoesNotCountAsAnAnchorOrLowBar() {
        val equipment = setOf("bands", "pullupbar")
        val p = StudioStarterPlan(equipment, true, "Szabadtér / Street workout", "Középhaladó",
            60, emptySet(), emptySet())
        val row = catalog.first { it.id == "band-row" }
        val inverted = catalog.first { it.id == "inverted-row" }
        assertFalse(p.eligible(row))
        assertFalse(p.eligible(inverted))
        val withAnchors = StudioStarterPlan(equipment + setOf("band-anchor", "low-bar"), true,
            "Szabadtér / Street workout", "Középhaladó", 60, emptySet(), emptySet())
        assertTrue(withAnchors.eligible(row))
        assertTrue(withAnchors.eligible(inverted))
    }

    @Test fun stockUpdateIsIdempotentAndLeavesEditedDaysAndPrescriptionsAlone() {
        val (programs, days) = NativeBuiltinCatalog.programs
        val program = programs.single { it.id == "bands-ab" }
        val stock = days.filter { it.programId == program.id }.sortedBy { it.sortOrder }
        val legacyA = """["band-chest-press","band-row","band-overhead-press","band-biceps-curl","band-triceps-pushdown","band-pallof-press"]"""
        val legacyB = """["band-lat-pulldown","band-chest-press","band-face-pull","band-row","band-biceps-curl","band-pallof-press"]"""
        val previous = listOf(stock[0].copy(exercisesJson = legacyA), stock[1].copy(exercisesJson = legacyB))
        val update = requireNotNull(NativeBuiltinCatalog.reviewUpdate(ProgramWithDays(program, previous)))
        assertEquals(stock, update.days)
        assertEquals(previous.map { it.compositeId }, update.days.map { it.compositeId })
        assertNull(NativeBuiltinCatalog.reviewUpdate(update))
        assertNull(NativeBuiltinCatalog.reviewUpdate(ProgramWithDays(program, previous.map {
            if (it.dayId == "A") it.copy(exercisesJson = """["band-row"]""") else it
        })))
        val editedRecipe = program.copy(rawJson = JSONObject(program.rawJson)
            .put("prescriptions", JSONObject().put("band-row", JSONObject().put("weight", 8))).toString())
        assertNull(NativeBuiltinCatalog.reviewUpdate(ProgramWithDays(editedRecipe, previous)))
    }

    @Test fun catalogLoadIsLeftBlankButExplicitProgramLoadIsPreserved() {
        val source = NativeBuiltinCatalog.exercises.single { it.id == "leg-press" }
        val program = NativeBuiltinCatalog.programs.first.single { it.id == "gym-fullbody" }
        val default = NativeProgramPrescription.effective(program, JSONObject.quote("leg-press"), source, emptyMap(), 90)
        assertEquals("", NativeProgramPrescription.fields(default).weight)
        val explicit = NativeProgramPrescription.effective(program,
            """{"id":"leg-press","weightKg":32.5}""", source, emptyMap(), 90)
        assertEquals(32.5, explicit.getDouble("weight"), 0.0)
    }

    @Test fun lowReadinessChangesOnlySupportedCurrentDayDrafts() {
        val now = LocalDate.parse("2026-11-18")
        val low = RecoveryDayReading(now, "samsung_health", ReadingState.AVAILABLE, RecoverySource.SAMSUNG_ENERGY,
            48.0, 48.0, 48.0, null, null, "")
        assertTrue(suggestGentlerStarterPlan(low, now))
        assertFalse(suggestGentlerStarterPlan(low, now.plusDays(1)))
        assertFalse(suggestGentlerStarterPlan(low.copy(state = ReadingState.NO_DATA), now))
        assertFalse(suggestGentlerStarterPlan(null, now))
        val gentle = StudioStarterPlan(setOf("machines"), true, "Edzőterem", "Haladó", 60,
            emptySet(), emptySet(), gentlerStart = true)
        assertTrue(requireNotNull(gentle.buildFromTemplates(catalog, templates, "Izomépítés", 3))
            .days.flatten().all { it.defaultSets <= 2 })
    }

    @Test fun onlyProvenRepsAndSetsCanProvideATrialLoad() {
        fun row(reps: Int, done: Boolean = true, effort: String = "good"): WorkoutHistoryEntity {
            val raw = JSONObject().put("id", "finished").put("programId", "gym-fullbody")
                .put("exercises", JSONArray().put(JSONObject().put("id", "leg-press").put("loadType", "total")
                    .put("repUnit", "ism.").put("effort", effort).put("sets", JSONArray().apply {
                        repeat(2) { put(JSONObject().put("weight", 25).put("reps", reps).put("done", done)) }
                    })))
            return WorkoutHistoryEntity(id = "finished", workoutKey = "finished", syncId = "finished",
                started = "2026-11-16T18:00:00Z", finished = "2026-11-16T19:00:00Z",
                startedEpochMs = 1794852000000, finishedEpochMs = 1794855600000, durationSeconds = 3600,
                programId = "gym-fullbody", programName = "Gym", dayId = "A", dayName = "A", scheduleId = "",
                quickWorkout = false, originDevice = "phone", ownershipProtocol193 = 0, ownerEpoch193 = 0,
                effortFeedback = null, feedbackSkipped = false, feedbackUpdatedAt = 0,
                totalVolumeKg = 0.0, completedSetsCount = 2, totalSetsCount = 2, totalRepsCount = reps * 2,
                hasPr226 = false, prRecordsJson226 = "[]", wearMetricsJson = null, health240Json = null,
                timeLogs162Json = null, activeIntervalsJson = null, editedAt = null, rawJson = raw.toString())
        }
        fun load(row: WorkoutHistoryEntity, sets: Int = 2) =
            NativeProfileHistory128.startingLoad(row, "leg-press", "total", "ism.", "", "8–12", sets)
        assertEquals(25.0, requireNotNull(load(row(10))), 0.0)
        assertNull(load(row(6)))
        assertNull(load(row(10, done = false)))
        assertNull(load(row(10, effort = "pain")))
        assertNull(load(row(10), sets = 3))
    }
}
