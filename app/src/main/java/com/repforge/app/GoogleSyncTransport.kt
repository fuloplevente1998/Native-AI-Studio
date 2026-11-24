package com.repforge.app

import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.repforge.shared.LegacySyncIdentity18
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.repforge.app.GoogleSyncRepository.Companion.SNAPSHOT_NAME_REGEX
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import com.repforge.app.GoogleSyncRepository.Companion.LIMIT
import com.repforge.app.GoogleSyncRepository.Companion.PHOTO_LIMIT
import com.repforge.app.GoogleSyncRepository.Companion.MANAGED_CALENDAR_DESC
import com.repforge.app.GoogleSyncRepository.Companion.FILE_ID_REGEX
import com.repforge.app.GoogleSyncRepository.Companion.UUID_REGEX
import com.repforge.app.GoogleSyncRepository.Companion.APP_FILE_REGEX
import com.repforge.app.GoogleSyncRepository.ApiError


// =========================================================================
// Low-Level HTTP & Drive / Calendar Transport
// =========================================================================

internal fun GoogleSyncRepository.verifyAndFetchProfile(accessToken: String, connecting: Boolean = false): JSONObject {
    val sub = prefs().getString("sub", "").orEmpty()
    val email = prefs().getString("email", "").orEmpty()
    if (!connecting) NativeGoogleAccountPolicy.requireConnected(sub, email)
    val profile = request(accessToken, "GET", "https://www.googleapis.com/oauth2/v3/userinfo", null)
    NativeGoogleAccountPolicy.requireMatchingProfile(sub, email, profile, connecting)
    return profile
}

internal fun GoogleSyncRepository.driveListInternal(accessToken: String): JSONArray {
    val all = JSONArray()
    var page = ""
    do {
        val q = enc("trashed = false and name contains 'repforge-sync-'")
        val fields = enc("nextPageToken,files(id,name,createdTime,version)")
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=$fields&pageSize=1000" +
            if (page.isEmpty()) "" else "&pageToken=${enc(page)}"
        val x = request(accessToken, "GET", url, null)
        val a = x.optJSONArray("files")
        if (a != null) {
            for (i in 0 until a.length()) all.put(a.get(i))
        }
        page = x.optString("nextPageToken")
    } while (page.isNotEmpty())
    return all
}

internal fun GoogleSyncRepository.driveReadInternal(accessToken: String, id: String, expectedOwner: String): JSONObject? {
    if (!FILE_ID_REGEX.matches(id)) throw IOException("Hibás fájlazonosító.")
    val meta = request(accessToken, "GET", "https://www.googleapis.com/drive/v3/files/$id?fields=name,version", null)
    if (!meta.optString("name").startsWith("repforge-sync-")) {
        throw IOException("Nem TrainPilot-kompatibilis mentés.")
    }
    val snap = request(accessToken, "GET", "https://www.googleapis.com/drive/v3/files/$id?alt=media", null)
    if ("RepForgeSync" != snap.optString("app") || snap.optInt("schema") != 1 || expectedOwner != snap.optString("owner")) {
        throw IOException("Nem TrainPilot-kompatibilis mentés.")
    }
    return snap.optJSONObject("data")
}

internal fun GoogleSyncRepository.writeSnapshotInternal(accessToken: String, owner: String, device: String, parsed: JSONObject) {
    val data = parsed.toString()
    if ("RepForgeSync" != parsed.optString("app") ||
        owner != parsed.optString("owner") ||
        data.toByteArray(StandardCharsets.UTF_8).size > LIMIT
    ) {
        throw IOException("Érvénytelen felhőmentés.")
    }
    if (!UUID_REGEX.matches(device)) throw IOException("Hibás eszközazonosító.")
    val meta = JSONObject()
        .put("name", "repforge-sync-$device-${UUID.randomUUID()}.json")
        .put("parents", JSONArray().put("appDataFolder"))
        .put("mimeType", "application/json")
    val boundary = "repforge" + UUID.randomUUID().toString().replace("-", "")
    val body = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n" +
        "--$boundary\r\nContent-Type: application/json\r\n\r\n$data\r\n--$boundary--\r\n"
    val created = http(
        accessToken,
        "POST",
        "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,version",
        body,
        "multipart/related; boundary=$boundary"
    )
    val read = request(
        accessToken,
        "GET",
        "https://www.googleapis.com/drive/v3/files/${created.getString("id")}?alt=media",
        null
    )
    if (read.toString() != parsed.toString()) {
        throw IOException("A felhőmentés visszaellenőrzése nem sikerült.")
    }
}

