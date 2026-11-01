package com.example

import com.repforge.app.BackupJsonIO
import com.repforge.app.BackupRestoreManager
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupJsonMemoryRegressionTest {
    /** A whole-document serializer/parser would call these and fail immediately. */
    private class NoStringObject : JSONObject() {
        override fun toString(): String = error("Whole backup must not be serialized in memory")
    }

    @Test fun largeUnicodeBackupStreamsAndNormalizationSharesHealthTree() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val records = JSONArray()
        val text = "árvíztűrő 🏋️ ".repeat(300)
        repeat(2000) { records.put(JSONObject().put("id", it).put("text", text)) }
        val health = NoStringObject().apply { put("records", records) }
        val root = NoStringObject().apply {
            put("history", JSONArray()); put("nativeHealthJournal", health)
        }
        val normalized = BackupRestoreManager(context).normalizeLegacyRepForgePayload(root)
        assertSame(health, normalized.getJSONObject("nativeHealthJournal"))
        val file = File(context.cacheDir, "large-unicode-backup.json")
        BackupJsonIO.writeFile(file, normalized, BackupRestoreManager.JSON_LIMIT)
        assertTrue(file.length() > 10 * 1024 * 1024)
        val restored = BackupJsonIO.readFile(file, BackupRestoreManager.JSON_LIMIT) as JSONObject
        val values = restored.getJSONObject("nativeHealthJournal").getJSONArray("records")
        assertEquals(2000, values.length())
        assertEquals(text, values.getJSONObject(1999).getString("text"))
        file.delete()
    }

    @Test fun streamingLimitsRejectBeforeReplacementAndRejectTrailingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "oversized-backup.json")
        try {
            BackupJsonIO.writeFile(file, JSONObject().put("data", "x".repeat(10000)), 100)
            fail("Expected size limit")
        } catch (_: IOException) { }
        assertFalse(com.repforge.app.NativeReplacementJournal(context).exists())
        try {
            BackupJsonIO.read("{} {}".reader()); fail("Expected trailing data rejection")
        } catch (_: IOException) { }
        file.delete()
    }
    @Test fun multiMegabyteHealthRestoreStagesInstallsAndRollsBackWithoutWholeSnapshotStrings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = com.repforge.app.HealthJournalStore(context)
        store.erase()
        val records = JSONArray()
        val stamp = 1793527200000L
        repeat(600) { index ->
            records.put(JSONObject().put("channel", com.repforge.app.HealthJournalStore.HC).put("source", "com.example.fixture")
                .put("type", "StepsRecord").put("recordId", "record-$index")
                .put("lastModifiedMs", stamp).put("startMs", stamp).put("endMs", stamp + 1000)
                .put("day", com.repforge.app.HealthJournalStore.day(stamp))
                .put("data", JSONObject().put("value", index).put("notes", "árvíz 🏋️ ".repeat(1000))))
        }
        val snapshot = NoStringObject().apply {
            put("schemaVersion", 1); put("records", records); put("days", JSONArray()); put("deletions", JSONArray())
        }
        val token = store.prepareRestore(snapshot)
        store.close()
        val reopened = com.repforge.app.HealthJournalStore(context)
        assertEquals(token, reopened.pendingRestore())
        reopened.installRestore(token)
        val installed = reopened.exportSnapshot().getJSONArray("records")
        assertEquals(600, installed.length())
        assertEquals(599, installed.getJSONObject(599).getJSONObject("data").getInt("value"))
        reopened.finishRestore(token, false)
        assertEquals(0, reopened.exportSnapshot().getJSONArray("records").length())
        reopened.readableDatabase.rawQuery("SELECT COUNT(*) FROM meta_parts WHERE key IN ('restore_old','restore_next')", null).use {
            assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
        }
        reopened.close()
    }

}
