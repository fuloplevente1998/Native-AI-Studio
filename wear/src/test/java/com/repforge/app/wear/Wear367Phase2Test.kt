package com.repforge.app.wear

import kotlin.math.hypot
import kotlin.math.min
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** #367 phase 2 (3.1-3.8): pure behaviour of the Wear workout footer, heart rate, timer, lock and paging. */
class Wear367Phase2Test {
    private val sizes = listOf(176f, 192f, 200f, 216f, 225f, 240f, 254f)

    // 3.1 ---------------------------------------------------------------------------------
    @Test fun recordActionIsIdenticalOnFirstMiddleAndLastExercise() {
        val first = wearFooterState(false, false, 0, 0, true)
        val last = wearFooterState(false, true, 5, 0, true)
        assertEquals(WearFooterState(WearFooterKind.RECORD, true), first)
        assertEquals(first, last)
        assertEquals(WearFooterState(WearFooterKind.RECORD, false), wearFooterState(false, true, 0, 0, false))
    }

    @Test fun completedExerciseOffersNextInTheMiddleAndFinishOnlyOnTheLast() {
        assertEquals(WearFooterState(WearFooterKind.NEXT, true), wearFooterState(true, false, 3, 0, false))
        assertEquals(WearFooterState(WearFooterKind.FINISH, true), wearFooterState(true, true, 3, 0, false))
        // Nothing recorded: the workout can never be finished (and so never lost by a mis-tap).
        assertEquals(WearFooterState(WearFooterKind.FINISH, false), wearFooterState(true, true, 0, 0, false))
    }

    @Test fun restSyncAndPhoneStatesOutrankRecordingOnEveryPosition() {
        for (last in listOf(false, true)) {
            assertEquals(WearFooterKind.SKIP_REST, wearFooterState(false, last, 1, 30, true).kind)
            assertEquals(WearFooterKind.SKIP_REST, wearFooterState(true, last, 1, 30, true).kind)
            val blocked = wearFooterState(true, last, 2, 0, true, syncBlocked = true)
            assertEquals(WearFooterState(WearFooterKind.WAIT_SYNC, false), blocked)
            assertEquals(WearFooterState(WearFooterKind.WAIT_PHONE, false), wearFooterState(false, last, 0, 0, true, awaitingPhone = true))
            assertEquals(WearFooterState(WearFooterKind.RESUME_AUTO, true), wearFooterState(false, last, 0, 0, true, autoPausedWithCommand = true))
        }
    }

    // 3.4 / 3.1 geometry ------------------------------------------------------------------
    @Test fun workoutActionSitsCloserToTheRimAndStaysInsideTheRoundAtEverySize() {
        for (size in sizes) for ((w, h) in listOf(size to size, size to size - 20f)) {
            val page = wearWorkoutPageLayout(w, h)
            val old = wearFixedPageLayout(w, h, false, 1)
            val diameter = min(w, h)
            val origin = (h - diameter) / 2f
            val centerY = page.footerTop + WEAR_FOOTER_BUTTON_DP / 2f - origin - diameter / 2f
            assertTrue("button inside the circle at $w x $h", hypot(0f, centerY) + WEAR_FOOTER_BUTTON_DP / 2f <= diameter / 2f - WEAR_FOOTER_RIM_MARGIN_DP + .01f)
            assertTrue("closer to the rim than the old 48 dp footer", page.footerTop + WEAR_FOOTER_BUTTON_DP > old.footerTop + 48f)
            assertTrue("touch target at least the old 56 dp action", WEAR_FOOTER_BUTTON_DP >= 56f)
            assertEquals(WEAR_FOOTER_BUTTON_DP, page.footerWidth, .001f)
            assertEquals(WEAR_PINNED_ROW_DP, page.pinnedHeight, .001f)
        }
    }

    @Test fun bodyPinnedHeartRateRowAndActionNeverOverlap() {
        for (size in sizes) {
            val page = wearWorkoutPageLayout(size, size)
            assertTrue(page.bodyTop + page.bodyHeight <= page.pinnedTop)
            assertEquals(page.footerTop, page.pinnedTop + page.pinnedHeight, .001f)
            // heading + stopwatch face always fit: the timed screens never need to scroll
            assertTrue("timed content fits at $size", page.bodyHeight >= 16f + 2f + WEAR_TIMER_BOX_DP)
        }
        // default (pinned-less) layout is unchanged for the other screens
        val plain = wearFixedPageLayout(192f, 192f, false, 2)
        assertEquals(plain.footerTop, plain.pinnedTop, .001f)
        assertEquals(0f, plain.pinnedHeight, .001f)
    }

    @Test fun previousPerformanceRowYieldsBeforeTheHeartRateOrTheAction() {
        val body192 = wearWorkoutPageLayout(192f, 192f).bodyHeight
        assertTrue(wearShowPreviousRow(body192, 1f))
        assertFalse(wearShowPreviousRow(body192, 1.3f))
        assertFalse(wearShowPreviousRow(wearWorkoutPageLayout(160f, 160f).bodyHeight, 1f))
    }

