package com.repforge.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Exercises the actual repository/HTTP error parser against scripted Google responses. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleCalendarSyncRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val prefs get() = context.getSharedPreferences("repforge-google", Context.MODE_PRIVATE)
    private lateinit var db: TrainPilotDatabase
    private var previousDb: TrainPilotDatabase? = null
    private val singleton = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
    private val description = "RepForge managed workout calendar v1"
    private val root = "/calendar/v3/calendars"
    private val eventId = "rf" + "a".repeat(64)
    private val steps = ArrayDeque<Step>()
    private val calls = mutableListOf<Call>()
    private lateinit var repository: GoogleSyncRepository

    private data class Step(val method: String, val path: String, val code: Int, val json: String)
    private data class Call(val method: String, val path: String, val body: String)

    @Before fun setUp() {
        previousDb = singleton.get(null) as? TrainPilotDatabase
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        singleton.set(null, db)
        prefs.edit().clear().putString("sub", "account-sub").putString("email", "owner@example.test")
            .putString("profile", "{\"sub\":\"account-sub\",\"email\":\"owner@example.test\"}")
            .putString("drive-sentinel", "keep-cloud-backup-reference").commit()
        repository = GoogleSyncRepository(context, NativeGoogleOperationGate()) { url -> fakeConnection(url) }
        expect("GET", "/oauth2/v3/userinfo", json = "{\"sub\":\"account-sub\",\"email\":\"owner@example.test\"}")
    }

    @After fun tearDown() {
        singleton.set(null, null)
        db.close()
    }

    private fun expect(method: String, path: String, code: Int = 200, json: String = "{}") {
        steps.addLast(Step(method, path, code, if (code >= 400)
            "{\"error\":{\"errors\":[{\"reason\":\"fixture-error\"}]}}" else json))
    }
    private fun calendar() = "{\"description\":\"$description\"}"
    private fun listed(id: String) = "{\"id\":\"$id\",\"description\":\"$description\",\"accessRole\":\"owner\"}"
    private fun cache(id: String = "live") { prefs.edit().putString("calendar", id).commit() }
    private fun live() { cache(); expect("GET", "$root/live", json = calendar()) }
    private fun entry(status: String = "planned", id: String = "schedule", googleId: String = eventId) = CalendarScheduleEntity(
        id, googleId, "2026-11-04", "2026-11-04T18:00:00Z", "2026-11-04T19:00:00Z",
        "program", "Original program", "day", "Original day", status, 42L,
        "{\"provider\":\"original\",\"note\":\"preserve this\"}")
    private fun save(vararg rows: CalendarScheduleEntity) = runBlocking {
        db.calendarScheduleDao().upsertEntries(rows.toList())
        db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("unrelated", "{\"keep\":true}", 1L))
    }
    private fun sync() = runBlocking { repository.syncCalendarCloud("fixture-token") }
    private fun assertPreserved(vararg rows: CalendarScheduleEntity) = runBlocking {
        assertEquals(rows.toSet(), db.calendarScheduleDao().getAllEntries().toSet())
        assertEquals("account-sub", prefs.getString("sub", null))
        assertEquals("owner@example.test", prefs.getString("email", null))
        assertEquals("keep-cloud-backup-reference", prefs.getString("drive-sentinel", null))
        assertEquals("{\"keep\":true}", db.keyValueDocumentDao().getDocument("unrelated")?.json)
        assertTrue("Every expected Google request must be consumed", steps.isEmpty())
        assertFalse(calls.any { it.method == "DELETE" && !it.path.contains("/events/") })
    }

    @Test fun missingCachedCalendarIsNotReselectedFromStaleListAndPaginationFindsLiveCalendar() {
        val row = entry(); save(row); cache("stale")
        expect("GET", "$root/stale", 404)
        expect("GET", "/calendar/v3/users/me/calendarList?maxResults=250", json =
            "{\"items\":[${listed("stale")}],\"nextPageToken\":\"page two\"}")
        expect("GET", "/calendar/v3/users/me/calendarList?maxResults=250&pageToken=page+two", json =
            "{\"items\":[${listed("live")}]}")
        expect("GET", "$root/live", json = calendar())
        expect("POST", "$root/live/events?sendUpdates=none", 201)
        assertEquals(1, sync().getOrThrow())
        assertEquals("live", prefs.getString("calendar", null))
        assertEquals(1, calls.count { it.path == "$root/stale" })
        assertPreserved(row)
    }

    @Test fun unavailableListCandidateIsValidatedBeforeUseAndNextCandidateIsUsed() {
        save()
        expect("GET", "/calendar/v3/users/me/calendarList?maxResults=250", json =
            "{\"items\":[${listed("stale")},${listed("live")}]}")
        expect("GET", "$root/stale", 410)
        expect("GET", "$root/live", json = calendar())
        assertEquals(0, sync().getOrThrow())
        assertEquals("live", prefs.getString("calendar", null))
        assertPreserved()
    }

    @Test fun missingCalendarCreatesOnceAndRepeatedSyncUsesSameEventIdWithoutDuplicateInsert() {
        val row = entry(); save(row); cache("gone")
        expect("GET", "$root/gone", 410)
        expect("GET", "/calendar/v3/users/me/calendarList?maxResults=250")
        expect("POST", root, 201, "{\"id\":\"new\"}")
        expect("POST", "$root/new/events?sendUpdates=none", 201)
        assertEquals(1, sync().getOrThrow())
        expect("GET", "/oauth2/v3/userinfo", json = "{\"sub\":\"account-sub\",\"email\":\"owner@example.test\"}")
        expect("GET", "$root/new", json = calendar())
        // The confirmed payload is unchanged: no second remote write is necessary.
        assertEquals(1, sync().getOrThrow())
        assertEquals(1, calls.count { it.method == "POST" && it.path == root })
        assertEquals(setOf(eventId), calls.filter { it.path.contains("/events") }.map { JSONObject(it.body).getString("id") }.toSet())
        assertPreserved(row)
    }

    @Test fun failedReplacementKeepsOldReferenceIdentityAndLocalData() {
        val row = entry(); save(row); cache("gone")
        expect("GET", "$root/gone", 404)
        expect("GET", "/calendar/v3/users/me/calendarList?maxResults=250")
        expect("POST", root, 403)
        assertTrue(sync().isFailure)
        assertEquals("gone", prefs.getString("calendar", null))
        assertEquals("", repository.uiState.value.lastCalendarSyncIso)
        assertPreserved(row)
    }

    @Test fun permissionFailureDoesNotTreatCalendarAsMissingOrCreateReplacement() {
        val row = entry(); save(row); cache()
        expect("GET", "$root/live", 403)
        assertTrue(sync().isFailure)
        assertEquals("live", prefs.getString("calendar", null))
        assertEquals("", repository.uiState.value.lastCalendarSyncIso)
        assertPreserved(row)
    }

    @Test fun eventPermissionFailureDoesNotRecoverInsertOrReportSuccess() {
        val row = entry(); save(row); live()
        expect("POST", "$root/live/events?sendUpdates=none", 409)
        expect("PUT", "$root/live/events/$eventId?sendUpdates=none", 403)
        assertTrue(sync().isFailure)
        assertEquals(1, calls.count { it.method == "POST" })
        assertEquals("", repository.uiState.value.lastCalendarSyncIso)
        assertPreserved(row)
    }

    @Test fun mismatchedCalendarDescriptionCannotWriteEventsOrReplaceCalendar() {
        val row = entry(); save(row); cache()
        expect("GET", "$root/live", json = "{\"description\":\"personal calendar\"}")
        assertTrue(sync().isFailure)
        assertEquals("live", prefs.getString("calendar", null))
        assertPreserved(row)
    }

    @Test fun missingEventAfterInsertConflictRetriesSameIdOnlyAfterCalendarVerification() {
        val row = entry(); save(row); live()
        expect("POST", "$root/live/events?sendUpdates=none", 409)
        expect("PUT", "$root/live/events/$eventId?sendUpdates=none", 404)
        expect("GET", "$root/live", json = calendar())
        expect("POST", "$root/live/events?sendUpdates=none", 201)
        assertEquals(1, sync().getOrThrow())
        val writes = calls.filter { it.path.contains("/events") }
        assertEquals(3, writes.size)
        assertEquals(1, writes.map { it.body }.distinct().size)
        assertEquals(eventId, JSONObject(writes.first().body).getString("id"))
        assertPreserved(row)
    }

    @Test fun calendarDisappearingDuringWriteDoesNotCreateAnotherCalendarOrReportSuccess() {
        val row = entry(); save(row); live()
        expect("POST", "$root/live/events?sendUpdates=none", 409)
        expect("PUT", "$root/live/events/$eventId?sendUpdates=none", 410)
        expect("GET", "$root/live", 404)
        val error = sync().exceptionOrNull()!!
        assertTrue(error.message.orEmpty().contains("404; GET calendar"))
        assertEquals("", repository.uiState.value.lastCalendarSyncIso)
        assertEquals("live", prefs.getString("calendar", null))
        assertPreserved(row)
    }

    @Test fun repeatedEventConflictAndMissingIsBoundedAndDiagnosesExactOperationWithoutIds() {
        val row = entry(); save(row); live()
        expect("POST", "$root/live/events?sendUpdates=none", 409)
        expect("PUT", "$root/live/events/$eventId?sendUpdates=none", 404)
        expect("GET", "$root/live", json = calendar())
        expect("POST", "$root/live/events?sendUpdates=none", 409)
        expect("PUT", "$root/live/events/$eventId?sendUpdates=none", 404)
        val message = sync().exceptionOrNull()!!.message.orEmpty()
        assertTrue(message.contains("404; PUT calendar event"))
        assertFalse(message.contains(eventId))
        assertFalse(message.contains("fixture-token"))
        assertEquals("", repository.uiState.value.lastCalendarSyncIso)
        assertPreserved(row)
    }

    @Test fun missingSkippedEventsAreAlreadyDeletedAndDoNotCreateEvents() {
        val first = entry("skipped")
        val second = entry("skipped", "other", "rf" + "b".repeat(64))
        save(first, second); live()
        // DAO order is intentionally not part of the contract.
        val ids = runBlocking { db.calendarScheduleDao().getAllEntries().map { it.googleEventId } }
        expect("DELETE", "$root/live/events/${ids[0]}", 404)
        expect("DELETE", "$root/live/events/${ids[1]}", 410)
        assertEquals(2, sync().getOrThrow())
        assertPreserved(first, second)
    }

    @Test fun duplicateLocalRowsWithSameGoogleEventIdProduceOneRemoteWrite() {
        val first = entry(id = "first")
        val second = entry(id = "second")
        save(first, second); live()
        expect("POST", "$root/live/events?sendUpdates=none", 201)
        assertEquals(1, sync().getOrThrow())
        assertEquals(1, calls.count { it.path.contains("/events") })
        assertEquals(1, repository.uiState.value.lastCalendarSyncCount)
        assertPreserved(first, second)
    }

    @Test fun skippedDuplicateCannotDeleteAnActiveEventWithSameGoogleId() {
        val cancelled = entry("skipped", "old")
        val active = entry("planned", "current")
        save(cancelled, active); live()
        expect("POST", "$root/live/events?sendUpdates=none", 201)
        assertEquals(1, sync().getOrThrow())
        assertEquals(0, calls.count { it.method == "DELETE" })
        assertEquals(1, calls.count { it.method == "POST" })
        assertPreserved(cancelled, active)
    }

    @Test fun unchangedSecondSyncDoesNotSendEventAgain() {
        val row = entry(); save(row); live()
        expect("POST", "$root/live/events?sendUpdates=none", 201)
        assertEquals(1, sync().getOrThrow())
        expect("GET", "/oauth2/v3/userinfo", json = JSONObject().put("sub", "account-sub").put("email", "owner@example.test").toString())
        expect("GET", "$root/live", json = calendar())
        assertEquals(1, sync().getOrThrow())
        assertEquals(1, calls.count { it.path.contains("/events") })
        assertPreserved(row)
    }

    @Test fun diagnosticLabelsContainNoRemoteIdentifiersAndRetryPolicyRemainsIdempotent() {
        assertEquals("PUT calendar event", GoogleRequestPolicy.calendarOperation("PUT",
            "https://www.googleapis.com$root/private%40example.test/events/$eventId?access_token=secret"))
        assertNull(GoogleRequestPolicy.calendarOperation("GET", "https://example.test$root/private"))
        assertFalse(GoogleRequestPolicy.canRetry("POST", "https://www.googleapis.com$root", null))
        assertTrue(GoogleRequestPolicy.canRetry("POST", "https://www.googleapis.com$root/live/events?sendUpdates=none", eventId))
        assertFalse(GoogleRequestPolicy.transientStatus(404))
    }

    private fun fakeConnection(url: URL): HttpURLConnection = object : HttpURLConnection(url) {
        private val output = ByteArrayOutputStream()
        private var response: Step? = null
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getOutputStream() = output
        override fun getResponseCode(): Int {
            if (response == null) {
                val path = url.file
                val expected = steps.removeFirstOrNull() ?: throw AssertionError("Unexpected $requestMethod $path")
                assertEquals(expected.method, requestMethod)
                assertEquals(expected.path, path)
                calls.add(Call(requestMethod, path, output.toString("UTF-8")))
                response = expected
            }
            return response!!.code
        }
        override fun getInputStream() = ByteArrayInputStream(response!!.json.toByteArray(Charsets.UTF_8))
        override fun getErrorStream() = ByteArrayInputStream(response!!.json.toByteArray(Charsets.UTF_8))
    }
}
