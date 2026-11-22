package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Weight photos are annotations on one immutable measurement identity. */
internal object NativeWeightPhotos {
    data class Request(
        val weightId: String,
        val label: String,
        val ticket: NativeDataReplacement.Continuation
    )

    fun prepare(context: Context, weightId: String, label: String = "other"): Request {
        require(weightId.isNotBlank() && label in setOf("before", "after", "other"))
        return Request(weightId, label, NativeDataReplacement.continuationForCurrentData(context))
    }

    suspend fun importAndAttach(
        context: Context,
        db: TrainPilotDatabase,
        request: Request,
        importPhoto: suspend () -> Result<StoredPhotoMetadata?>
    ): Boolean = importAndStorePrivatePhoto(context, importPhoto) { photo ->
        attach(context, db, request, photo)
    }

    suspend fun attach(
        context: Context,
        db: TrainPilotDatabase,
        request: Request,
        photo: StoredPhotoMetadata
    ) = NativeDataReplacement.mutate(context) {
        request.ticket.requireCurrent(context)
        check(WorkoutPhotoRepository(context).exists(photo.id)) { "Photo file is missing" }
        db.withTransaction {
            val current = db.bodyWeightDao().getWeightById(request.weightId)
                ?: error("Weight measurement deleted")
            val raw = runCatching { JSONObject(current.rawJson) }.getOrElse { JSONObject() }
            val photos = raw.optJSONArray("photos") ?: JSONArray()
            check((0 until photos.length()).none {
                photos.optJSONObject(it)?.optString("id") == photo.id
            }) { "Photo already attached" }
            val createdAt = Instant.now().toString()
            photos.put(
                JSONObject()
                    .put("id", photo.id)
                    .put("label", request.label)
                    .put("createdAt", createdAt)
                    .put("width", photo.width)
                    .put("height", photo.height)
                    .put("bytes", photo.bytes)
            )
            raw.put("photos", photos)
            db.bodyWeightDao().upsertWeight(
                current.copy(rawJson = raw.toString())
            )
        }
    }

    suspend fun remove(context: Context, db: TrainPilotDatabase, weightId: String, photoId: String) =
        NativeDataReplacement.mutate(context) {
            val unreferenced = db.withTransaction {
                val current = db.bodyWeightDao().getWeightById(weightId)
                    ?: error("Weight measurement deleted")
                val raw = runCatching { JSONObject(current.rawJson) }.getOrElse { JSONObject() }
                val old = raw.optJSONArray("photos") ?: JSONArray()
                val remaining = JSONArray()
                var removed = false
                for (index in 0 until old.length()) {
                    val photo = old.optJSONObject(index) ?: continue
                    if (photo.optString("id") == photoId) {
                        removed = true
                    } else {
                        remaining.put(photo)
                    }
                }
                check(removed) { "Photo not attached" }
                raw.put("photos", remaining)
                db.bodyWeightDao().upsertWeight(current.copy(rawJson = raw.toString()))
                unreferencedFiles(db, setOf(photoId))
            }
            unreferenced.forEach(WorkoutPhotoRepository(context)::delete)
        }

    /** Restored records can share a single private file. Call in the deleting Room transaction. */
    suspend fun unreferencedFiles(db: TrainPilotDatabase, candidateIds: Set<String>): Set<String> {
        if (candidateIds.isEmpty()) return emptySet()
        val referenced = mutableSetOf<String>()
        fun collect(photos: JSONArray?) {
            if (photos == null) return
            for (index in 0 until photos.length()) {
                val photo = photos.optJSONObject(index) ?: continue
                val id = photo.optString("id")
                if (id in candidateIds && !photo.optBoolean("deleted", false) &&
                    !BackupRestoreManager.photoDeleted(photo)) referenced.add(id)
            }
        }
        for (weight in db.bodyWeightDao().getAllWeights()) {
            collect(runCatching { JSONObject(weight.rawJson).optJSONArray("photos") }.getOrNull())
        }
        val documents = db.keyValueDocumentDao()
        documents.getDocument(NativeLoosePhotos.KEY)?.let { collect(JSONArray(it.json)) }
        documents.getDocument("draft")?.let { collect(JSONObject(it.json).optJSONArray("photos")) }

        return candidateIds.filterTo(mutableSetOf()) { id ->
            id !in referenced && !db.workoutHistoryDao().hasLiveCloudPhotoReference(id, ":")
        }
    }
}

/** The importer returns a newly created private file that this operation owns until commit. */
internal suspend fun importAndStorePrivatePhoto(
    context: Context,
    importPhoto: suspend () -> Result<StoredPhotoMetadata?>,
    attach: suspend (StoredPhotoMetadata) -> Unit
): Boolean {
    val caller = currentCoroutineContext()
    caller.ensureActive()
    // File creation and its rollback must both finish if the screen is left mid-import.
    return withContext(Dispatchers.IO + NonCancellable) {
        val photo = importPhoto().getOrThrow() ?: return@withContext false
        var attached = false
        try {
            caller.ensureActive()
            attach(photo)
            attached = true
        } finally {
            if (!attached) WorkoutPhotoRepository(context).delete(photo.id)
        }
        true
    }
}
