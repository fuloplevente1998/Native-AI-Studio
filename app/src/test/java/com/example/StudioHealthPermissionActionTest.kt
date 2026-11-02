package com.example

import com.example.ui.components.runStudioHealthPermissionAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class StudioHealthPermissionActionTest {
    @Test fun grantedAccessSynchronizesWithoutOpeningConsent() = runBlocking {
        val events = mutableListOf<String>()
        runStudioHealthPermissionAction({ events += "actual-grant"; true },
            { error("Repeated permission dialog") }, { events += "guard" }, { events += "sync" })
        assertEquals(1, events.count { it == "sync" })
        assertEquals(1, events.count { it == "actual-grant" })
    }
    @Test fun newlyGrantedAccessIsCheckedAgainBeforeSync() = runBlocking {
        var allowed = false; var requests = 0; var syncs = 0; var reads = 0
        runStudioHealthPermissionAction({ reads++; allowed }, { requests++; allowed = true }, {}, { syncs++ })
        assertEquals(1, requests); assertEquals(2, reads); assertEquals(1, syncs)
    }
    @Test fun successfulDialogWithoutAnActualGrantCannotRead() = runBlocking {
        var requests = 0
        try {
            runStudioHealthPermissionAction({ false }, { requests++ }, {}, { error("Unauthorized sync") })
            fail("Denied consent hidden")
        } catch (_: IllegalStateException) { }
        assertEquals(1, requests)
    }
    @Test fun sceneLossDuringConsentAndSdkErrorsDoNotFallBackOrRead() = runBlocking {
        var resumed = true
        try {
            runStudioHealthPermissionAction({ false }, { resumed = false }, { check(resumed) }, { error("Obsolete sync") })
            fail("Scene loss ignored")
        } catch (_: IllegalStateException) { }
        try {
            runStudioHealthPermissionAction({ throw UnsupportedOperationException("SDK unavailable") },
                { error("Consent cannot fix unavailable SDK") }, {}, { error("Fallback forbidden") })
            fail("SDK failure hidden")
        } catch (_: UnsupportedOperationException) { }
    }
}
