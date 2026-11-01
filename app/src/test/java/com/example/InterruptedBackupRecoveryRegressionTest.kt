package com.example

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.BackupRestoreManager
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeReplacementJournal
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Reproduction: a process dies after fencing a restore, but before/after the redo receipt. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class InterruptedBackupRecoveryRegressionTest {

    /** Keep Android Main available while recovery suspends on IO and returns to Main.
     * runBlocking on the test/main thread deadlocks the real Main dispatcher.
     * Do not replace Dispatchers.Main with a background dispatcher: execution checks its looper.
     */
    private fun <T> awaitNative(operation: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { operation() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try {
                    return future.get(10, TimeUnit.MILLISECONDS)
                } catch (waiting: TimeoutException) {
                    check(System.nanoTime() < deadline) { "Native recovery did not finish within 30 seconds" }
                }
            }
        } finally {
            future.cancel(true)
            worker.shutdownNow()
        }
    }

    @Test fun explicitResumeRetiresOrphanFenceWithoutReimportingOrErasingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(NativeReplacementJournal(context).exists())
        val backup = File(context.filesDir, "user-selected-backup.json").apply { writeText("original backup bytes") }
        val photo = File(context.filesDir, "user-photo.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val dao = TrainPilotDatabase.getInstance(context).keyValueDocumentDao()
        val row = KeyValueDocumentEntity("recovery-regression-sentinel", "{\"retained\":true}", 123L)
        awaitNative { dao.putDocument(row) }

        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().putBoolean("blocked", true).commit()
        val execution = PhoneWorkoutExecution.store(context)
        execution.setRestoreBlocked(true)

        val result = awaitNative { BackupRestoreManager(context).resumeInterruptedReplacement() }

        assertTrue(result.exceptionOrNull()?.message ?: "recovery failed", result.isSuccess)
        assertFalse(NativeDataReplacement.blocked(context))
        assertFalse(execution.restoreBlocked())
        assertFalse(NativeReplacementJournal(context).exists())
        assertEquals(row, awaitNative { dao.getDocument(row.key) })
        assertEquals("original backup bytes", backup.readText())
        assertArrayEquals(byteArrayOf(1, 2, 3), photo.readBytes())
    }

    @Test fun executionOnlyOrphanFenceCanAlsoBeRetired() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(NativeDataReplacement.blocked(context))
        val execution = PhoneWorkoutExecution.store(context)
        execution.setRestoreBlocked(true)

        val result = awaitNative { BackupRestoreManager(context).resumeInterruptedReplacement() }

        assertTrue(result.exceptionOrNull()?.message ?: "recovery failed", result.isSuccess)
        assertFalse(execution.restoreBlocked())
        assertFalse(NativeDataReplacement.blocked(context))
    }

    @Test fun resumeWithoutInterruptedReplacementReportsFailure() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = awaitNative { BackupRestoreManager(context).resumeInterruptedReplacement() }
        assertTrue(result.isFailure)
        assertFalse(NativeDataReplacement.blocked(context))
        assertFalse(NativeReplacementJournal(context).exists())
    }

    @Test fun damagedExistingReceiptIsNotSilentlyCleared() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receipt = File(context.noBackupFilesDir, "native-data-replacement.json")
        receipt.writeText("{ damaged receipt")
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().putBoolean("blocked", true).commit()

        try {
            val result = awaitNative { BackupRestoreManager(context).resumeInterruptedReplacement() }
            assertTrue(result.isFailure)
            assertTrue(NativeDataReplacement.blocked(context))
            assertTrue(NativeReplacementJournal(context).exists())
            assertEquals("{ damaged receipt", receipt.readText())
        } finally {
            receipt.delete() // Only the isolated Robolectric test's fabricated receipt.
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
                .edit().remove("blocked").commit()
        }
    }
}
