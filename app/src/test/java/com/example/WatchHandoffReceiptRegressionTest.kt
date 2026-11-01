package com.example

import com.repforge.app.NativeWatchHandoffReceipt
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WatchHandoffReceiptRegressionTest {
    private fun frame(owner: String = "watch") = JSONObject().put("active", true).put("workoutId", "current")
        .put("timerOwner", owner).put("ownershipProtocol193", 1).put("ownerEpoch193", 3)
        .put("handoff193", JSONObject().put("state", "committed").put("id", "transfer").put("epoch", 3).put("target", owner))
    private fun journal(stage: String, owner: String = "watch") = JSONObject().put("stage", stage)
        .put("request", JSONObject().put("id", "transfer").put("workoutId", "current")).put("snapshot", frame(owner))

    @Test fun sourceCommitIsNotAnInstalledWatchAcknowledgement() {
        assertFalse(NativeWatchHandoffReceipt.confirmed(frame(), journal("committed"), "current", "watch"))
        assertTrue(NativeWatchHandoffReceipt.pending(frame(), journal("committed")))
        assertTrue(NativeWatchHandoffReceipt.confirmed(frame(), journal("settled"), "current", "watch"))
        assertFalse(NativeWatchHandoffReceipt.pending(frame(), journal("settled")))
    }

    @Test fun oldWorkoutTransferOrEpochCannotConfirmCurrentHandoff() {
        assertFalse(NativeWatchHandoffReceipt.confirmed(frame(), journal("settled"), "other", "watch"))
        assertFalse(NativeWatchHandoffReceipt.confirmed(frame(), journal("settled").apply {
            getJSONObject("request").put("id", "old-transfer") }, "current", "watch"))
        assertFalse(NativeWatchHandoffReceipt.confirmed(frame(), journal("settled").apply {
            getJSONObject("snapshot").put("ownerEpoch193", 2) }, "current", "watch"))
        assertFalse(NativeWatchHandoffReceipt.confirmed(frame().put("active", false), journal("settled"), "current", "watch"))
    }

    @Test fun phoneReturnRequiresInstalledNativeReceiptAndUnreadableStateStaysBlocked() {
        assertFalse(NativeWatchHandoffReceipt.confirmed(frame("phone"), journal("committed", "phone"), "current", "phone"))
        assertTrue(NativeWatchHandoffReceipt.confirmed(frame("phone"), journal("installed", "phone"), "current", "phone"))
        assertTrue(NativeWatchHandoffReceipt.pending(frame(), JSONObject().put("stage", "unreadable")))
    }
    @Test fun expiredUnofferedRequestCanRetryButDurableTransferNeverUnlocksOnTimeout() {
        val request = journal("requested").apply { getJSONObject("request").put("createdAt", 1_000) }
        assertTrue(NativeWatchHandoffReceipt.pending(frame(), request, now = 10_000))
        assertFalse(NativeWatchHandoffReceipt.pending(frame(), request, now = 32_000))
        request.put("stage", "offered")
        assertTrue(NativeWatchHandoffReceipt.pending(frame(), request, now = 32_000))
        request.put("stage", "committed")
        assertTrue(NativeWatchHandoffReceipt.pending(frame(), request, now = 100_000))
        request.put("stage", "requested")
        val offered = frame().apply { getJSONObject("handoff193").put("state", "offered") }
        assertTrue(NativeWatchHandoffReceipt.pending(offered, request, now = 100_000))
    }

}
