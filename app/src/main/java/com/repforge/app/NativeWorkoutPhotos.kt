package com.repforge.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Persist photo annotations in the native authority, before updating the summary preview. */
internal object NativeWorkoutPhotos {
    data class Request(val workoutId: String, val generation: Long, val label: String,
        val ticket: NativeDataReplacement.Continuation)

    fun prepare(context: Context, workoutId: String, label: String): Request {
        require(label in setOf("before", "after", "other"))
        val frame = NativeWorkoutEditing.project(context)
        check(frame.getString("workoutId") == workoutId) { "Workout changed" }
        return Request(workoutId, frame.getLong("generation"), label,
            NativeDataReplacement.continuationForCurrentData(context))
    }

    suspend fun attach(context: Context, request: Request, photo: StoredPhotoMetadata): JSONArray =
        edit(context, request) { photos ->
            check(WorkoutPhotoRepository(context).exists(photo.id)) { "Photo file is missing" }
            check((0 until photos.length()).none { photos.getJSONObject(it).optString("id") == photo.id })
            photos.put(JSONObject().put("id", photo.id).put("label", request.label)
                .put("createdAt", Instant.now().toString()).put("width", photo.width)
                .put("height", photo.height).put("bytes", photo.bytes))
        }

    suspend fun remove(context: Context, request: Request, photoId: String): JSONArray =
        edit(context, request) { photos ->
            JSONArray().also { kept ->
                for (i in 0 until photos.length()) {
                    val photo = photos.getJSONObject(i)
                    if (photo.optString("id") != photoId) kept.put(photo)
                }
            }
        }

    private suspend fun edit(context: Context, request: Request,
        change: (JSONArray) -> JSONArray): JSONArray = NativeDataReplacement.mutate(context) {
        withContext(Dispatchers.Main.immediate) {
            request.ticket.requireCurrent(context)
            val frame = NativeWorkoutEditing.project(context)
            check(frame.getString("workoutId") == request.workoutId &&
                frame.getLong("generation") == request.generation) { "Workout changed" }
            val photos = change(frame.optJSONArray("photos") ?: JSONArray())
            NativeWorkoutEditing.commitEdit(context, frame.put("photos", photos))
                .getJSONArray("photos")
        }
    }
}
