package com.example.ui.components

import com.repforge.app.data.db.TrainPilotDatabase
import java.net.URI
import java.util.Locale

/** Only exact YouTube IDs or recognised HTTPS YouTube video URLs are embeddable. */
private val youtubeIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
private val dnsLabelPattern = Regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")
private val ipv4Pattern = Regex("^[0-9]+(?:\\.[0-9]+){3}$")

fun parseYouTubeVideoId(value: String): String? {
    val raw = value.trim()
    if (youtubeIdPattern.matches(raw)) return raw
    val uri = runCatching { URI(raw) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true) ||
        uri.userInfo != null || uri.port !in listOf(-1, 443) || '\\' in raw) return null
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

/**
 * URL must be an absolute, public DNS HTTPS URL without credentials or a
 * nonstandard port. Reject nonweb schemes, IPs, local hosts and URI tricks.
 */
fun safeExternalExerciseVideoUrl(value: String): String? {
    val raw = value.trim()
    if ('\\' in raw || raw.any { it.isISOControl() }) return null
    val uri = runCatching { URI(raw) }.getOrNull() ?: return null
    val host = uri.host?.lowercase(Locale.ROOT) ?: return null
    val labels = host.split('.')
    val safeHost = host.length <= 253 &&
        labels.size >= 2 &&
        labels.all { it.length in 1..63 && dnsLabelPattern.matches(it) } &&
        !ipv4Pattern.matches(host) &&
        !host.endsWith(".local") && !host.endsWith(".localhost") &&
        !host.endsWith(".internal") && !host.endsWith(".test") &&
        !host.endsWith(".invalid") &&
        labels.last().any { it in 'a'..'z' }
    return raw.takeIf {
        uri.isAbsolute && uri.scheme.equals("https", ignoreCase = true) &&
            uri.userInfo == null && uri.port in listOf(-1, 443) &&
            safeHost
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
    allowCatalogByTitle: Boolean = true,
    language: String = "hu"
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
    val fallbackNoNotes = when (language.lowercase(Locale.ROOT)) {
        "en" -> "No execution description is available for this exercise."
        "de" -> "Für diese Übung ist keine Ausführungsbeschreibung verfügbar."
        "ro" -> "Nu există o descriere de execuție pentru acest exercițiu."
        else -> "Ehhez a gyakorlathoz nincs végrehajtási leírás."
    }
    val notes = selectedNotesHu?.takeIf { it.isNotBlank() }
        ?: match?.notesHu?.takeIf { it.isNotBlank() }
        ?: fallbackNoNotes
    return ExerciseVideoDetails(link, notes)
}
