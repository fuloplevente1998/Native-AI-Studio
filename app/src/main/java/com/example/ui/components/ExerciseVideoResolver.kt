package com.example.ui.components

import com.repforge.app.data.db.TrainPilotDatabase
import java.net.URI
import java.util.Locale

/** Only exact YouTube IDs or recognised HTTPS YouTube video URLs are embeddable. */
private val youtubeIdPattern = Regex("^[A-Za-z0-9_-]{11}$")

fun parseYouTubeVideoId(value: String): String? {
    val raw = value.trim()
    if (youtubeIdPattern.matches(raw)) return raw
    val uri = runCatching { URI(raw) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.userInfo != null) return null
    val host = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.")?.removePrefix("m.")
        ?: return null
    val path = uri.path.orEmpty().trim('/')
    val candidate = when (host) {
        "youtu.be" -> path.takeIf { !it.contains('/') }
        "youtube.com", "youtube-nocookie.com" -> when {
            path == "watch" -> uri.rawQuery.orEmpty().split('&')
                .mapNotNull { part ->
                    val key = part.substringBefore('=')
                    if (key == "v") part.substringAfter('=', "") else null
                }.firstOrNull()
            path.startsWith("embed/") || path.startsWith("shorts/") || path.startsWith("live/") ->
                path.substringAfter('/').takeIf { !it.contains('/') }
            else -> null
        }
        else -> null
    }
    return candidate?.takeIf { youtubeIdPattern.matches(it) }
}

/** Do not dispatch javascript:, file:, intent:, http: or malformed links to other apps. */
fun safeExternalExerciseVideoUrl(value: String): String? {
    val raw = value.trim()
    val uri = runCatching { URI(raw) }.getOrNull() ?: return null
    return raw.takeIf {
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host?.contains('.') == true &&
            uri.userInfo == null
    }
}

enum class ExerciseVideoState { YOUTUBE, EXTERNAL_ONLY, MISSING, INVALID }

data class ExerciseVideoLink(
    val state: ExerciseVideoState,
    val externalUrl: String? = null,
    val youtubeId: String? = null
)

/**
 * The selected Room/UI exercise URL wins. A malformed nonblank URL must NOT be
 * silently replaced by a catalog or generic video. Custom exercises have no fallback.
 */
fun resolveExerciseVideoLink(
    storedVideoUrl: String?,
    catalogVideoUrl: String?,
    isCustom: Boolean
): ExerciseVideoLink {
    val chosen = storedVideoUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: if (isCustom) null else catalogVideoUrl?.trim()?.takeIf { it.isNotEmpty() }
    if (chosen == null) return ExerciseVideoLink(ExerciseVideoState.MISSING)
    val videoId = parseYouTubeVideoId(chosen)
    if (videoId != null) {
        return ExerciseVideoLink(
            ExerciseVideoState.YOUTUBE,
            externalUrl = "https://www.youtube.com/watch?v=$videoId",
            youtubeId = videoId
        )
    }
    val external = safeExternalExerciseVideoUrl(chosen)
    return if (external != null) {
        ExerciseVideoLink(ExerciseVideoState.EXTERNAL_ONLY, externalUrl = external)
    } else {
        ExerciseVideoLink(ExerciseVideoState.INVALID)
    }
}

data class ExerciseVideoDetails(val link: ExerciseVideoLink, val notesHu: String)

/**
 * Lookup by exact library ID, or (only without an ID) one unambiguous exact
 * localized title. No substring match, guessed ID, or universal video fallback.
 */
fun resolveExerciseVideoAndNotes(
    exerciseTitle: String,
    exerciseId: String? = null,
    storedVideoUrl: String? = null,
    isCustom: Boolean = false,
    selectedNotesHu: String? = null,
    allowCatalogByTitle: Boolean = true
): ExerciseVideoDetails {
    val catalog = runCatching { TrainPilotDatabase.defaultExercises() }.getOrDefault(emptyList())
    val knownId = exerciseId?.takeIf { it.isNotBlank() }
    val match = if (knownId != null) {
        catalog.firstOrNull { it.id.equals(knownId, ignoreCase = true) }
    } else if (allowCatalogByTitle) {
        catalog.filter { entry ->
            listOf(entry.nameHu, entry.nameEn, entry.nameDe, entry.nameRo)
                .any { it.equals(exerciseTitle.trim(), ignoreCase = true) }
        }.singleOrNull()
    } else null
    val link = resolveExerciseVideoLink(storedVideoUrl, match?.videoUrl, isCustom)
    val notes = selectedNotesHu?.takeIf { it.isNotBlank() }
        ?: match?.notesHu?.takeIf { it.isNotBlank() }
        ?: "Ehhez a gyakorlathoz nincs végrehajtási leírás."
    return ExerciseVideoDetails(link, notes)
}
