package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.BackupRestoreManager
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeReplacementJournal
import com.repforge.app.PhoneWorkoutExecution
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Reproduction: a process dies after fencing a restore, but before/after the redo receipt. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InterruptedBackupRecoveryRegressionTest {

    @Test fun explicitResumeRetiresOrphanFenceWithoutReimportingOrErasingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(NativeReplacementJournal(context).exists())

        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().putBoolean("blocked", true).commit()
        val execution = PhoneWorkoutExecution.store(context)
        execution.setRestoreBlocked(true)

        val result = BackupRestoreManager(context).resumeInterruptedReplacement()

        assertTrue(result.exceptionOrNull()?.message ?: "recovery failed", result.isSuccess)
        assertFalse(NativeDataReplacement.blocked(context))
        assertFalse(execution.restoreBlocked())
        assertFalse(NativeReplacementJournal(context).exists())
    }

    @Test fun damagedExistingReceiptIsNotSilentlyCleared() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receipt = File(context.noBackupFilesDir, "native-data-replacement.json")
        receipt.writeText("{ damaged receipt")
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().putBoolean("blocked", true).commit()

        try {
            val result = BackupRestoreManager(context).resumeInterruptedReplacement()
            assertTrue(result.isFailure)
            assertTrue(NativeDataReplacement.blocked(context))
            assertTrue(NativeReplacementJournal(context).exists())
        } finally {
            receipt.delete() // Only the isolated Robolectric test's fabricated receipt.
            context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
                .edit().remove("blocked").commit()
        }
    }
}
