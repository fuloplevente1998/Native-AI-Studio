package com.repforge.app.ui.health

import android.content.Context
import android.os.Looper
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeHealthBackupGate
import com.repforge.app.NativeDataReplacement
import com.repforge.app.PhoneOwnership193
import com.repforge.app.PhoneWorkoutExecution
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Split the foreground admission by the real thread ownership of its dependencies.
 *
 * PhoneWorkoutExecution.store() is canonically main-looper-only. Calling it from an
 * SDK/IO publisher catches "Execution requires main looper" and rejects EVERY read.
 * The other conditions are checked on IO, including again under the short commit lease.
 */
internal object NativeHealthForegroundAdmission {
    private val ownershipTransitions = setOf(
        "requested", "offered", "incoming", "accepted", "committed", "installed"
    )

    internal fun eligible(
        replacementReady: Boolean,
        executionReady: Boolean,
        ownershipStage: String,
        frames: List<JSONObject?>
    ): Boolean =
        replacementReady && executionReady && ownershipStage !in ownershipTransitions &&
            frameBlock(frames) == null

    internal enum class Block(val message: String) {
        RESTORE("Adat-visszaállítás vagy adatcsere van folyamatban. Várd meg a befejezését, majd frissíts újra."),
        BACKUP("Biztonsági mentés van folyamatban. A napi szinkron utána indítható."),
        HANDOVER("Az órás átadás/visszavétel még nincs lezárva. Várd meg a visszaigazolást."),
        SUSPENDED("Az edzésvezérlés egy mentési, elvetési vagy órás átadási művelet miatt foglalt. Fejezd be a műveletet az Edzés oldalon, vagy várd meg az átadás visszaigazolását."),
        FINALIZING("Az edzés mentése vagy elvetése még nincs lezárva. Fejezd be a műveletet az edzés képernyőjén."),
        ACTIVE("Folyamatban lévő vagy szüneteltetett edzés van. A teljes napi szinkron az edzés lezárása után indítható."),
        UNREADABLE("A natív edzés- vagy adatcsereállapot nem olvasható. A szinkron biztonsági okból nem indult el.")
    }

    /** Project out only an explicit inactive frame from an authoritative terminal source.
     * endExecution() / OwnershipJournal.close() deliberately retain the old flags for recovery.
     * No stored frame, recovery marker or measurement is deleted or rewritten here.
     */
    internal fun admissionFrame(frame: JSONObject?, terminalSource: Boolean): JSONObject? =
        frame?.takeUnless { terminalSource && it.opt("active") == false }

    private fun frameBlock(frames: List<JSONObject?>): Block? {
        if (frames.filterNotNull().any { it.optBoolean("finalizationPending") || it.optBoolean("discardPending") }) return Block.FINALIZING
        if (frames.filterNotNull().any { it.optBoolean("active") || it.optBoolean("workoutPaused") }) return Block.ACTIVE
        return null
    }

    /** No metadata lock, provider IPC or coroutine hop in this main-only section. */
    fun mainBlockReason(context: Context): Block? {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Foreground admission requires main looper" }
        return try {
            val execution = PhoneWorkoutExecution.store(context)
            if (execution.restoreBlocked()) Block.RESTORE
            else {
                val frameReason = frameBlock(listOf(admissionFrame(execution.snapshot(), terminalSource = true)))
                when {
                    frameReason == Block.FINALIZING -> frameReason
                    execution.commandsSuspended() -> Block.SUSPENDED
                    else -> frameReason
                }
            }
        } catch (_: Exception) { Block.UNREADABLE }
    }

    fun safeOnMain(context: Context): Boolean = mainBlockReason(context) == null

    /** Room/restore/backup/ownership checks are legal on an SDK reader's IO thread. */
    fun offMainBlockReason(context: Context): Block? = try {
        when {
            !NativeHealthBackupGate.available() -> Block.BACKUP
            NativeDataReplacement.blocked(context) -> Block.RESTORE
            HealthJournalStore.get(context).pendingRestore().isNotEmpty() -> Block.RESTORE
            else -> {
                val ownership = PhoneOwnership193.journal(context).read()
                val stage = ownership.optString("stage")
                if (stage in ownershipTransitions) Block.HANDOVER
                else frameBlock(listOf(ActiveWorkoutStore.current(context),
                    admissionFrame(ownership.optJSONObject("snapshot"), terminalSource = stage == "closed")))
            }
        }
    } catch (_: Exception) { Block.UNREADABLE }

    fun safeOffMain(context: Context): Boolean = offMainBlockReason(context) == null

    /** A suspended command engine alone cannot distinguish saving from ownership transfer.
     * Prefer the durable IO-side reason in that case; suspension itself always stays blocked. */
    internal fun combinedBlock(main: Block?, local: Block?): Block? =
        if (main == Block.SUSPENDED) local ?: main else main ?: local

    /** Outside ALL metadata locks; only the main-looper execution probe changes dispatcher. */
    suspend fun requireMainAdmission(context: Context) {
        val main = withContext(Dispatchers.Main.immediate) { mainBlockReason(context) }
        val reason = if (main == Block.SUSPENDED) withContext(Dispatchers.IO) {
            combinedBlock(main, offMainBlockReason(context))
        } else main
        check(reason == null) { reason!!.message }
    }

    /**
     * Actual HC/Samsung synchronous provider publication callback.
     *
     * The IO caller briefly hops to Main *before* acquiring the metadata mutex.
     * Once under the mutex, only the IO-safe ownership/restore checks are repeated,
     * and the writer is a local journal write (never SDK IPC).
     */
    fun publish(context: Context, requireIdentity: () -> Unit, writer: () -> Unit) {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Provider journal publication must run off the main thread"
        }
        runBlocking {
            requireMainAdmission(context)
            NativeDataReplacement.mutate(context) {
                NativeHealthBackupGate.publish {
                    check(safeOffMain(context)) {
                        offMainBlockReason(context)?.message ?: "Az egészségadatok közzététele közben az állapot megváltozott."
                    }
                    requireIdentity()
                    writer()
                }
            }
        }
    }
}