internal fun GoogleSyncRepository.pruneSnapshotsInternal(accessToken: String, owner: String, device: String): JSONObject {
    if (!UUID_REGEX.matches(device)) throw IOException("Invalid device ID.")
    // 2806: every device of this account (incl. old WebView-era installs) keeps only its latest DRIVE_SNAPSHOTS_KEPT
    // snapshots (user decision). This runs only after this phone's new snapshot was published and verified.
    val files = appFiles(accessToken, "name contains 'repforge-sync-'")
    val byDevice = HashMap<String, ArrayList<JSONObject>>()
    for (i in 0 until files.length()) {
        val f = files.getJSONObject(i)
        val name = f.optString("name")
        val nameDevice = SNAPSHOT_NAME_REGEX.find(name)?.groupValues?.get(1) ?: continue
        if (APP_FILE_REGEX.matches(name)) byDevice.getOrPut(nameDevice) { ArrayList() }.add(f)
    }
    var deleted = 0
    var protectedLegacy = 0
    for ((fileDevice, rows) in byDevice) for (f in snapshotsToPrune(rows)) {
        val snapshot = request(
            accessToken,
            "GET",
            "https://www.googleapis.com/drive/v3/files/${enc(f.getString("id"))}?alt=media",
            null
        )
        if ("RepForgeSync" != snapshot.optString("app") ||
            owner != snapshot.optString("owner") ||
            fileDevice != snapshot.optString("device")
        ) {
            continue
        }
        val data = snapshot.optJSONObject("data")
        var legacy = data == null
        for (key in arrayOf("history", "weights")) {
            val a = data?.optJSONArray(key)
            if (a == null) {
                legacy = true
                continue
            }
            for (j in 0 until a.length()) {
                val id = a.getJSONObject(j).optString("id")
                if (LegacySyncIdentity18.isLegacyProtectedId(id)) legacy = true
            }
        }
        if (legacy) protectedLegacy++ // Reported only; no longer kept forever.
        request(accessToken, "DELETE", "https://www.googleapis.com/drive/v3/files/${enc(f.getString("id"))}", null)
        deleted++
    }
    return JSONObject().put("deleted", deleted).put("protectedLegacy", protectedLegacy)
}

internal const val DRIVE_SNAPSHOTS_KEPT = 3

/** Oldest first; everything except the newest [keep] snapshots (by createdTime). Never empty-handed: keeps at least one. */
internal fun snapshotsToPrune(rows: List<JSONObject>, keep: Int = DRIVE_SNAPSHOTS_KEPT): List<JSONObject> {
    val sorted = rows.sortedBy { it.optString("createdTime") }
    return sorted.dropLast(keep.coerceAtLeast(1))
}

private fun photoName(id: String): String = "trainpilot-photo-$id.jpg"

private fun md5(f: File): String {
    val d = MessageDigest.getInstance("MD5")
    FileInputStream(f).use { input ->
        val b = ByteArray(8192)
        while (true) {
            val n = input.read(b)
            if (n == -1) break
            d.update(b, 0, n)
        }
    }
    val s = StringBuilder()
    for (x in d.digest()) s.append(String.format(Locale.US, "%02x", x.toInt() and 255))
    return s.toString()
}

