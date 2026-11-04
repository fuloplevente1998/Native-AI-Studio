package com.example.ui.components

import android.content.Intent
import androidx.compose.ui.graphics.Color
import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.model.ProgramDayModel
import com.example.model.TrainingProgramModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ExerciseVideoResolverTest {
    private val gobletId = "MeIiIdhvXT4"
    private val plankId = "pSHjTRCQxIw"

    @Test fun supportedYouTubeFormatsResolveExactId() {
        listOf(
            gobletId,
            "https://www.youtube.com/watch?v=$gobletId&t=30",
            "https://m.youtube.com/watch?v=$gobletId",
            "https://youtu.be/$gobletId?t=2",
            "https://www.youtube.com/embed/$gobletId",
            "https://www.youtube-nocookie.com/embed/$gobletId",
            "https://youtube.com/shorts/$gobletId",
            "https://youtube.com/live/$gobletId"
        ).forEach { source -> assertEquals(source, gobletId, parseYouTubeVideoId(source)) }
    }

    @Test fun rejectsSpoofedUnsupportedAndTruncatedYouTubeIds() {
        listOf(
            "",
            "abc",
            "https://youtube.com/watch?v=short",
            "https://youtube.com/watch?v=$gobletId-extra",
            "https://www.youtube.com.evil.example/watch?v=$gobletId",
            "https://notyoutube.com/watch?v=$gobletId",
            "http://youtube.com/watch?v=$gobletId",
            "https://youtu.be/$gobletId/another",
            "javascript:alert(1)"
        ).forEach { source -> assertNull(source, parseYouTubeVideoId(source)) }
    }

    @Test fun selectedSavedVideoOverridesCatalogForCustomAndBuiltin() {
        val result = resolveExerciseVideoLink(
            storedVideoUrl = "https://youtu.be/$plankId",
            catalogVideoUrl = "https://www.youtube.com/watch?v=$gobletId",
            isCustom = false
        )
        assertEquals(ExerciseVideoState.YOUTUBE, result.state)
        assertEquals(plankId, result.youtubeId)
        assertEquals("https://www.youtube.com/watch?v=$plankId", result.externalUrl)
    }

    @Test fun customMissingNeverFallsBackToCatalog() {
        val result = resolveExerciseVideoLink("", "https://youtu.be/$gobletId", isCustom = true)
        assertEquals(ExerciseVideoState.MISSING, result.state)
        assertNull(result.externalUrl)
    }

    @Test fun missingUnknownHasNoGenericOrUnrelatedVideo() {
        val result = resolveExerciseVideoAndNotes("Egyedi, ismeretlen guggolás")
        assertEquals(ExerciseVideoState.MISSING, result.link.state)
        assertNull(result.link.externalUrl)
    }

    @Test fun nonBlankMalformedSavedUrlIsNeverReplacedByCatalog() {
        val result = resolveExerciseVideoLink("notaurl", "https://youtu.be/$gobletId", isCustom = false)
        assertEquals(ExerciseVideoState.INVALID, result.state)
        assertNull(result.externalUrl)
    }

    @Test fun validNonYouTubeCustomLinkCanBeOpenedExternally() {
        val result = resolveExerciseVideoLink("https://vimeo.com/12345", null, isCustom = true)
        assertEquals(ExerciseVideoState.EXTERNAL_ONLY, result.state)
        assertEquals("https://vimeo.com/12345", result.externalUrl)
        assertNull(result.youtubeId)
    }

    @Test fun unsafeLinksNeverOpenOtherApps() {
        listOf(
            "javascript:alert(1)",
            "file:///secret",
            "intent://example",
            "http://example.org",
            "https://user:password@example.org/secret"
        ).forEach { source ->
            assertNull(source, safeExternalExerciseVideoUrl(source))
            assertEquals(ExerciseVideoState.INVALID, resolveExerciseVideoLink(source, null, true).state)
        }
    }

    @Test fun verifiedGobletCatalogMappingKeepsExactExercise() {
        val byId = resolveExerciseVideoAndNotes("Goblet guggolás", exerciseId = "goblet-squat")
        val byName = resolveExerciseVideoAndNotes("Goblet guggolás")
        assertEquals(ExerciseVideoState.YOUTUBE, byId.link.state)
        assertEquals(gobletId, byId.link.youtubeId)
        assertEquals(byId.link.youtubeId, byName.link.youtubeId)
        assertTrue(byId.notesHu.contains("kézisúlyzót"))
    }

    @Test fun ambiguousNameOnlySelectionDoesNotGuessBuiltinVideo() {
        val result = resolveExerciseVideoAndNotes(
            exerciseTitle = "Goblet guggolás",
            allowCatalogByTitle = false
        )
        assertEquals(ExerciseVideoState.MISSING, result.link.state)
    }

    @Test fun unknownIdMustNotUseMatchingTitleFallback() {
        val result = resolveExerciseVideoAndNotes(
            exerciseTitle = "Goblet guggolás",
            exerciseId = "imported-custom-987"
        )
        assertEquals(ExerciseVideoState.MISSING, result.link.state)
    }

    @Test fun customCanUseItsPersistedUrlWithAnUnmatchedId() {
        val result = resolveExerciseVideoAndNotes(
            exerciseTitle = "Egyéni kitörés",
            exerciseId = "custom-123",
            storedVideoUrl = "https://youtu.be/$plankId",
            isCustom = true,
            selectedNotesHu = "Saját végrehajtási utasítás"
        )
        assertEquals(plankId, result.link.youtubeId)
        assertEquals("Saját végrehajtási utasítás", result.notesHu)
    }

    @Test fun customWithSameNameAsBuiltinDoesNotInheritCatalogVideo() {
        val result = resolveExerciseVideoAndNotes(
            exerciseTitle = "Goblet guggolás",
            storedVideoUrl = "",
            isCustom = true
        )
        assertEquals(ExerciseVideoState.MISSING, result.link.state)
        assertFalse(result.notesHu.isBlank())
    }

    @Test fun securityPolicyRejectsUnsafeHttpsTargetsAndPorts() {
        listOf(
            "https://localhost/video",
            "https://127.0.0.1/video",
            "https://192.168.1.10/video",
            "https://private.local/video",
            "https://service.internal/video",
            "https://www.youtube.com:8443/watch?v=$gobletId",
            "https://abc..example.com/video",
            "https://-bad.example.com/video",
            "https://user@example.com/video",
            "https://example.com\\@evil.example/path",
            "https://example.com:9999/watch"
        ).forEach { source ->
            assertNull(source, safeExternalExerciseVideoUrl(source))
        }
        assertEquals(
            "https://vimeo.com/12345",
            safeExternalExerciseVideoUrl("https://vimeo.com/12345")
        )
    }

    @Test fun untrustedYouTubeHostCannotBecomeEmbeddedPlayer() {
        val stored = "https://www.youtube.com.evil.example/watch?v=$gobletId"
        assertNull(parseYouTubeVideoId(stored))
        val resolved = resolveExerciseVideoLink(stored, "https://youtu.be/$plankId", false)
        assertEquals(ExerciseVideoState.EXTERNAL_ONLY, resolved.state)
        assertNull(resolved.youtubeId)
        assertEquals(stored, resolved.externalUrl)
    }

    @Test fun externalExerciseVideoIntentBuildsDirectBrowserActionView() {
        val resolved = resolveExerciseVideoAndNotes("Goblet Squat", exerciseId = "goblet-squat", language = "en")
        val intent = buildExternalExerciseVideoIntent(resolved.link.externalUrl)
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent!!.action)
        assertEquals("https://www.youtube.com/watch?v=$gobletId", intent.dataString)
        assertTrue(intent.categories.contains(Intent.CATEGORY_BROWSABLE))
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertNull(buildExternalExerciseVideoIntent("javascript:alert(1)"))
        assertNull(buildExternalExerciseVideoIntent(null))
    }

    @Test fun catalogExecutionNotesAndModelsLocalizeInHuEnDeRo() {
        val enResolved = resolveExerciseVideoAndNotes("Goblet Squat", exerciseId = "goblet-squat", language = "en")
        val deResolved = resolveExerciseVideoAndNotes("Goblet Kniebeuge", exerciseId = "goblet-squat", language = "de")
        val roResolved = resolveExerciseVideoAndNotes("Genuflexiune Goblet", exerciseId = "goblet-squat", language = "ro")
        assertTrue(enResolved.notesHu.contains("dumbbell", ignoreCase = true))
        assertTrue(deResolved.notesHu.contains("Kurzhantel", ignoreCase = true))
        assertTrue(roResolved.notesHu.contains("ganteră", ignoreCase = true))

        val muscleItem = MuscleLoadItem(
            id = "chest",
            nameHu = "Mell",
            setsText = "12 sorozat",
            percentage = 80,
            ratio = 0.8f,
            levelLabelHu = "Magas",
            topExercisesHu = emptyList(),
            exerciseIdToOpen = "bench-press",
            setsCount = 12
        )
        assertEquals("Chest", muscleItem.localizedName(AppLanguage.EN))
        assertEquals("12 sets", muscleItem.localizedSetsText(AppLanguage.EN))
        assertEquals("Higher load", muscleItem.localizedLevelLabel(AppLanguage.EN))
        assertEquals("Brust", muscleItem.localizedName(AppLanguage.DE))
        assertEquals("12 Sätze", muscleItem.localizedSetsText(AppLanguage.DE))
        assertEquals("Piept", muscleItem.localizedName(AppLanguage.RO))
        assertEquals("12 serii", muscleItem.localizedSetsText(AppLanguage.RO))

        val day = ProgramDayModel(
            id = "day-a",
            dayCode = "A nap",
            titleHu = "Felsőtest A",
            titleEn = "Upper Body A",
            durationMinutes = 52,
            exercises = emptyList(),
            titleDe = "Oberkörper A",
            titleRo = "Partea superioară A"
        )
        assertEquals("Day A", day.localizedDayCode(AppLanguage.EN))
        assertEquals("Tag A", day.localizedDayCode(AppLanguage.DE))
        assertEquals("Ziua A", day.localizedDayCode(AppLanguage.RO))
        assertEquals("Upper Body A", day.localizedTitle(AppLanguage.EN))
        assertEquals("Oberkörper A", day.localizedTitle(AppLanguage.DE))
        assertEquals("Partea superioară A", day.localizedTitle(AppLanguage.RO))

        val program = TrainingProgramModel(
            id = "full-body-a-b",
            nameHu = "Teljes test A/B",
            nameEn = "Full Body A/B",
            subtitleHu = "Heti 3 napos alap program",
            subtitleEn = "3-day foundational full body split",
            tagsHu = listOf("Edzőterem", "Középhaladó"),
            isCustom = false,
            isActive = true,
            days = listOf(day),
            nameDe = "Ganzkörper A/B",
            nameRo = "Full Body A/B",
            subtitleDe = "3-Tage Ganzkörper-Grundprogramm",
            subtitleRo = "Program de bază 3 zile pe săptămână"
        )
        assertEquals("Full Body A/B", program.localizedName(AppLanguage.EN))
        assertEquals("Ganzkörper A/B", program.localizedName(AppLanguage.DE))
        assertEquals("3-day foundational full body split", program.localizedSubtitle(AppLanguage.EN))
        assertTrue(program.localizedTags(AppLanguage.EN).contains("Gym"))

        val ex = ExerciseItem(
            id = "goblet-squat",
            nameHu = "Goblet guggolás",
            nameEn = "Goblet Squat",
            nameDe = "Goblet Kniebeuge",
            nameRo = "Genuflexiune Goblet",
            equipmentHu = "1 kézisúlyzó",
            primaryMusclesHu = "Quad, Farizom",
            secondaryMusclesHu = "Törzs",
            stepsHu = listOf("Tartsd a súlyt mellkas előtt"),
            stepsEn = listOf("Hold the dumbbell at chest height")
        )
        assertEquals("Goblet Squat", ex.localizedName(AppLanguage.EN))
        assertEquals("1 dumbbell", ex.localizedEquipment(AppLanguage.EN))
        assertEquals("Quads, Glutes", ex.localizedPrimaryMuscles(AppLanguage.EN))
        assertEquals("Hold the dumbbell at chest height", ex.localizedSteps(AppLanguage.EN).first())
    }
    @Test fun translationDoesNotReplaceExplicitEquipmentOrCoachFeedback() {
        val ex = ExerciseItem(id = "custom", nameHu = "My exercise", isCustom = true,
            equipmentHu = "My special apparatus", loadType = "per_hand",
            coachHintHu = "Keep this actual Coach restriction", stepsEn = listOf("Execution step"))
        assertEquals("My special apparatus", ex.localizedEquipment(AppLanguage.EN))
        assertEquals("Keep this actual Coach restriction", ex.localizedCoachHint(AppLanguage.EN))
        assertEquals("1 dumbbell", ex.copy(equipmentHu = "1 kézisúlyzó").localizedEquipment(AppLanguage.EN))
        assertEquals("Legs, Glutes", com.example.model.translateMuscleListLabel("Láb/far", AppLanguage.EN))
        assertEquals("Core / Abs", com.example.model.translateMuscleListLabel("Törzs / Has", AppLanguage.EN))
        assertEquals("Forearms, Back", com.example.model.translateMuscleListLabel("Alkar / Hát", AppLanguage.EN))
        assertEquals("Obliques", com.example.model.translateMuscleListLabel("Oldalsó hasizom", AppLanguage.EN))
    }

    @Test fun everyBundledExerciseHasTranslatedEquipmentAndMuscleFragments() {
        val untranslated = Regex("kézisúlyzó|csiga|támasz|rögzítés|gép|futócipő|lábhajlító|combhajlító|hátsó váll|lapocka|kondíció", RegexOption.IGNORE_CASE)
        for (entry in com.repforge.app.data.db.TrainPilotDatabase.defaultExercises()) {
            val raw = org.json.JSONObject(entry.rawJson)
            for (language in listOf(AppLanguage.EN, AppLanguage.DE, AppLanguage.RO)) {
                val equipment = com.example.model.translateEquipmentLabel(raw.optString("equipment"), entry.loadType, language)
                val muscles = com.example.model.translateMuscleListLabel(raw.optString("target"), language)
                assertFalse("${entry.id} $language: $equipment", untranslated.containsMatchIn(equipment))
                assertFalse("${entry.id} $language: $muscles", untranslated.containsMatchIn(muscles))
            }
        }
    }

    @Test fun customAndPersonalProgramsAndWorkoutLogsLocalizeAcrossAllLanguages() {
        val customClone = TrainingProgramModel(
            id = "custom-program-123",
            nameHu = "Otthoni A/B – Alap",
            nameEn = "Otthoni A/B – Alap",
            nameDe = "Otthoni A/B – Alap",
            nameRo = "Otthoni A/B – Alap",
            subtitleHu = "Otthon • Kezdő • 2 edzésnap",
            subtitleEn = "Otthon • Kezdő • 2 edzésnap",
            subtitleDe = "Otthon • Kezdő • 2 edzésnap",
            subtitleRo = "Otthon • Kezdő • 2 edzésnap",
            tagsHu = listOf("Otthon", "Kezdő"),
            isCustom = true,
            isActive = true,
            days = emptyList()
        )
        assertFalse(customClone.localizedName(AppLanguage.DE).contains("Otthoni"))
        assertEquals("Zuhause • Anfänger • 2 Trainingstage", customClone.localizedSubtitle(AppLanguage.DE))
        assertEquals("Home • Beginner • 2 workout days", customClone.localizedSubtitle(AppLanguage.EN))
        assertEquals("Acasă • Începător • 2 zile de antrenament", customClone.localizedSubtitle(AppLanguage.RO))

        val personalPlan = TrainingProgramModel(
            id = "personal-456",
            nameHu = "Saját Erő terv (Otthon)",
            nameEn = "Saját Erő terv (Otthon)",
            subtitleHu = "Kezdő • 3 nap/hét • 45 perc",
            subtitleEn = "Kezdő • 3 nap/hét • 45 perc",
            tagsHu = listOf("Otthon", "Kezdő"),
            isCustom = true,
            isActive = false,
            days = emptyList()
        )
        assertEquals("Custom Strength Plan (Home)", personalPlan.localizedName(AppLanguage.EN))
        assertEquals("Eigener Kraft-Plan (Zuhause)", personalPlan.localizedName(AppLanguage.DE))
        assertEquals("Plan propriu Forță (Acasă)", personalPlan.localizedName(AppLanguage.RO))
        assertEquals("Anfänger • 3 Tage/Woche • 45 Min.", personalPlan.localizedSubtitle(AppLanguage.DE))

        val logged = com.example.model.LoggedWorkoutEntry(
            id = "log-1",
            titleHu = "Otthoni A/B – Alap • A nap • Fő fókusz"
        )
        assertTrue(logged.localizedTitle(AppLanguage.DE).contains("Tag A • Hauptfokus"))
        assertFalse(logged.localizedTitle(AppLanguage.DE).contains("Otthoni"))
    }

}
