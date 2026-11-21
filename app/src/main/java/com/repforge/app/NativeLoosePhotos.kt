package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Photos added outside a workout or weight entry live in the shared private gallery. */
internal object NativeLoosePhotos {
    const val KEY = "native:loosePhotos"

    data class Request(val ticket: NativeDataReplacement.Continuation)

    fun prepare(context: Context): Request =
        Request(NativeDataReplacement.continuationForCurrentData(context))

    suspend fun importAndAttach(
        context: Context,
        database: TrainPilotDatabase,
        request: Request,
        importPhoto: suspend () -> Result<StoredPhotoMetadata?>
    ): Boolean = importAndStorePrivatePhoto(context, importPhoto) { photo ->
        attach(context, database, request, photo)
    }

    suspend fun attach(
        context: Context,
        database: TrainPilotDatabase,
        request: Request,
        photo: StoredPhotoMetadata
    ) = NativeDataReplacement.mutate(context) {
        request.ticket.requireCurrent(context)
        check(WorkoutPhotoRepository(context).exists(photo.id)) { "Photo file is missing" }
        database.withTransaction {
            val current = database.keyValueDocumentDao().getDocument(KEY)
            val photos = current?.let { JSONArray(it.json) } ?: JSONArray()
            check((0 until photos.length()).none { photos.optJSONObject(it)?.optString("id") == photo.id }) {
                "Photo already exists in the private gallery"
            }
            photos.put(
                JSONObject()
                    .put("id", photo.id)
                    .put("label", "other")
                    .put("createdAt", Instant.now().toString())
                    .put("width", photo.width)
                    .put("height", photo.height)
                    .put("bytes", photo.bytes)
            )
            database.keyValueDocumentDao().putDocument(
                KeyValueDocumentEntity(KEY, photos.toString(), System.currentTimeMillis())
            )
        }
    }

    suspend fun count(database: TrainPilotDatabase): Int =
        database.keyValueDocumentDao().getDocument(KEY)?.let { document ->
            JSONArray(document.json).length()
        } ?: 0
}