internal fun GoogleSyncRepository.writeDrivePhotoInternal(accessToken: String, id: String, f: File): String {
    val name = photoName(id)
    val q = enc("trashed = false and name = '$name'")
    val existing = request(
        accessToken,
        "GET",
        "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=${enc("files(id,name,size,md5Checksum)")}&pageSize=2",
        null
    )
    val files = existing.optJSONArray("files")
    if (files != null && files.length() > 0) {
        val x = files.getJSONObject(0)
        if (x.optLong("size", -1L) == f.length() && md5(f).equals(x.optString("md5Checksum"), ignoreCase = true)) {
            return x.getString("id")
        }
    }
    val meta = JSONObject()
        .put("name", name)
        .put("parents", JSONArray().put("appDataFolder"))
        .put("mimeType", "image/jpeg")
    val boundary = "trainpilot" + UUID.randomUUID().toString().replace("-", "")
    val c = (URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,size,md5Checksum").openConnection() as HttpURLConnection).apply {
        connectTimeout = 20_000
        readTimeout = 45_000
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Authorization", "Bearer $accessToken")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        setChunkedStreamingMode(8192)
    }
    try {
        c.outputStream.use { o ->
            FileInputStream(f).use { input ->
                o.write(
                    "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n--$boundary\r\nContent-Type: image/jpeg\r\n\r\n"
                        .toByteArray(StandardCharsets.UTF_8)
                )
                val b = ByteArray(8192)
                while (true) {
                    val n = input.read(b)
                    if (n == -1) break
                    o.write(b, 0, n)
                }
                o.write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
            }
        }
        val status = c.responseCode
        if (status !in 200..299) throw apiError(c, status)
        val created = JSONObject(String(readResponse(c, LIMIT), StandardCharsets.UTF_8))
        if (name != created.optString("name") ||
            created.optLong("size", -1L) != f.length() ||
            !md5(f).equals(created.optString("md5Checksum"), ignoreCase = true)
        ) {
            throw IOException("A fotó Drive-ellenőrzése nem sikerült.")
        }
        return created.getString("id")
    } finally {
        c.disconnect()
    }
}

internal fun GoogleSyncRepository.readDrivePhotoInternal(accessToken: String, pid: String, driveFileId: String, destFile: File) {
    val name = photoName(pid)
    val meta = request(
        accessToken,
        "GET",
        "https://www.googleapis.com/drive/v3/files/$driveFileId?fields=name,size,mimeType,md5Checksum",
        null
    )
    val size = meta.optLong("size", -1L)
    if (name != meta.optString("name") || "image/jpeg" != meta.optString("mimeType") || size <= 0L || size > PHOTO_LIMIT) {
        throw IOException("Nem érvényes TrainPilot-fotó.")
    }
    val bytes = download(accessToken, "https://www.googleapis.com/drive/v3/files/$driveFileId?alt=media", PHOTO_LIMIT)
    NativeCloudPhotoStage.installDownload(pid, meta, bytes, destFile)
}

internal fun GoogleSyncRepository.deleteDrivePhotoInternal(accessToken: String, pid: String, driveFileId: String) {
    val name = photoName(pid)
    try {
        val meta = request(accessToken, "GET", "https://www.googleapis.com/drive/v3/files/$driveFileId?fields=name", null)
        if (name != meta.optString("name")) throw IOException("Nem ehhez a naplófotóhoz tartozó Drive-fájl.")
        request(accessToken, "DELETE", "https://www.googleapis.com/drive/v3/files/$driveFileId", null)
    } catch (e: ApiError) {
        if (e.code != 404 && e.code != 410) throw e
    }
}

internal fun GoogleSyncRepository.requireManagedCalendar(accessToken: String, id: String) {
    val calendar = request(accessToken, "GET", "https://www.googleapis.com/calendar/v3/calendars/${enc(id)}", null)
    if (MANAGED_CALENDAR_DESC != calendar.optString("description")) {
        throw IOException("This is not a TrainPilot-managed calendar.")
    }
}

internal suspend fun GoogleSyncRepository.ensureManagedCalendar(accessToken: String, dataTicket: NativeDataReplacement.Continuation): String {
    var id = prefs().getString("calendar", "").orEmpty()
    val unavailable = mutableSetOf<String>()
    if (id.isNotEmpty()) {
        try {
            requireManagedCalendar(accessToken, id)
            return id
        } catch (e: ApiError) {
            if (e.code != 404 && e.code != 410) throw e
            unavailable.add(id)
            // Keep the old reference until a usable replacement has been obtained.
            id = ""
        }
    }
    var page = ""
    do {
        currentCoroutineContext().ensureActive()
        dataTicket.requireCurrent(appContext)
        val r = request(
            accessToken,
            "GET",
            "https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250" +
                if (page.isEmpty()) "" else "&pageToken=${enc(page)}",
            null
        )
        val a = r.optJSONArray("items")
        if (a != null) {
            for (i in 0 until a.length()) {
                val c = a.getJSONObject(i)
                val candidate = c.optString("id")
                if (MANAGED_CALENDAR_DESC != c.optString("description") ||
                    "owner" != c.optString("accessRole") || c.optBoolean("deleted") ||
                    candidate.isBlank() || candidate in unavailable) continue
                try {
                    // CalendarList metadata can outlive access to the calendar itself.
                    requireManagedCalendar(accessToken, candidate)
                    id = candidate
                    break
                } catch (e: ApiError) {
                    if (e.code != 404 && e.code != 410) throw e
                    unavailable.add(candidate)
                }
            }
        }
        page = r.optString("nextPageToken")
    } while (id.isEmpty() && page.isNotEmpty())

    if (id.isEmpty()) {
        val c = JSONObject()
            .put("summary", "TrainPilot edzések")
            .put("description", MANAGED_CALENDAR_DESC)
            .put("timeZone", TimeZone.getDefault().id)
        dataTicket.requireCurrent(appContext)
        id = request(accessToken, "POST", "https://www.googleapis.com/calendar/v3/calendars", c).getString("id")
    }
    NativeDataReplacement.mutate(appContext) {
        dataTicket.requireCurrent(appContext)
        check(prefs().edit().putString("calendar", id).commit()) { "A Google-naptár állapota nem menthető." }
    }
    return id
}

