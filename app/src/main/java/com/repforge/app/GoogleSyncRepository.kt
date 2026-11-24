package com.repforge.app

import androidx.activity.result.ActivityResult
import androidx.room.withTransaction
import com.repforge.app.ui.settings.NativeGoogleMaintenance
import android.accounts.Account
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.NativeBackupPrivacy
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.LegacySyncIdentity18
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

data class GoogleSyncUiState(
    val connectedEmail: String = "",
    val connectedName: String = "",
    val connectedSub: String = "",
    val isBusy: Boolean = false,
    val activeOperation: String = "",
    val stageMessage: String = "",
    val lastDriveSyncIso: String = "",
    val lastCalendarSyncIso: String = "",
    val lastCalendarSyncCount: Int? = null,
    val errorMessage: String? = null
)

internal enum class GoogleSyncAction(val key: String) { CONNECT("connect"), DRIVE("drive"), CALENDAR("calendar") }
internal data class GoogleSyncCompletion(val action: GoogleSyncAction, val syncedAt: String? = null, val count: Int? = null, val maintenance: NativeGoogleMaintenance.Report? = null)

/**
 * Native replacement of GoogleSyncPlugin + #18 multi-device Drive merge engine.
 * Access tokens are never written to prefs, backups, or logs; UI forwards only an opaque consent result.
 */
