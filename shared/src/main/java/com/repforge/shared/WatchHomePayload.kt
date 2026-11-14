package com.repforge.shared

import com.google.android.gms.wearable.DataMap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Home contains the catalog, programs, journal and theme. Keep it below DataItem's 100KB cap. */
object WatchHomePayload {
    const val INLINE_LIMIT = 48 * 1024
    const val PACKED_LIMIT = 64 * 1024
    const val RAW_LIMIT = 2 * 1024 * 1024
    private const val GZIP = "snapshotGzip"
    private const val FORMAT = "snapshotEncoding"

    fun write(map: DataMap, snapshot: String) {
        val bytes = snapshot.toByteArray(Charsets.UTF_8)
        require(bytes.size <= RAW_LIMIT) { "Watch home exceeds the decoded payload budget" }
        if (bytes.size <= INLINE_LIMIT) {
            map.remove(GZIP)
            map.remove(FORMAT)
            map.putString("snapshot", snapshot)
        } else {
            val compressed = ByteArrayOutputStream().also { output ->
                GZIPOutputStream(output).use { it.write(bytes) }
            }.toByteArray()
            require(compressed.size <= PACKED_LIMIT) { "Watch home exceeds the compressed payload budget" }
            map.remove("snapshot")
            map.putInt(FORMAT, 1)
            map.putByteArray(GZIP, compressed)
        }
    }

    /** Corrupt/unknown packets never replace the last durable home snapshot. */
    fun read(map: DataMap): String? = try {
        if (!map.containsKey(GZIP)) {
            map.getString("snapshot")?.takeIf { it.toByteArray(Charsets.UTF_8).size <= RAW_LIMIT }
        } else if (map.getInt(FORMAT) != 1) null else {
            val bytes = map.getByteArray(GZIP)
            if (bytes == null || bytes.size > PACKED_LIMIT) null else {
                val output = ByteArrayOutputStream()
                GZIPInputStream(bytes.inputStream()).use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        require(output.size() + count <= RAW_LIMIT) { "Inflated watch home exceeds budget" }
                        output.write(buffer, 0, count)
                    }
                }
                Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(output.toByteArray())).toString()
            }
        }
    } catch (_: Exception) { null }
}
