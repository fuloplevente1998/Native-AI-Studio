package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutPhotoEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** A photo annotation must never replace the parent workout or its measured history. */
internal object NativeJournalPhotos {
    data class Request(val workoutId: String, val label: String,
        val ticket: NativeDataReplacement.Continuation)

    fun prepare(context: Context, workoutId: String, label: String): Request {
        require(workoutId.isNotBlank() && label in setOf("before", "after", "other"))
        return Request(workoutId, label, NativeDataReplacement.continuationForCurrentData(context))
    }

    suspend fun attach(context: Context, db: TrainPilotDatabase, request: Request,
        photo: StoredPhotoMetadata) = NativeDataReplacement.mutate(context) {
        request.ticket.requireCurrent(context)
        check(WorkoutPhotoRepository(context).exists(photo.id)) { "Photo file is missing" }
        val dao = db.workoutHistoryDao()
        val initial = dao.getWorkoutWithDetails(request.workoutId)?.workout ?: error("Workout deleted")
        val aliases = setOf(initial.id, initial.syncId, initial.workoutKey).filter(String::isNotBlank).toSet()
        NativeJournalEditor.requireNotActive(context, aliases)
        db.withTransaction {
            val current = dao.getWorkoutWithDetailsById(initial.id)?.workout ?: error("Workout deleted")
            for (id in aliases) check(!dao.isTombstoned(id, JSONObject.quote(id))) { "Workout deleted" }
            val raw = JSONObject(current.rawJson)
            val photos = raw.optJSONArray("photos") ?: JSONArray()
            val createdAt = Instant.now().toString()
            photos.put(JSONObject().put("id", photo.id).put("label", request.label)
                .put("createdAt", createdAt).put("width", photo.width)
                .put("height", photo.height).put("bytes", photo.bytes))
            raw.put("photos", photos)
            check(dao.updateJournalPhotoJson(current.id, current.rawJson, raw.toString()) == 1) {
                "Workout changed; reopen photos"
            }
            dao.insertNewJournalPhoto(WorkoutPhotoEntity(photo.id, current.id, request.label,
                createdAt, System.currentTimeMillis(), photo.width, photo.height, photo.bytes, null, null))
        }
    }
}