internal fun GoogleSyncRepository.appFiles(accessToken: String, query: String): JSONArray {
    val all = JSONArray()
    var page = ""
    do {
        val result = request(
            accessToken,
            "GET",
            "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=${enc("trashed = false and ($query)")}&fields=${enc("nextPageToken,files(id,name,createdTime)")}&pageSize=1000" +
                if (page.isEmpty()) "" else "&pageToken=${enc(page)}",
            null
        )
        val files = result.optJSONArray("files")
        if (files != null) {
            for (i in 0 until files.length()) all.put(files.get(i))
        }
        page = result.optString("nextPageToken")
    } while (page.isNotEmpty())
    return all
}

internal fun GoogleSyncRepository.download(accessToken: String, url: String, limit: Int): ByteArray {
    var attempt = 0
    while (true) {
        val c = connectionFactory(URL(url)).apply {
            instanceFollowRedirects = false
            connectTimeout = 20_000
            readTimeout = 45_000
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        try {
            val status = c.responseCode
            if (status !in 200..299) {
                val error = apiError(c, status)
                if (attempt < 3 && GoogleRequestPolicy.transientStatus(status, error.reason)) {
                    val delay = GoogleRequestPolicy.delay(attempt, c.getHeaderField("Retry-After"), System.currentTimeMillis())
                    if (delay >= 0L) {
                        c.disconnect()
                        Thread.sleep(delay)
                        attempt++
                        continue
                    }
                }
                throw error
            }
            return readResponse(c, limit)
        } finally {
            c.disconnect()
        }
    }
}

internal fun GoogleSyncRepository.request(accessToken: String, method: String, url: String, body: JSONObject?): JSONObject {
    return http(accessToken, method, url, body?.toString(), "application/json; charset=UTF-8")
}

private fun GoogleSyncRepository.http(accessToken: String, method: String, url: String, body: String?, type: String): JSONObject {
    val eventId = if (body != null && method == "POST" && type.startsWith("application/json")) {
        runCatching { JSONObject(body).optString("id") }.getOrDefault("")
    } else ""
    val retryable = GoogleRequestPolicy.canRetry(method, url, eventId)
    var attempt = 0
    while (true) {
        val c = connectionFactory(URL(url)).apply {
            instanceFollowRedirects = false
            connectTimeout = 20_000
            readTimeout = 30_000
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", type)
                c.outputStream.use { o -> o.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val status = c.responseCode
            if (status !in 200..299) {
                val error = apiError(c, status).let { error ->
                    ApiError(error.code, error.reason, GoogleRequestPolicy.calendarOperation(method, url))
                }
                if (retryable && attempt < 3 && GoogleRequestPolicy.transientStatus(status, error.reason)) {
                    val delay = GoogleRequestPolicy.delay(attempt, c.getHeaderField("Retry-After"), System.currentTimeMillis())
                    if (delay >= 0L) {
                        c.disconnect()
                        Thread.sleep(delay)
                        attempt++
                        continue
                    }
                }
                if (status == 401) {
                    Identity.getAuthorizationClient(appContext)
                        .clearToken(ClearTokenRequest.builder().setToken(accessToken).build())
                }
                throw error
            }
            if (status == 204) return JSONObject()
            val text = String(readResponse(c, LIMIT), StandardCharsets.UTF_8)
            return if (text.isEmpty()) JSONObject() else JSONObject(text)
        } finally {
            c.disconnect()
        }
    }
}

private fun readResponse(c: HttpURLConnection, limit: Int): ByteArray {
    c.inputStream.use { input ->
        ByteArrayOutputStream().use { out ->
            val b = ByteArray(8192)
            while (true) {
                val n = input.read(b)
                if (n == -1) break
                if (out.size() + n > limit) throw IOException("A válasz túl nagy.")
                out.write(b, 0, n)
            }
            return out.toByteArray()
        }
    }
}

internal fun enc(x: String): String = URLEncoder.encode(x, "UTF-8")


private fun apiError(connection: HttpURLConnection, code: Int): ApiError {
    var reason = ""
    try {
        connection.errorStream?.use { input ->
            ByteArrayOutputStream().use { out ->
                val b = ByteArray(1024)
                while (true) {
                    val n = input.read(b)
                    if (n == -1 || out.size() + n > 8192) break
                    out.write(b, 0, n)
                }
                val error = JSONObject(out.toString("UTF-8")).optJSONObject("error")
                val details = error?.optJSONArray("errors")
                if (details != null && details.length() > 0) {
                    reason = details.getJSONObject(0).optString("reason")
                }
            }
        }
    } catch (_: Exception) {
    }
    return ApiError(code, reason)
}

internal suspend fun GoogleSyncRepository.formatError(e: Exception): String {
    val lang = runCatching { settingsStore.current().language }.getOrDefault(Locale.getDefault().language)
    val network = GoogleRequestPolicy.networkMessage(e, lang)
    if (network != null) return network
    if (e is ApiError) return apiMessage(e, lang)
    return e.message ?: "A Google-művelet nem sikerült."
}

private fun apiMessage(error: ApiError, lang: String): String {
    val messages = when {
        error.code == 401 -> arrayOf(
            "A Google-engedély lejárt vagy visszavonták. Indíts kézi szinkront az új engedélykéréshez.",
            "Google access expired or was revoked. Run manual sync to request access again.",
            "Google-Zugriff ist abgelaufen oder widerrufen. Manuell synchronisieren, um erneut Zugriff anzufordern.",
            "Accesul Google a expirat sau a fost revocat. Pornește sincronizarea manuală pentru o nouă autorizare."
        )
        error.reason == "storageQuotaExceeded" -> arrayOf(
            "A Google Drive tárhelye megtelt. Szabadíts fel helyet, majd indíts kézi szinkront.",
            "Google Drive storage is full. Free some space, then run manual sync.",
            "Google Drive ist voll. Speicher freigeben und manuell synchronisieren.",
            "Spațiul Google Drive este plin. Eliberează spațiu și sincronizează manual."
        )
        error.code == 429 || GoogleRequestPolicy.transientStatus(error.code, error.reason) -> arrayOf(
            "A Google átmenetileg nem fogadja a kérést. Várj, majd próbáld újra; a helyi adatok megmaradnak.",
            "Google is temporarily refusing requests. Wait and retry; local data remains available.",
            "Google lehnt Anfragen vorübergehend ab. Warten und erneut versuchen; lokale Daten bleiben erhalten.",
            "Google refuză temporar cererile. Așteaptă și reîncearcă; datele locale rămân disponibile."
        )
        error.code == 403 -> arrayOf(
            "A Google nem engedélyezte ezt a műveletet. Ellenőrizd az engedélyt, az API/OAuth beállítást és a tanúsítványt.",
            "Google did not permit this operation. Check permission, API/OAuth configuration and the signing certificate.",
            "Google erlaubt diesen Vorgang nicht. Berechtigung, API/OAuth-Einstellung und Zertifikat prüfen.",
            "Google nu a permis operațiunea. Verifică permisiunea, configurația API/OAuth și certificatul."
        )
        else -> arrayOf(
            "A Google-művelet nem sikerült.",
            "The Google operation failed.",
            "Der Google-Vorgang ist fehlgeschlagen.",
            "Operațiunea Google a eșuat."
        )
    }
    val idx = when (lang) {
        "hu" -> 0
        "de" -> 2
        "ro" -> 3
        else -> 1
    }
    return "${messages[idx]} (${error.code}" + error.operation?.let { "; $it" }.orEmpty() + ")"
}
