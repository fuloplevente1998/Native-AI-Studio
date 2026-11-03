package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

}
