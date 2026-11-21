package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.TrainPilotDatabase
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
            db.withTransaction {
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
            }
            WorkoutPhotoRepository(context).delete(photoId)
        }
}