    // 3.3 ---------------------------------------------------------------------------------
    private fun health(now: Long, hr: Any? = 128, state: String = "active", sampleAgo: Long? = 2000, workout: String = "w1") =
        JSONObject().put("workoutId", workout).put("source", "wear_health_services").put("state", state)
            .put("updatedAt", now - 1000).apply {
                if (hr != null) put("heartRate", hr)
                if (sampleAgo != null) put("lastSampleAt", now - sampleAgo)
            }

    @Test fun liveHeartRateIsShownForTheRunningWorkout() {
        val now = 1_000_000L
        assertEquals(WearWorkoutPulse(128, true), wearWorkoutPulse(health(now), "w1", now))
    }

    @Test fun pausedOrBriefGapShowsTheLastRealSampleDimmedAndOldDataIsMissing() {
        val now = 1_000_000L
        assertEquals(WearWorkoutPulse(131, false), wearWorkoutPulse(health(now, 131, "paused", 40_000), "w1", now))
        assertEquals(WearWorkoutPulse(131, false), wearWorkoutPulse(health(now, 131, "active", 60_000), "w1", now))
        assertEquals(WearWorkoutPulse(null, false), wearWorkoutPulse(health(now, 131, "paused", WEAR_PULSE_FRESH_MS + 1), "w1", now))
    }

    @Test fun missingMeasurementIsNeverZeroOrFabricated() {
        val now = 1_000_000L
        val none = WearWorkoutPulse(null, false)
        assertEquals(none, wearWorkoutPulse(null, "w1", now))
        assertEquals(none, wearWorkoutPulse(health(now, hr = null), "w1", now))
        assertEquals(none, wearWorkoutPulse(health(now, hr = 0), "w1", now))
        assertEquals(none, wearWorkoutPulse(health(now, hr = 12), "w1", now))
        assertEquals(none, wearWorkoutPulse(health(now, hr = 400), "w1", now))
        assertEquals(none, wearWorkoutPulse(health(now, state = "paused", sampleAgo = null), "w1", now))
        assertEquals(none, wearWorkoutPulse(health(now, workout = "old"), "w1", now))
        assertEquals(none, wearWorkoutPulse(health(now, state = "ended", sampleAgo = 40_000), "w1", now))
        assertEquals(none, wearWorkoutPulse(health(now, state = "paused", sampleAgo = 40_000).put("heartRateAvailability", "unavailable"), "w1", now))
        assertEquals(none, wearWorkoutPulse(health(now).put("source", "phone"), "w1", now))
    }

    @Test fun movementRowCarriesOnlyRealOutdoorValues() {
        val now = 1_000_000L
        assertNull(wearLiveMovementRow(health(now), "w1"))
        assertEquals("Edzés: 120 m", wearLiveMovementRow(health(now).put("distanceMeters", 120), "w1"))
        assertEquals("Edzés: 300 lépés · 120 m", wearLiveMovementRow(health(now).put("steps", 300).put("distanceMeters", 120), "w1"))
        assertNull(wearLiveMovementRow(health(now).put("steps", -1), "w1"))
        assertNull(wearLiveMovementRow(health(now, workout = "old").put("steps", 10), "w1"))
    }

    // 3.6 ---------------------------------------------------------------------------------
    @Test fun stopwatchFaceHasOneSizeForEveryStateAndFontScale() {
        for (scale in listOf(.85f, 1f, 1.15f, 1.3f, 2f))
            assertEquals(WEAR_TIMER_SP * min(scale, WEAR_TIMER_MAX_FONT_SCALE), wearTimerFontSp(scale) * scale, .001f)
        assertTrue(WEAR_TIMER_BOX_DP >= WEAR_TIMER_LINE_SP * WEAR_TIMER_MAX_FONT_SCALE)
        assertEquals(WearTimerGlyph.PLAY, wearTimerGlyph(done = false, running = false))
        assertEquals(WearTimerGlyph.PAUSE, wearTimerGlyph(done = false, running = true))
        assertEquals(WearTimerGlyph.DONE, wearTimerGlyph(done = true, running = true))
    }

    // 3.5 ---------------------------------------------------------------------------------
    @Test fun unlockHoldSurvivesFingerDriftButNotLeavingTheButton() {
        val margin = WEAR_UNLOCK_DRIFT_DP
        assertTrue(wearUnlockHoldInside(24f, 24f, 48f, 48f, margin))
        assertTrue(wearUnlockHoldInside(-margin, 48f + margin, 48f, 48f, margin))
        assertFalse(wearUnlockHoldInside(-margin - 1f, 20f, 48f, 48f, margin))
        assertFalse(wearUnlockHoldInside(20f, 48f + margin + 1f, 48f, 48f, margin))
    }

