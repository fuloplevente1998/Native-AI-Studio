package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearClosureRecoveryTest {
    private fun local() = JSONObject().put("commandId", "discard-one").put("workoutId", "old")
        .put("action", "discardWorkout").put("status", "pending")
    private fun receipt(status: String) = JSONObject(local().toString()).put("schema", 1)
        .put("sequence", 17).put("status", status)
    private fun closure(status: String = "pending") = WearClosure("discard-one", "old", "discardWorkout", status, "", 10, 0, 0)

    @Test fun rejectionAndNonterminalReceiptsExposeErrorInsteadOfEndlessWaiting() {
        for (status in listOf("superseded", "processed", "applied", "error")) {
            val result = wearClosureReceipt(local(), receipt(status))!!
            assertEquals("error", result.getString("status"))
            assertTrue(result.getString("message").isNotBlank())
            assertEquals("pending", local().getString("status"))
        }
    }
    @Test fun onlyTheMatchingTerminalActionConfirmsCompletion() {
        assertEquals("discarded", wearClosureReceipt(local(), receipt("discarded"))!!.getString("status"))
        assertEquals("error", wearClosureReceipt(local(), receipt("saved"))!!.getString("status"))
        val finish = local().put("action", "finishWorkout")
        assertEquals("saved", wearClosureReceipt(finish, receipt("saved").put("action", "finishWorkout"))!!.getString("status"))
    }
    @Test fun foreignMalformedAndWrongSequenceReceiptsCannotChangeClosure() {
        for ((key, value) in listOf("schema" to 2, "commandId" to "other", "workoutId" to "new",
            "action" to "finishWorkout", "sequence" to 0, "status" to "unknown"))
            assertNull(key, wearClosureReceipt(local(), receipt("discarded").put(key, value)))
        val queued = local().put("sequence", 17)
        assertNull(WearSaveSyncModel.accept(org.json.JSONArray().put(queued), org.json.JSONArray(),
            receipt("discarded").put("sequence", 18)))
    }
    @Test fun lostReceiptAndTransportFailureExposeRetryWithoutClaimingDeletion() {
        for (reason in listOf("timeout", "transport", "local")) {
            val result = wearClosureDelivery(closure(), WearSaveSyncState("old", WearSavePhase.ERROR, retryable = true, reason = reason))!!
            assertEquals("error", result.status)
            assertTrue(result.message.isNotBlank())
            assertEquals("discard-one", result.commandId)
        }
    }
    @Test fun orphanedClosureAfterRestartIsRecoverableAndDeliveryAloneIsNotSuccess() {
        assertEquals("error", wearClosureDelivery(closure(), null)!!.status)
        assertEquals("error", wearClosureDelivery(closure(), WearSaveSyncState("old", WearSavePhase.SYNCED))!!.status)
        for (phase in listOf(WearSavePhase.LOCAL, WearSavePhase.SAVING, WearSavePhase.WAITING))
            assertEquals("pending", wearClosureDelivery(closure(), WearSaveSyncState("old", phase))!!.status)
        assertEquals("pending", wearClosureDelivery(closure(), WearSaveSyncState("new", WearSavePhase.ERROR))!!.status)
    }
    @Test fun confirmedDeletionIsNeverReplacedByUnrelatedDeliveryErrors() {
        assertEquals("discarded", wearClosureDelivery(closure("discarded"), WearSaveSyncState("old", WearSavePhase.ERROR))!!.status)
        assertEquals("saved", wearClosureDelivery(closure("saved"), null)!!.status)
    }
    @Test fun homeDescribesPendingDiscardAsConfirmationAndKeepsItInactive() {
        val home = wearHomeDashboardState(null, null, closure(), null, false, 10000)
        assertEquals("Elvetés visszaigazolása", home.title)
        assertFalse(home.active)
        assertEquals(HomeAction.RESULT, home.action)
    }
}
