package com.example.followup.inappvideo

import com.example.ui.components.inappvideo.ExerciseVideoLoad
import com.example.ui.components.inappvideo.EXERCISE_VIDEO_ORIGIN
import com.example.ui.components.inappvideo.exerciseVideoPlayerEvent
import com.example.ui.components.inappvideo.ExerciseVideoMode
import com.example.ui.components.inappvideo.allowedInAppVideoNavigation
import com.example.ui.components.inappvideo.embeddedYouTubeError
import com.example.ui.components.inappvideo.exerciseYouTubeEmbedHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppVideoPolicyTest {
    private val selected = "MeIiIdhvXT4"

    @Test fun embedUsesOnlyTheSelectedVerifiedId() {
        val html = exerciseYouTubeEmbedHtml(selected)
        assertTrue(html.contains("https://www.youtube.com/embed/$selected"))
        assertTrue(html.contains("origin=https%3A%2F%2Fcom.trainpilot.com"))
        assertTrue(html.contains("autoplay=0"))
        assertTrue(html.contains("playsinline=1"))
        assertTrue(html.contains("rel=0"))
        assertFalse(html.contains("tpPlayer.playVideo"))
        assertFalse(html.contains("id=\"cover\""))
        assertFalse(html.contains("pSHjTRCQxIw"))
        assertTrue(html.contains("trainpilot-video:"))
        assertTrue(html.contains("function tpPause()"))
        assertFalse(html.contains("addJavascriptInterface"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun embedRejectsUnverifiedIdToPreventHtmlScriptInjection() {
        exerciseYouTubeEmbedHtml("bad\" onload=\"alert(1)")
    }

    @Test fun browserOnlyAllowsExplicitHttpsPublicDomainNavigation() {
        assertTrue(allowedInAppVideoNavigation("https://www.youtube.com/watch?v=$selected"))
        assertTrue(allowedInAppVideoNavigation("https://vimeo.com/123456"))
        listOf(
            "http://www.youtube.com/watch?v=$selected",
            "intent://example/#Intent",
            "file:///sdcard/secret",
            "content://provider/secret",
            "javascript:alert(1)",
            "data:text/html,hello",
            "https://127.0.0.1/watch",
            "https://localhost/watch",
            "https://example.com:8080/watch",
            "https://user:pass@example.com/video",
            " https://vimeo.com/123456 "
        ).forEach { assertFalse(it, allowedInAppVideoNavigation(it)) }
    }

    @Test fun blockedUnavailableAndUnknownPlayerErrorsAreExplicit() {
        for (code in listOf(2, 5, 100, 101, 150, 153, 999)) {
            assertFalse("Error $code must not be silent", embeddedYouTubeError(code).isBlank())
        }
        assertEquals(embeddedYouTubeError(101), embeddedYouTubeError(150))
        assertTrue(embeddedYouTubeError(100).contains("eltávolításra"))
        assertTrue(embeddedYouTubeError(153).contains("azonosítását"))
    }

    @Test fun modeNamesDistinguishEmbedFromRealInAppBrowser() {
        assertFalse(ExerciseVideoMode.EMBED == ExerciseVideoMode.BROWSER)
    }
    @Test fun apiReadyIsNotPlaybackAndCannotUndoFailureOrPlayInProgress() {
        var state: ExerciseVideoLoad = ExerciseVideoLoad.Loading
        assertEquals(state, exerciseVideoPlayerEvent(state, "paused"))
        state = exerciseVideoPlayerEvent(state, "ready")
        assertEquals(ExerciseVideoLoad.Ready, state)
        state = exerciseVideoPlayerEvent(state, "starting")
        assertEquals(ExerciseVideoLoad.Starting, state)
        assertEquals(state, exerciseVideoPlayerEvent(state, "ready"))
        state = exerciseVideoPlayerEvent(state, "playing")
        assertEquals(ExerciseVideoLoad.Playing, state)
        assertEquals(state, exerciseVideoPlayerEvent(state, "ready"))
        state = exerciseVideoPlayerEvent(state, "error:153")
        assertTrue(state is ExerciseVideoLoad.Failed)
        for (late in listOf("ready", "paused", "playing", "starting")) {
            assertEquals(state, exerciseVideoPlayerEvent(state, late))
        }
    }

    @Test fun autoplayBlockIsFailureAndAppIdentityMatchesInstalledPackage() {
        assertTrue(exerciseVideoPlayerEvent(ExerciseVideoLoad.Starting, "blocked") is ExerciseVideoLoad.Failed)
        assertEquals("https://" + com.repforge.app.BuildConfig.APPLICATION_ID.lowercase(java.util.Locale.ROOT),
            EXERCISE_VIDEO_ORIGIN)
    }

}
