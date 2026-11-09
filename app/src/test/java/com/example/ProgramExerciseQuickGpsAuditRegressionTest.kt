package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppLanguage
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.LoggedWorkoutEntry
import com.example.model.ProgramDayModel
import com.example.model.TrainingProgramModel
import com.example.model.translateEquipmentLabel
import com.example.model.translateMuscleListLabel
import com.example.model.translateProgramTitle
import com.example.ui.components.parseYouTubeVideoId
import com.example.ui.components.programEquipmentCategoryLabel
import com.example.ui.components.programEquipmentSummary
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.NativeOutdoorStart
import com.repforge.app.NativeWorkoutSession
import com.repforge.app.data.db.NativeBuiltinCatalog
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgramExerciseQuickGpsAuditRegressionTest {

    @Test
    fun homeBasicProgramRenamedToOtthoniABWithStableIdAndLegacyAliasCompatibility() {
        val (programs, _) = NativeBuiltinCatalog.programs
        val homeBasic = programs.firstOrNull { it.id == "home-basic" }
        assertNotNull("home-basic must exist in builtin catalog", homeBasic)
        requireNotNull(homeBasic)

        assertEquals("home-basic", homeBasic.id)
        assertEquals("Otthoni A/B", homeBasic.nameHu)
        assertEquals("Home A/B", homeBasic.nameEn)
        assertEquals("Zuhause A/B", homeBasic.nameDe)
        assertEquals("Acasă A/B", homeBasic.nameRo)

        // Legacy alias translation compatibility (non-destructive for historical logs)
        assertEquals("Home A/B", translateProgramTitle("Otthoni A/B", AppLanguage.EN))
        assertEquals("Zuhause A/B", translateProgramTitle("Otthoni A/B", AppLanguage.DE))
        assertEquals("Acasă A/B", translateProgramTitle("Otthoni A/B", AppLanguage.RO))

        assertEquals("Home A/B", translateProgramTitle("Otthoni A/B – Alap", AppLanguage.EN))
        assertEquals("Zuhause A/B", translateProgramTitle("Otthoni A/B – Alap", AppLanguage.DE))
        assertEquals("Acasă A/B", translateProgramTitle("Otthoni A/B – Alap", AppLanguage.RO))
        assertEquals("Home A/B (personalized)", translateProgramTitle("Otthoni A/B – Alap (személyre szabott)", AppLanguage.EN))

        // Historical HU log entry must preserve its exact stored HU title when viewed in HU
        val historicalLog = LoggedWorkoutEntry(
            id = "hist_1",
            titleHu = "Otthoni A/B – Alap"
        )
        assertEquals("Otthoni A/B – Alap", historicalLog.localizedTitle(AppLanguage.HU))
        assertEquals("Home A/B", historicalLog.localizedTitle(AppLanguage.EN))
        assertEquals("Zuhause A/B", historicalLog.localizedTitle(AppLanguage.DE))
        assertEquals("Acasă A/B", historicalLog.localizedTitle(AppLanguage.RO))
    }

    @Test
    fun all13BuiltinProgramsAreValidAndHomeLevel2DiffersFromHomeBasic() {
        val (programs, days) = NativeBuiltinCatalog.programs
        val exercisesById = NativeBuiltinCatalog.exercises.associateBy { it.id }

        assertEquals(13, programs.size)
        assertEquals(13, programs.map { it.id }.distinct().size)

        val expectedIds = setOf(
            "home-basic", "home-level2", "home-varied", "gym-fullbody", "gym-ab",
            "home-upper-ab", "home-quick-30", "calisthenics-ab", "bands-ab",
            "kettlebell-ab", "gym-upper-lower", "gym-ppl", "gym-strength-abc"
        )
        assertEquals(expectedIds, programs.map { it.id }.toSet())

        for (prog in programs) {
            assertTrue("Blank nameHu for ${prog.id}", prog.nameHu.isNotBlank())
            assertTrue("Blank nameEn for ${prog.id}", prog.nameEn.isNotBlank())
            assertTrue("Blank nameDe for ${prog.id}", prog.nameDe.isNotBlank())
            assertTrue("Blank nameRo for ${prog.id}", prog.nameRo.isNotBlank())
            assertTrue("Blank location for ${prog.id}", prog.location.isNotBlank())
            assertTrue("Blank level for ${prog.id}", prog.level.isNotBlank())

            val progDays = days.filter { it.programId == prog.id }
            assertTrue("Program ${prog.id} must have at least 1 day", progDays.isNotEmpty())

            for (day in progDays) {
                assertTrue("Blank day nameHu in ${prog.id}:${day.dayId}", day.nameHu.isNotBlank())
                assertTrue("Blank day nameEn in ${prog.id}:${day.dayId}", day.nameEn.isNotBlank())
                assertTrue("Blank day nameDe in ${prog.id}:${day.dayId}", day.nameDe.isNotBlank())
                assertTrue("Blank day nameRo in ${prog.id}:${day.dayId}", day.nameRo.isNotBlank())

                val arr = JSONArray(day.exercisesJson)
                val dayExIds = (0 until arr.length()).map { idx ->
                    val item = arr.get(idx)
                    if (item is JSONObject) item.optString("id") else item.toString()
                }
                assertTrue("Day ${day.compositeId} must have >= 3 exercises", dayExIds.size >= 3)
                assertEquals(
                    "Duplicate exercises in ${day.compositeId}: $dayExIds",
                    dayExIds.size,
                    dayExIds.distinct().size
                )
                for (exId in dayExIds) {
                    val ex = exercisesById[exId]
                    assertNotNull("Unknown exercise $exId in ${day.compositeId}", ex)
                    requireNotNull(ex)
                    assertNotEquals(
                        "Indoor program ${prog.id} should not contain distance exercise $exId",
                        "distance",
                        ex.measurementType
                    )
                }
            }
        }

        // Verify home-level2 is NOT a duplicate of home-basic
        fun programExerciseList(progId: String): List<List<String>> =
            days.filter { it.programId == progId }.sortedBy { it.sortOrder }.map { d ->
                val arr = JSONArray(d.exercisesJson)
                (0 until arr.length()).map { idx ->
                    val item = arr.get(idx)
                    if (item is JSONObject) item.optString("id") else item.toString()
                }
            }

        val basicDays = programExerciseList("home-basic")
        val level2Days = programExerciseList("home-level2")
        assertNotEquals("home-level2 must differ from home-basic", basicDays, level2Days)
        assertNotEquals("home-level2 Day A must differ from home-basic Day A", basicDays[0], level2Days[0])
        assertNotEquals("home-level2 Day B must differ from home-basic Day B", basicDays[1], level2Days[1])
    }

    @Test
    fun programEquipmentSummaryIsDataDrivenForBuiltinAndCustomPrograms() {
        // 1. home-basic has no resistance bands -> must NOT mention gumiszalag / resistance band
        for (lang in AppLanguage.entries) {
            val summaryById = programEquipmentCategoryLabel("home-basic", lang)
            val summaryByName = programEquipmentCategoryLabel("Otthoni A/B", lang)
            val summaryByLegacy = programEquipmentCategoryLabel("Otthoni A/B – Alap", lang)
            assertEquals(summaryById, summaryByName)
            assertEquals(summaryById, summaryByLegacy)
            assertFalse("home-basic ($lang) must not claim band: $summaryById",
                summaryById.lowercase().contains("gumiszalag") ||
                    summaryById.lowercase().contains("resistance band") ||
                    summaryById.lowercase().contains("widerstandsband") ||
                    summaryById.lowercase().contains("bandă elastică")
            )
        }
        val homeBasicHu = programEquipmentCategoryLabel("home-basic", AppLanguage.HU).lowercase()
        assertTrue(homeBasicHu.contains("kézisúlyzó"))
        assertTrue(homeBasicHu.contains("kétkezes rúd"))
        assertTrue(homeBasicHu.contains("pad/támasz"))
        assertTrue(homeBasicHu.contains("saját testsúly"))

        // 2. bands-ab MUST include gumiszalag and stabil rögzítés
        val bandsHu = programEquipmentCategoryLabel("bands-ab", AppLanguage.HU).lowercase()
        assertTrue("bands-ab HU: $bandsHu", bandsHu.contains("gumiszalag"))
        assertTrue("bands-ab HU anchor: $bandsHu", bandsHu.contains("stabil rögzítés"))

        val bandsEn = programEquipmentCategoryLabel("bands-ab", AppLanguage.EN).lowercase()
        assertTrue("bands-ab EN: $bandsEn", bandsEn.contains("resistance band"))
        assertTrue("bands-ab EN anchor: $bandsEn", bandsEn.contains("secure anchor"))

        // 3. calisthenics-ab MUST include pull-up bar and dip bars
        val caliHu = programEquipmentCategoryLabel("calisthenics-ab", AppLanguage.HU).lowercase()
        assertTrue("calisthenics-ab HU: $caliHu", caliHu.contains("húzódzkodórúd"))
        assertTrue("calisthenics-ab HU: $caliHu", caliHu.contains("tolódzkodó korlát"))

        // 4. kettlebell-ab MUST include kettlebell
        val kbHu = programEquipmentCategoryLabel("kettlebell-ab", AppLanguage.HU).lowercase()
        assertTrue("kettlebell-ab HU: $kbHu", kbHu.contains("kettlebell"))
        assertFalse("kettlebell-ab HU should not claim gumiszalag: $kbHu", kbHu.contains("gumiszalag"))

        // 5. Custom program equipment summary updates dynamically when exercises change
        val bwEx = ExerciseItem(id = "pushup", nameHu = "Fekvőtámasz", equipmentHu = "testsúly", loadType = "bodyweight")
        val dbEx = ExerciseItem(id = "goblet-squat", nameHu = "Goblet guggolás", equipmentHu = "1 kézisúlyzó", loadType = "single_dumbbell")
        val bandEx = ExerciseItem(id = "band-row", nameHu = "Gumiszalagos evezés", equipmentHu = "gumiszalag + stabil rögzítés", loadType = "band")
        val machineEx = ExerciseItem(id = "leg-press", nameHu = "Lábtoló", equipmentHu = "lábtoló gép", loadType = "machine")

        val customBwProgram = TrainingProgramModel(
            id = "custom_1",
            nameHu = "Otthoni saját terv",
            nameEn = "Custom Home Plan",
            subtitleHu = "Saját",
            subtitleEn = "Custom",
            tagsHu = listOf("Otthon", "Kezdő"),
            isCustom = true,
            isActive = true,
            days = listOf(
                ProgramDayModel("d1", "A nap", "A", "A", 45, listOf(bwEx))
            )
        )
        val bwSummary = programEquipmentSummary(customBwProgram, AppLanguage.HU)
        assertTrue("Bodyweight-only custom program: $bwSummary", bwSummary.contains("Saját testsúlyos • Eszköz nélkül"))
        assertFalse(bwSummary.lowercase().contains("gumiszalag"))

        val withDumbbell = customBwProgram.copy(
            days = listOf(ProgramDayModel("d1", "A nap", "A", "A", 45, listOf(bwEx, dbEx)))
        )
        val dbSummary = programEquipmentSummary(withDumbbell, AppLanguage.HU).lowercase()
        assertTrue(dbSummary.contains("kézisúlyzó"))
        assertTrue(dbSummary.contains("saját testsúly"))
        assertFalse(dbSummary.contains("gumiszalag"))

        val withBandAndMachine = customBwProgram.copy(
            days = listOf(ProgramDayModel("d1", "A nap", "A", "A", 45, listOf(bandEx, machineEx)))
        )
        val bandMachineSummary = programEquipmentSummary(withBandAndMachine, AppLanguage.HU).lowercase()
        assertTrue(bandMachineSummary.contains("gumiszalag"))
        assertTrue(bandMachineSummary.contains("stabil rögzítés"))
        assertTrue(bandMachineSummary.contains("edzőtermi gépek"))
        assertFalse(bandMachineSummary.contains("kézisúlyzó"))
    }

    @Test
    fun all140BuiltinExercisesPassStructuralAndSemanticAuditAndHomeBasicUsesBarbellFloorPress() {
        val exercises = NativeBuiltinCatalog.exercises
        assertEquals(140, exercises.size)
        assertEquals(140, exercises.map { it.id }.distinct().size)

        val validLoadTypes = setOf(
            "bodyweight", "single_dumbbell", "per_hand", "total", "weight",
            "barbell", "kettlebell", "band", "cable", "machine"
        )
        val validMeasurementTypes = setOf("reps", "time", "bilateral_time", "distance")

        for (ex in exercises) {
            assertTrue("Blank id", ex.id.isNotBlank())
            assertTrue("Blank nameHu for ${ex.id}", ex.nameHu.isNotBlank())
            assertTrue("Blank nameEn for ${ex.id}", ex.nameEn.isNotBlank())
            assertTrue("Blank nameDe for ${ex.id}", ex.nameDe.isNotBlank())
            assertTrue("Blank nameRo for ${ex.id}", ex.nameRo.isNotBlank())
            assertTrue("Invalid loadType for ${ex.id}: ${ex.loadType}", ex.loadType in validLoadTypes)
            assertTrue("Invalid measurementType for ${ex.id}: ${ex.measurementType}", ex.measurementType in validMeasurementTypes)
            assertTrue("Invalid defaultSets for ${ex.id}", ex.defaultSets in 1..6)
            assertTrue("Invalid defaultRestSeconds for ${ex.id}", ex.defaultRestSeconds in 15..300)
            assertTrue("Blank primaryMuscle for ${ex.id}", ex.primaryMuscle.isNotBlank())
            assertTrue("Blank notesHu for ${ex.id}", ex.notesHu.isNotBlank())
            assertTrue("Blank notesEn for ${ex.id}", ex.notesEn.isNotBlank())
            assertTrue("Blank notesDe for ${ex.id}", ex.notesDe.isNotBlank())
            assertTrue("Blank notesRo for ${ex.id}", ex.notesRo.isNotBlank())

            if (ex.measurementType == "distance") {
                assertTrue("Outdoor distance exercise ${ex.id} must be GPS eligible", NativeOutdoorStart.eligible(ex.id, ex.measurementType, ex.repUnit))
            } else {
                val resolvedVideoId = parseYouTubeVideoId(ex.videoUrl.orEmpty())
                assertTrue("Missing or invalid video URL for ${ex.id}: ${ex.videoUrl}", !resolvedVideoId.isNullOrBlank())
            }

            val raw = JSONObject(ex.rawJson)
            val eqRaw = raw.optString("equipment")
            val targetRaw = raw.optString("target")
            for (lang in AppLanguage.entries) {
                assertTrue(translateEquipmentLabel(eqRaw, ex.loadType, lang).isNotBlank())
                assertTrue(translateMuscleListLabel(targetRaw, lang).isNotBlank())
            }
        }

        // #215: Both barbell-floor-press and db-floor-press exist as distinct catalog exercises
        val exercisesById = exercises.associateBy { it.id }
        val barbellFloorPress = exercisesById["barbell-floor-press"]
        val dbFloorPress = exercisesById["db-floor-press"]
        assertNotNull("barbell-floor-press must exist", barbellFloorPress)
        assertNotNull("db-floor-press must still exist", dbFloorPress)
        assertEquals("Földön fekvenyomás – kétkezes rúddal", barbellFloorPress!!.nameHu)
        assertEquals("Barbell Floor Press", barbellFloorPress.nameEn)
        assertEquals("total", barbellFloorPress.loadType)
        assertEquals("per_hand", dbFloorPress!!.loadType)

        // #215: Only home-basic Day A uses barbell-floor-press in built-in program templates
        val (_, days) = NativeBuiltinCatalog.programs
        val homeBasicDayA = days.single { it.programId == "home-basic" && it.dayId == "A" }
        val homeBasicDayAIds = JSONArray(homeBasicDayA.exercisesJson).let { arr ->
            (0 until arr.length()).map { idx ->
                val item = arr.get(idx)
                if (item is JSONObject) item.optString("id") else item.toString()
            }
        }
        assertTrue("home-basic Day A must contain barbell-floor-press", "barbell-floor-press" in homeBasicDayAIds)
        assertFalse("home-basic Day A must not contain db-floor-press", "db-floor-press" in homeBasicDayAIds)
    }

    @Test
    fun outdoorGpsEligibilityMatchesOnlyOutdoorDistanceExercises() {
        val exercises = NativeBuiltinCatalog.exercises
        val eligibleIds = exercises.filter {
            NativeOutdoorStart.eligible(it.id, it.measurementType, it.repUnit)
        }.map { it.id }.toSet()

        val expectedOutdoorIds = setOf(
            "running", "jogging", "walking", "hiking", "cycling", "inline-skating"
        )
        assertEquals(expectedOutdoorIds, eligibleIds)

        // Indoor timed / strength exercises must NEVER be GPS-eligible
        assertFalse(NativeOutdoorStart.eligible("plank", "time", "mp"))
        assertFalse(NativeOutdoorStart.eligible("side-plank", "bilateral_time", "mp/oldal"))
        assertFalse(NativeOutdoorStart.eligible("goblet-squat", "reps", "ism."))
        assertFalse(NativeOutdoorStart.eligible("barbell-bench", "reps", "ism."))
    }

    @Test
    fun customExercisesAndQuickWorkoutIntegrateCleanlyWithRoomSession(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = TrainPilotDatabase.getInstance(context)
        TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context)
        ActiveWorkoutStore.clear(context)

        val vm = TrainPilotViewModel()
        vm.addCustomExercise(
            nameHu = "Egyedi Plank Tartás",
            category = ExerciseCategory.CORE,
            equipmentHu = "Saját testsúly",
            descriptionHu = "Feszítsd meg a törzsedet 60 másodpercig.",
            isTimed = true,
            addToCurrentProgramDay = false
        )
        vm.addCustomExercise(
            nameHu = "Egyedi Kézisúlyzós Nyomás",
            category = ExerciseCategory.CHEST,
            equipmentHu = "2 kézisúlyzó + pad",
            descriptionHu = "Kontrollált leengedés és kinyomás.",
            isTimed = false,
            addToCurrentProgramDay = false
        )

        val customExercises = vm.uiState.value.exercises.filter { it.isCustom }
        assertEquals(2, customExercises.size)
        assertNotEquals(customExercises[0].id, customExercises[1].id)

        val timedCustom = customExercises.first { it.isTimed }
        assertEquals("bodyweight", timedCustom.loadType)
        assertEquals("mp", timedCustom.repUnit)
        assertEquals("time", timedCustom.measurementType)
        assertEquals(0f, timedCustom.defaultWeightKg, 0.001f)

        val weightedCustom = customExercises.first { !it.isTimed }
        assertEquals("per_hand", weightedCustom.loadType)
        assertEquals("ism.", weightedCustom.repUnit)
        assertEquals("reps", weightedCustom.measurementType)
        assertEquals(20f, weightedCustom.defaultWeightKg, 0.001f)

        // Quick workout with catalog exercises (strength + timed + bilateral)
        val frame = NativeWorkoutSession.startQuickExercises(
            context = context,
            exerciseIds = listOf("goblet-squat", "plank", "side-plank"),
            title = "Gyors teszt edzés",
            automaticEnabled = true,
            database = db
        )
        assertTrue(frame.optBoolean("active"))
        assertEquals(3, frame.getJSONArray("exercises").length())
        ActiveWorkoutStore.clear(context)
        Unit
    }
}