class GoogleSyncRepository internal constructor(
    context: Context,
    private val operations: NativeGoogleOperationGate,
    internal val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) {
    constructor(context: Context) : this(context, processOperations)

    companion object {
        private const val PREFS_NAME = "repforge-google"
        private val processOperations = NativeGoogleOperationGate()
        private val accountEpoch = java.util.concurrent.atomic.AtomicLong(0)
        internal const val LIMIT = 20 * 1024 * 1024
        internal const val PHOTO_LIMIT = 6 * 1024 * 1024
        internal const val MANAGED_CALENDAR_DESC = "RepForge managed workout calendar v1"
        internal val FILE_ID_REGEX = Regex("[A-Za-z0-9_-]+")
        internal val UUID_REGEX = Regex("[a-f0-9-]{36}")
        internal val SNAPSHOT_NAME_REGEX = Regex("^repforge-sync-([a-z0-9-]{36})-")
        internal val APP_FILE_REGEX = Regex("repforge-sync-[a-z0-9-]{36}-[A-Za-z0-9_-]{1,80}\\.json|trainpilot-photo-[a-f0-9-]{36}\\.jpg")
        private val EVENT_ID_REGEX = Regex("rf[0-9a-f]{64}")
    }

    internal val appContext = context.applicationContext
    private val database = TrainPilotDatabase.getInstance(appContext)
    internal val settingsStore = TrainPilotSettingsStore(appContext)
    private val migrator = LegacyWebViewMigrator(appContext, database, settingsStore)

    private val _uiState = MutableStateFlow(loadInitialUiState())
    val uiState: StateFlow<GoogleSyncUiState> = _uiState.asStateFlow()

    internal fun prefs(): SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadInitialUiState(): GoogleSyncUiState {
        val p = prefs()
        val rawProfile = p.getString("profile", "") ?: ""
        val profile = runCatching { if (rawProfile.isNotEmpty()) JSONObject(rawProfile) else null }.getOrNull()
        return GoogleSyncUiState(
            connectedEmail = profile?.optString("email").orEmpty(),
            connectedName = profile?.optString("name").orEmpty(),
            connectedSub = profile?.optString("sub").orEmpty(),
            lastDriveSyncIso = p.getString("lastDriveSyncIso", "").orEmpty(),
            lastCalendarSyncIso = p.getString("lastCalendarSyncIso", "").orEmpty(),
            lastCalendarSyncCount = if (p.contains("lastCalendarSyncCount")) p.getInt("lastCalendarSyncCount", 0) else null
        )
    }

    fun refreshProfileState() {
        val loaded = loadInitialUiState()
        _uiState.update { current ->
            if (current.connectedSub != loaded.connectedSub) loaded.copy(
                isBusy = current.isBusy, activeOperation = current.activeOperation,
                stageMessage = current.stageMessage, errorMessage = current.errorMessage)
            else current.copy(connectedEmail = loaded.connectedEmail,
                connectedName = loaded.connectedName, connectedSub = loaded.connectedSub,
                lastDriveSyncIso = current.lastDriveSyncIso.ifBlank { loaded.lastDriveSyncIso },
                lastCalendarSyncIso = current.lastCalendarSyncIso.ifBlank { loaded.lastCalendarSyncIso },
                lastCalendarSyncCount = current.lastCalendarSyncCount ?: loaded.lastCalendarSyncCount)
        }
    }

    private fun scopesForOperation(op: String): List<Scope> {
        val scopes = mutableListOf(
            Scope("openid"),
            Scope("https://www.googleapis.com/auth/userinfo.email"),
            Scope("https://www.googleapis.com/auth/userinfo.profile")
        )
        scopes.addAll(requiredGoogleCloudScopes(op).map(::Scope))
        return scopes
    }

    /** UI state is feedback only; atomic admission is shared by every repository instance. */
    private suspend fun <T> runGoogleOperation(op: String, stage: String, operation: suspend () -> T): Result<T> {
        return try {
            operations.run {
                // Drive sync stages a snapshot/merge outside the replacement mutex.
                // An unattended Health read must not race this cloud backup window.
                if (op == GoogleSyncAction.DRIVE.key || op == "authorize-drive") NativeHealthBackupGate.begin()
                try {
                    check(op !in setOf("drive", "calendar") || !prefs().getBoolean("maintenancePaused:$op", prefs().getBoolean("maintenancePaused", false))) {
                        "Cloud synchronization is paused after maintenance; explicitly authorize sync again"
                    }
                    refreshProfileState()
                    _uiState.update { it.copy(isBusy = true, activeOperation = op, stageMessage = stage, errorMessage = null) }
                    currentCoroutineContext().ensureActive()
                    val value = operation()
                    currentCoroutineContext().ensureActive()
                    Result.success(value)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (e: Exception) {
                    val message = formatError(e)
                    _uiState.update { it.copy(errorMessage = message) }
                    Result.failure(Exception(message, e))
                } finally {
                    _uiState.update { it.copy(isBusy = false, activeOperation = "", stageMessage = "") }
                    if (op == GoogleSyncAction.DRIVE.key || op == "authorize-drive") NativeHealthBackupGate.end()
                }
            }
        } catch (busy: NativeGoogleOperationGate.Busy) { Result.failure(busy) }
    }

    /** Hold the account-operation gate from authorization through consent, verified identity and I/O.
     * The UI resolver is cancellable and process-local; no replacement/workout fence is held here.
     */
    internal suspend fun authorizeAndRun(
        activity: Activity?,
        action: GoogleSyncAction,
        resolve: (suspend (PendingIntent) -> ActivityResult)?,
        recoverEarlier: Boolean = false,
        maintenance: NativeGoogleMaintenance.Request? = null
    ): Result<GoogleSyncCompletion> = withContext(Dispatchers.IO) {
        val language = settingsStore.current().language
        val strings = com.repforge.app.ui.text.nativeResources(appContext, language)
        runGoogleOperation(maintenance?.kind?.key ?: "authorize-${action.key}", strings.getString(R.string.native_google_authorizing)) {
            val dataTicket = maintenance?.dataset ?: NativeDataReplacement.continuationForCurrentData(appContext)
            if (maintenance != null) requireMaintenance(maintenance)
            val email = prefs().getString("email", "").orEmpty()
            if (action != GoogleSyncAction.CONNECT)
                NativeGoogleAccountPolicy.requireConnected(prefs().getString("sub", "").orEmpty(), email)
            val operation = maintenance?.kind?.key ?: action.key
            val result = try {
                val authorization = withContext(Dispatchers.Main) {
                    val builder = AuthorizationRequest.builder().setRequestedScopes(scopesForOperation(operation) +
                        if (maintenance?.kind == NativeGoogleMaintenance.Kind.REVOKE)
                            scopesForOperation("drive") + scopesForOperation("calendar") else emptyList())
                    if (email.isNotBlank()) builder.setAccount(Account(email, "com.google"))
                    (if (activity != null) Identity.getAuthorizationClient(activity) else Identity.getAuthorizationClient(appContext))
                        .authorize(builder.build()).await()
                }
                if (authorization.hasResolution()) {
                    // 2805: unattended sync never opens consent UI; the card asks for reconnecting instead.
                    if (resolve == null) throw IOException(strings.getString(R.string.native_google_auth_failed))
                    val pending = authorization.pendingIntent ?: throw IOException(strings.getString(R.string.native_google_launch_failed))
                    _uiState.update { it.copy(stageMessage = strings.getString(R.string.native_google_waiting)) }
                    val response = resolve(pending)
                    currentCoroutineContext().ensureActive()
                    parseGoogleConsentResult(response, strings) { data ->
                        Identity.getAuthorizationClient(appContext).getAuthorizationResultFromIntent(data)
                    }
                } else authorization
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: ApiException) {
                throw IOException(strings.getString(googleAuthorizationErrorMessage(error.statusCode)))
            } catch (_: RuntimeException) {
                throw IOException(strings.getString(R.string.native_google_auth_failed))
            }
            val token = result.accessToken?.takeIf { it.isNotBlank() }
                ?: throw IOException(strings.getString(R.string.native_google_no_token))
            if (!hasRequiredGoogleCloudScopes(operation, result.grantedScopes)) {
                throw IOException(strings.getString(R.string.native_google_missing_scopes))
            }
            currentCoroutineContext().ensureActive()
            dataTicket.requireCurrent(appContext)
            if (maintenance != null) {
                requireMaintenance(maintenance)
                verifyAndFetchProfile(token)
                requireMaintenance(maintenance)
                val report = performMaintenance(activity, token, maintenance)
                GoogleSyncCompletion(action, maintenance = report)
            } else {
                if (action != GoogleSyncAction.CONNECT) {
                    verifyAndFetchProfile(token)
                    dataTicket.requireCurrent(appContext)
                    check(prefs().edit().putBoolean("maintenancePaused:${action.key}", false).commit())
                }
                when (action) {
                    GoogleSyncAction.CONNECT -> {
                        connectWithTokenInternal(token, dataTicket)
                        GoogleSyncCompletion(action)
                    }
                    GoogleSyncAction.DRIVE -> {
                        _uiState.update { it.copy(stageMessage = "Google Drive szinkronizálása…") }
                        GoogleSyncCompletion(action,
                            syncedAt = syncDriveCloudInternal(token, false, recoverEarlier, true))
                    }
                    GoogleSyncAction.CALENDAR -> {
                        _uiState.update { it.copy(stageMessage = "Naptár szinkronizálása…") }
                        GoogleSyncCompletion(action, count = syncCalendarCloudInternal(token, dataTicket))
                    }
                }
            }
        }
    }

    /**
     * 2805: daily background Drive/Calendar sync with an already granted scope. Never prompts,
     * never merges archives and never runs during an active workout or a maintenance pause.
     */
    internal suspend fun runUnattended(action: GoogleSyncAction): Result<GoogleSyncCompletion> {
        require(action != GoogleSyncAction.CONNECT)
        return authorizeAndRun(null, action, null, recoverEarlier = false)
    }

    internal fun unattendedAllowed(action: GoogleSyncAction): Boolean {
        val p = prefs()
        return p.getString("sub", "").orEmpty().isNotBlank() &&
            !p.getBoolean("maintenancePaused:${action.key}", p.getBoolean("maintenancePaused", false))
    }

    suspend fun completeConnectWithToken(accessToken: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        runGoogleOperation("connect", "Google-fiók ellenőrzése…") {
            connectWithTokenInternal(accessToken)
        }
    }

    private suspend fun connectWithTokenInternal(accessToken: String,
        dataTicket: NativeDataReplacement.Continuation = NativeDataReplacement.continuationForCurrentData(appContext)): JSONObject {
        val profile = verifyAndFetchProfile(accessToken, connecting = true)
        currentCoroutineContext().ensureActive()
        return NativeDataReplacement.mutate(appContext) {
            dataTicket.requireCurrent(appContext)
            val safe = JSONObject().put("sub", profile.getString("sub"))
                .put("email", profile.getString("email")).put("name", profile.optString("name"))
            val editor = prefs().edit().putString("profile", safe.toString())
                .putString("email", safe.getString("email")).putString("sub", safe.getString("sub"))
            if (prefs().getString("sub", "") != safe.getString("sub")) editor.remove("calendar")
            check(editor.commit()) { "A Google-fiók nem menthető." }
            accountEpoch.incrementAndGet()
            refreshProfileState()
            safe
        }
    }

    fun disconnect(): Result<Unit> = try {
        operations.runNow {
            // Disconnect clears identity, not a fail-closed maintenance pause. Otherwise
            // reconnecting could silently resurrect deleted cloud data before explicit sync.
            val previous = prefs()
            val pauses = listOf("maintenancePaused", "maintenancePaused:drive", "maintenancePaused:calendar")
                .filter { previous.contains(it) }.associateWith { previous.getBoolean(it, true) }
            val editor = previous.edit().clear()
            pauses.forEach { (key, value) -> editor.putBoolean(key, value) }
            check(editor.commit()) { "A Google-fiók lekapcsolása nem menthető." }
            accountEpoch.incrementAndGet()
            _uiState.value = loadInitialUiState()
            Result.success(Unit)
        }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun revokeAccess(activity: Activity): Result<Unit> = withContext(Dispatchers.IO) {
        runGoogleOperation("revoke", "Google maintenance") {
            val request = captureMaintenance(NativeGoogleMaintenance.Kind.REVOKE)
            prepareMaintenance(request)
            revokeAccessInternal(activity, request)
        }
    }

    internal suspend fun previewMaintenance(kind: NativeGoogleMaintenance.Kind): NativeGoogleMaintenance.Request =
        withContext(Dispatchers.IO) { operations.run { captureMaintenance(kind) } }

    private suspend fun captureMaintenance(kind: NativeGoogleMaintenance.Kind): NativeGoogleMaintenance.Request {
        val sub = prefs().getString("sub", "").orEmpty()
        val email = prefs().getString("email", "").orEmpty()
        NativeGoogleAccountPolicy.requireConnected(sub, email)
        NativeGoogleMaintenance.requireSafe(appContext)
        return NativeGoogleMaintenance.Request(kind, sub, email,
            NativeDataReplacement.continuationForCurrentData(appContext), accountEpoch.get())
    }

    private suspend fun requireMaintenance(request: NativeGoogleMaintenance.Request) {
        currentCoroutineContext().ensureActive()
        request.requireAccount(prefs().getString("sub", "").orEmpty(), prefs().getString("email", "").orEmpty(), accountEpoch.get())
        request.dataset.requireCurrent(appContext)
        NativeGoogleMaintenance.requireSafe(appContext)
    }

    private suspend fun prepareMaintenance(request: NativeGoogleMaintenance.Request) {
        requireMaintenance(request) // Main-only admission before the metadata lease.
        NativeDataReplacement.mutate(appContext) {
            request.dataset.requireCurrent(appContext)
            request.requireAccount(prefs().getString("sub", "").orEmpty(), prefs().getString("email", "").orEmpty(), accountEpoch.get())
            check(com.repforge.app.ui.health.NativeHealthForegroundAdmission.safeOffMain(appContext))
            // Persist fail-closed before any remote deletion. Local history is never touched.
            check(prefs().edit().putBoolean("maintenancePaused", true)
                .putBoolean("maintenancePaused:drive", true).putBoolean("maintenancePaused:calendar", true).commit())
            database.withTransaction {
                val dao = database.keyValueDocumentDao()
                for (prefix in listOf("cloudBase:", "cloudHeads:", "cloudCalendarSent:")) dao.deleteDocument(prefix + request.sub)
                val stamp = System.currentTimeMillis()
                dao.putDocument(KeyValueDocumentEntity("cloudPrefs", JSONObject().put("drive", false).put("calendar", false).toString(), stamp))
                dao.putDocument(KeyValueDocumentEntity("cloudStatus", "{}", stamp))
            }
        }
    }

    private suspend fun revokeAccessInternal(activity: Activity, request: NativeGoogleMaintenance.Request) {
        requireMaintenance(request)
        withContext(Dispatchers.Main) {
            Identity.getAuthorizationClient(activity).revokeAccess(RevokeAccessRequest.builder()
                .setAccount(Account(request.email, "com.google"))
                .setScopes((scopesForOperation("drive") + scopesForOperation("calendar")).distinct()).build()).await()
        }
        requireMaintenance(request)
        NativeDataReplacement.mutate(appContext) {
            request.dataset.requireCurrent(appContext)
            request.requireAccount(prefs().getString("sub", "").orEmpty(), prefs().getString("email", "").orEmpty(), accountEpoch.get())
            check(prefs().edit().clear().putBoolean("maintenancePaused", true).commit())
            accountEpoch.incrementAndGet()
            refreshProfileState()
        }
    }

    private suspend fun performMaintenance(activity: Activity?, token: String, request: NativeGoogleMaintenance.Request): NativeGoogleMaintenance.Report {
        prepareMaintenance(request)
        return try {
            when (request.kind) {
                NativeGoogleMaintenance.Kind.REVOKE -> {
                    revokeAccessInternal(requireNotNull(activity), request)
                    NativeGoogleMaintenance.Report(1, true)
                }
                NativeGoogleMaintenance.Kind.DRIVE -> deleteDriveInternal(token, request)
                NativeGoogleMaintenance.Kind.CALENDAR -> deleteCalendarsInternal(token, request)
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { NativeGoogleMaintenance.Report(0, false, true) }
    }

    /**
     * Full #18 multi-device Drive Cloud Sync:
     * 1. Lists all `repforge-sync-<device>-*.json` files in `appDataFolder`.
     * 2. Selects current snapshot per device + optional archive snapshots when `recoverEarlier == true`.
     * 3. Merges `history`, `weights`, `scheduled`, `programs`, `exercises`, and `settings` using `tp18MergeSync`.
     * 4. Syncs workout photos (`trainpilot-photo-<id>.jpg`) with MD5 verification.
     * 5. Accepts the local merge through the redo importer, then publishes/verifies the snapshot.
     * 6. Records `cloudBase:<owner>` only after publication, deletes photos and prunes old snapshots.
     */
    suspend fun syncDriveCloud(
        accessToken: String,
        silent: Boolean = false,
        recoverEarlier: Boolean = false,
        preferPhoneOnConflict: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        runGoogleOperation("drive", "Drive-fájlok lekérése…") {
            syncDriveCloudInternal(accessToken, silent, recoverEarlier, preferPhoneOnConflict)
        }
    }

    private suspend fun syncDriveCloudInternal(accessToken: String, silent: Boolean, recoverEarlier: Boolean, preferPhoneOnConflict: Boolean): String {
        check(ActiveWorkoutStore.current(appContext)?.optBoolean("active", false) != true) {
            "Fejezd be az aktív edzést a felhőszinkron előtt."
        }
        var photoStage: NativeCloudPhotoStage? = null
        return try {
            val profile = verifyAndFetchProfile(accessToken)
            val owner = profile.getString("sub")
            val localData = migrator.exportBackupJson(includeHealthJournal = false)
            val files = driveListInternal(accessToken)
            val (currentFiles, archiveFiles) = selectSnapshots18(files)

            val baseDoc = database.keyValueDocumentDao().getDocument("cloudBase:$owner")
            val baseData = baseDoc?.json?.let { runCatching { JSONObject(it) }.getOrNull() }

            val remotes = ArrayList<JSONObject>()
            for (i in currentFiles.indices) {
                _uiState.value = _uiState.value.copy(stageMessage = "Mentések olvasása (${i + 1}/${currentFiles.size})…")
                val f = currentFiles[i]
                val snap = driveReadInternal(accessToken, f.getString("id"), owner)
                if (snap != null) remotes.add(snap)
            }

            val archives = ArrayList<JSONObject>()
            if (recoverEarlier && !silent) {
                for (i in archiveFiles.indices) {
                    _uiState.value = _uiState.value.copy(stageMessage = "Korábbi mentések vizsgálata (${i + 1}/${archiveFiles.size})…")
                    val f = archiveFiles[i]
                    val snap = try { driveReadInternal(accessToken, f.getString("id"), owner) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { null }
                    if (snap != null) archives.add(snap)
                }
            }

            val tombstones = database.workoutHistoryDao().getAllTombstones()
            val deletedIds = HashSet<String>()
            val deletedKeys = HashSet<String>()
            for (t in tombstones) {
                if (t.workoutKey.isNotEmpty()) deletedKeys.add(t.workoutKey)
                val arr = runCatching { JSONArray(t.idsJson) }.getOrNull()
                if (arr != null) {
                    for (j in 0 until arr.length()) deletedIds.add(arr.optString(j))
                }
            }

            val includeHealth = settingsStore.current().healthBackupConsent
            val merged = NativeBackupPrivacy.sanitize(mergeSync18(
                local = localData,
                remotes = remotes,
                base = baseData,
                archives = archives,
                deletedHistoryIds = deletedIds,
                deletedHistoryKeys = deletedKeys,
                preferLocalOnConflict = preferPhoneOnConflict
            ), includeHealth = includeHealth)
            // A remote backup must never turn this device's health-sharing choice on.
            merged.optJSONObject("settings")?.put("healthBackupConsent", includeHealth)
            merged.put("healthIncluded", includeHealth)

            // Validate portable v144 favorites before any Drive upload or local replacement.
            NativeExerciseFavorites.decode(merged.opt(NativeExerciseFavorites.KEY))

            // Network work may only touch private staged files; errors must not become a successful sync.
            _uiState.value = _uiState.value.copy(stageMessage = "Edzésfotók szinkronizálása…")
            val stagedPhotos = NativeCloudPhotoStage(appContext, merged).also { photoStage = it }
            stagedPhotos.prepare(upload = { id, file -> writeDrivePhotoInternal(accessToken, id, file) },
                download = { id, driveId, file -> readDrivePhotoInternal(accessToken, id, driveId, file) })

            val deviceId = settingsStore.ensureCloudDeviceId()
            val mergedCanonical = LegacySyncIdentity18.canonical(merged)
            val baseCanonical = baseData?.let { LegacySyncIdentity18.canonical(it) }

            val continuation = NativeCloudMergeGuard.installAndPublish(appContext, localData, merged, stagedPhotos, owner) { accepted ->
                if (baseCanonical == null || mergedCanonical != baseCanonical || files.length() == 0) {
                    _uiState.update { it.copy(stageMessage = "Felhőmentés feltöltése és ellenőrzése…") }
                    val envelope = JSONObject().put("app", "RepForgeSync").put("schema", 1)
                        .put("owner", owner).put("device", deviceId).put("data", accepted)
                    writeSnapshotInternal(accessToken, owner, deviceId, envelope)
                }
            }

            // Persistent deletedAt/Drive IDs make a failed delete retryable. Reject stale continuations.
            val operationContext = currentCoroutineContext()
            stagedPhotos.finishRemoteDeletes { id, driveId ->
                continuation.requireCurrent(appContext)
                operationContext.ensureActive()
                NativeCloudPhotoDeletion.deleteUnreferenced(appContext, id, driveId, continuation) {
                    deleteDrivePhotoInternal(accessToken, id, driveId)
                }
            }
            val nowIso = Instant.now().toString()
            NativeDataReplacement.mutate(appContext) {
                continuation.requireCurrent(appContext)
                database.keyValueDocumentDao().putDocument(
                    KeyValueDocumentEntity("cloudStatus", JSONObject().put("drive", nowIso).toString(), System.currentTimeMillis()))
            }

            if (recoverEarlier) {
                _uiState.value = _uiState.value.copy(stageMessage = "Régi mentések karbantartása…")
                try { pruneSnapshotsInternal(accessToken, owner, deviceId) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Optional retention; the verified snapshot stays authoritative. */ }
            }

            WearSyncRepository.refreshWatchHomeAsync(appContext)
            prefs().edit().putString("lastDriveSyncIso", nowIso).apply()
            NativeSyncSchedule.recordAutoResult(appContext, GoogleSyncAction.DRIVE.key, null)
            _uiState.value = _uiState.value.copy(
                lastDriveSyncIso = nowIso,
                errorMessage = null
            )
            nowIso
        } finally { photoStage?.close() }
    }

    /** Syncs scheduled workouts with the TrainPilot-managed Google Calendar (`rf[0-9a-f]{64}` IDs). */
    suspend fun syncCalendarCloud(accessToken: String): Result<Int> = withContext(Dispatchers.IO) {
        runGoogleOperation("calendar", "Naptár szinkronizálása…") {
            syncCalendarCloudInternal(accessToken)
        }
    }

    private suspend fun syncCalendarCloudInternal(accessToken: String,
        dataTicket: NativeDataReplacement.Continuation = NativeDataReplacement.continuationForCurrentData(appContext)): Int {
        verifyAndFetchProfile(accessToken)
        currentCoroutineContext().ensureActive()
        NativeDataReplacement.mutate(appContext) {
            dataTicket.requireCurrent(appContext)
            NativeCalendarBackup.materializeLegacyRows(database)
        }
        val calendarId = ensureManagedCalendar(accessToken, dataTicket)
        val entries = database.calendarScheduleDao().getAllEntries()
        val base = "https://www.googleapis.com/calendar/v3/calendars/${enc(calendarId)}/events"
        var count = 0
        val operations = NativeCalendarSyncPlan.operations(entries).toList()
        val total = operations.size
        // Only skip writes confirmed by this app on this exact account and calendar.
        // A failed or interrupted run never records an unconfirmed operation.
        val syncScope = "${prefs().getString("sub", "").orEmpty()}|$calendarId"
        val fingerprintKey = "calendarConfirmedFingerprints"
        val previous = runCatching { JSONObject(prefs().getString(fingerprintKey, "{}").orEmpty()) }
            .getOrDefault(JSONObject())
        // Periodically reconcile with Google in case events were edited or removed elsewhere.
        val sameScope = previous.optString("scope") == syncScope
        val confirmed = if (sameScope) previous.optJSONObject("events") ?: JSONObject() else JSONObject()
        val checked = if (sameScope) previous.optJSONObject("eventCheckedAt") ?: JSONObject() else JSONObject()
        _uiState.update { it.copy(stageMessage = "Naptár szinkronizálása… (0/$total)") }
        for (operation in operations) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            dataTicket.requireCurrent(appContext)
            val fingerprint = operation.payload?.toString() ?: "<deleted>"
            if (confirmed.optString(operation.id) == fingerprint &&
                System.currentTimeMillis() - checked.optLong(operation.id, 0L) in 0L..86_400_000L) {
                count++
                _uiState.update { it.copy(stageMessage = "Naptár szinkronizálása… ($count/$total)") }
                continue
            }
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            dataTicket.requireCurrent(appContext)
            val id = operation.id
            if (operation.delete) {
                try { request(accessToken, "DELETE", "$base/$id", null) }
                catch (err: ApiError) { if (err.code != 404 && err.code != 410) throw err }
            } else {
                try { request(accessToken, "POST", "$base?sendUpdates=none", operation.payload) }
                catch (err: ApiError) {
                    if (err.code != 409) throw err
                    try { request(accessToken, "PUT", "$base/$id?sendUpdates=none", operation.payload) }
                    catch (missing: ApiError) {
                        if (missing.code != 404 && missing.code != 410) throw missing
                        // A conflict followed by a missing event may be a concurrent deletion.
                        // Verify the calendar first; never replace it on an uncertain event write.
                        currentCoroutineContext().ensureActive()
                        dataTicket.requireCurrent(appContext)
                        requireManagedCalendar(accessToken, calendarId)
                        dataTicket.requireCurrent(appContext)
                        try { request(accessToken, "POST", "$base?sendUpdates=none", operation.payload) }
                        catch (conflict: ApiError) {
                            if (conflict.code != 409) throw conflict
                            currentCoroutineContext().ensureActive()
                            dataTicket.requireCurrent(appContext)
                            request(accessToken, "PUT", "$base/$id?sendUpdates=none", operation.payload)
                        }
                    }
                }
            }
            // Commit after each confirmed response, so cancellation cannot mark pending work done.
            confirmed.put(operation.id, fingerprint)
            checked.put(operation.id, System.currentTimeMillis())
            dataTicket.requireCurrent(appContext)
            check(prefs().edit().putString(fingerprintKey, JSONObject()
                .put("scope", syncScope).put("checkedAt", System.currentTimeMillis())
                .put("events", confirmed).put("eventCheckedAt", checked).toString()).commit()) {
                "A naptár szinkronállapota nem menthető."
            }
            count++
            // Progress is reported only after the remote operation completed.
            _uiState.update { it.copy(stageMessage = "Naptár szinkronizálása… ($count/$total)") }
        }
        val nowIso = Instant.now().toString()
        dataTicket.requireCurrent(appContext)
        prefs().edit()
            .putString("lastCalendarSyncIso", nowIso)
            .putInt("lastCalendarSyncCount", count)
            .apply()
        NativeSyncSchedule.recordAutoResult(appContext, GoogleSyncAction.CALENDAR.key, null)
        _uiState.value = _uiState.value.copy(
            lastCalendarSyncIso = nowIso,
            lastCalendarSyncCount = count,
            errorMessage = null
        )
        return count
    }

    suspend fun deleteAllDriveAppData(accessToken: String): Result<Int> = withContext(Dispatchers.IO) {
        runGoogleOperation("drive-delete", "Google maintenance") {
            val ticket = captureMaintenance(NativeGoogleMaintenance.Kind.DRIVE)
            verifyAndFetchProfile(accessToken)
            val report = performMaintenance(null, accessToken, ticket)
            check(report.complete) { "Partial Google deletion: ${report.completed}; remote outcome uncertain" }
            report.completed
        }
    }

    suspend fun deleteManagedCalendars(accessToken: String): Result<Int> = withContext(Dispatchers.IO) {
        runGoogleOperation("calendar-delete", "Google maintenance") {
            val ticket = captureMaintenance(NativeGoogleMaintenance.Kind.CALENDAR)
            verifyAndFetchProfile(accessToken)
            val report = performMaintenance(null, accessToken, ticket)
            check(report.complete) { "Partial Google deletion: ${report.completed}; remote outcome uncertain" }
            report.completed
        }
    }

    private suspend fun deleteDriveInternal(token: String, ticket: NativeGoogleMaintenance.Request): NativeGoogleMaintenance.Report {
        requireMaintenance(ticket)
        val files = appFiles(token, "name contains 'repforge-sync-' or name contains 'trainpilot-photo-'")
        val ids = (0 until files.length()).map { files.getJSONObject(it) }
            .filter { APP_FILE_REGEX.matches(it.optString("name")) }.map { it.getString("id") }
        return NativeGoogleMaintenance.deleteSequentially(ids, { requireMaintenance(ticket) }) { id ->
            try { request(token, "DELETE", "https://www.googleapis.com/drive/v3/files/${enc(id)}", null) }
            catch (e: ApiError) { if (e.code != 404 && e.code != 410) throw e }
        }
    }

    private suspend fun deleteCalendarsInternal(token: String, ticket: NativeGoogleMaintenance.Request): NativeGoogleMaintenance.Report {
        var page = ""
        val ids = ArrayList<String>()
        do {
            requireMaintenance(ticket)
            val response = request(token, "GET", "https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250" +
                if (page.isEmpty()) "" else "&pageToken=${enc(page)}", null)
            val rows = response.optJSONArray("items") ?: JSONArray()
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                if (row.optString("description") == MANAGED_CALENDAR_DESC && row.optString("accessRole") == "owner") ids.add(row.getString("id"))
            }
            page = response.optString("nextPageToken")
        } while (page.isNotEmpty())
        val report = NativeGoogleMaintenance.deleteSequentially(ids, { requireMaintenance(ticket) }) { id ->
            try { request(token, "DELETE", "https://www.googleapis.com/calendar/v3/calendars/${enc(id)}", null) }
            catch (e: ApiError) { if (e.code != 404 && e.code != 410) throw e }
        }
        try {
            requireMaintenance(ticket)
            NativeDataReplacement.mutate(appContext) {
                ticket.dataset.requireCurrent(appContext)
                check(prefs().edit().remove("calendar").commit())
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return report.copy(complete = false) }
        return report
    }

    internal class ApiError(val code: Int, val reason: String, val operation: String? = null) :
        IOException("HTTP $code ($reason)" + operation?.let { " [$it]" }.orEmpty())
}
