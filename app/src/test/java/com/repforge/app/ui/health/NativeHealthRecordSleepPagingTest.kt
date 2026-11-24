package com.repforge.app.ui.health

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.HealthJournalStore
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset
import com.repforge.app.erase
import com.repforge.app.importRecords
import com.repforge.app.page

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeHealthRecordSleepPagingTest {
    @Test fun duplicateSdkParentIdsDoNotFillOrSplitSleepPages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        HealthJournalStore(context).use { store ->
            store.erase()
            try {
                val day = LocalDate.parse("2026-11-02")
                val midnight = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val data = JSONArray()
                repeat(30) { session ->
                    val start = midnight + session * 10L * 60_000L
                    val end = start + 8 * 60_000L
                    val stages = JSONArray().put(JSONObject().put("startMs", start).put("endMs", end)
                        .put("stage", "LIGHT").put("isAsleep", true))
                    repeat(2) { duplicate ->
                        data.put(JSONObject()
                            .put("channel", HealthJournalStore.SAMSUNG)
                            .put("source", "samsung.health")
                            .put("type", "SleepSessionRecord")
                            .put("recordId", "source-${session}-${duplicate}")
                            .put("lastModifiedMs", end)
                            .put("startMs", start).put("endMs", end)
                            .put("day", day.toString())
                            .put("data", JSONObject().put("durationMinutes", 8.0).put("stages", stages)))
                    }
                }
                store.importRecords(data, store.generation())
                val reader = NativeHealthRecords(store)
                val first = reader.load(HealthRecordQuery(day, HealthJournalStore.SAMSUNG,
                    type = "SleepSessionRecord"))
                assertEquals(25, first.rows.size)
                assertNotNull(first.next)
                val second = reader.load(HealthRecordQuery(day, HealthJournalStore.SAMSUNG,
                    type = "SleepSessionRecord", cursor = first.next, generation = first.generation))
                assertEquals(5, second.rows.size)
                assertNull(second.next)
                assertEquals(30, (first.rows + second.rows).map { it.startMs }.distinct().size)
                // The source journal still has all 60 original records for audit/recovery.
                assertEquals(60, store.page(day.toString(), day.toString(),
                    HealthJournalStore.SAMSUNG, "", 0, 0, 50, "SleepSessionRecord", false)
                    .getJSONArray("records").length() +
                    store.page(day.toString(), day.toString(), HealthJournalStore.SAMSUNG, "",
                        first.next!!.id, first.next!!.time, 50, "SleepSessionRecord", false)
                        .getJSONArray("records").length())
            } finally {
                store.erase()
            }
        }
    }
}