    @Test fun unlockCountdownMatchesTheRealThreeSecondHold() {
        assertEquals(3L, WearUnlockGesture189.HOLD_MS / 1000L)
        assertEquals(3, wearUnlockSecondsLeft(0))
        assertEquals(3, wearUnlockSecondsLeft(-5))
        assertEquals(3, wearUnlockSecondsLeft(1))
        assertEquals(2, wearUnlockSecondsLeft(1000))
        assertEquals(1, wearUnlockSecondsLeft(2999))
        assertEquals(0, wearUnlockSecondsLeft(3000))
        assertEquals(0, wearUnlockSecondsLeft(9000))
        val g = WearUnlockGesture189()
        g.down(0)
        assertFalse(g.up(2999))
        g.down(10_000)
        assertTrue(g.up(13_000))
    }

    @Test fun lockInstructionSaysContinuousThreeSecondsInEveryLanguage() {
        for (lang in listOf("hu", "en", "de", "ro")) {
            val copy = WearTouchCopy189.language(lang)
            assertTrue(copy.hold.contains('3'))
            assertTrue(copy.sec.isNotBlank())
        }
        assertTrue(WearTouchCopy189.language("hu").hold.contains("megszakítás nélkül"))
        assertTrue(WearTouchCopy189.language("en").hold.contains("without letting go"))
    }

    // 3.2 ---------------------------------------------------------------------------------
    @Test fun controlCaptionsForPauseResumeFinishAndDiscardAreAllDifferent() {
        for (lang in listOf("hu", "en", "de", "ro")) {
            val captions = listOf("Szünet", "Folytatás", "Befejezés", "Elvetés").map { wearText(it, lang) }
            assertEquals("distinct captions in $lang: $captions", 4, captions.toSet().size)
        }
        assertEquals("Discard", wearText("Elvetés", "en"))
    }

    // 3.7 / 3.8 ---------------------------------------------------------------------------
    @Test fun rightwardSwipesKeepTheSystemBackEdgeWhileLeftwardSwipesMayStartNearTheRim() {
        val w = 192f
        assertFalse(wearSwipeMayStart(10f, w, 30f, WEAR_SWIPE_BACK_EDGE_DP, WEAR_SWIPE_RIM_DP))
        assertTrue(wearSwipeMayStart(WEAR_SWIPE_BACK_EDGE_DP, w, 30f, WEAR_SWIPE_BACK_EDGE_DP, WEAR_SWIPE_RIM_DP))
        // the former 24 dp right band no longer swallows leftward (next page) swipes
        assertTrue(wearSwipeMayStart(w - 10f, w, -30f, WEAR_SWIPE_BACK_EDGE_DP, WEAR_SWIPE_RIM_DP))
        assertFalse(wearSwipeMayStart(w - 3f, w, -30f, WEAR_SWIPE_BACK_EDGE_DP, WEAR_SWIPE_RIM_DP))
        assertFalse(wearSwipeMayStart(3f, w, -30f, WEAR_SWIPE_BACK_EDGE_DP, WEAR_SWIPE_RIM_DP))
        assertFalse(wearSwipeMayStart(96f, w, 0f, WEAR_SWIPE_BACK_EDGE_DP, WEAR_SWIPE_RIM_DP))
        // the old generic helper still describes a symmetric centre band
        assertTrue(wearMainSwipeCanStart(96f, w, 24f))
    }

    @Test fun slowLongDragsAndShortQuickFlicksChangeThePageButNotAccidentalMovements() {
        // existing 36 dp commit distance is kept
        assertEquals(36f, WEAR_SWIPE_COMMIT_DP, 0f)
        assertTrue(wearSwipeCommits(-WEAR_SWIPE_COMMIT_DP, 5f, 0f))
        assertTrue(wearSwipeCommits(WEAR_SWIPE_COMMIT_DP, -5f, 10f))
        // slow and short: no change
        assertFalse(wearSwipeCommits(-24f, 2f, -100f))
        // quick flick in the same direction: changes
        assertTrue(wearSwipeCommits(-22f, 3f, -700f))
        assertTrue(wearSwipeCommits(20f, 3f, 700f))
        // flick that ended in the opposite direction, or too small to be intentional: no change
        assertFalse(wearSwipeCommits(-22f, 3f, 700f))
        assertFalse(wearSwipeCommits(-WEAR_SWIPE_FLING_MIN_DP + 1f, 0f, -2000f))
        // diagonal or vertical-dominant movement never pages, however far or fast
        assertFalse(wearSwipeCommits(-40f, 40f, -900f))
        assertFalse(wearSwipeCommits(-40f, 30f, -900f))
        assertTrue(wearSwipeCommits(-40f, 20f, 0f))
    }

    @Test fun thresholdConstantsKeepTheirDocumentedRelations() {
        assertTrue(WEAR_SWIPE_FLING_MIN_DP < WEAR_SWIPE_COMMIT_DP)
        assertTrue(WEAR_SWIPE_RIM_DP < WEAR_SWIPE_BACK_EDGE_DP)
        assertEquals(500L, WEAR_TAP_MAX_MS)
        assertTrue(WEAR_SWIPE_DOMINANCE > 1f)
    }
}
