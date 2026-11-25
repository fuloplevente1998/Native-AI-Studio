package com.repforge.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 2809/1 B: optional Liquid Glass background image. The picked image is copied, downscaled and
 * re-encoded into app-private storage (filesDir, excluded from cloud backup and device transfer by
 * the backup rules, never part of the Drive JSON or the photo ZIP). Settings and portable backups
 * hold only the relative private path; a restore onto another device without the file simply shows
 * the living backdrop.
 */
internal object GlassBackdropImages {
    const val DIR = "glass_backdrop"
    const val PORTABLE_KEY = "glassBackdropImage2809"
    private const val MAX_EDGE = 1440
    private const val QUALITY = 86
    private const val SOURCE_LIMIT = 40L * 1024 * 1024
    private val pathPattern = Regex("""glass_backdrop/backdrop_[0-9a-f]{32}\.jpg""")

    /** Only our own file names are accepted, so a backup can never point at another private file. */
    fun isValidPath(path: String): Boolean = pathPattern.matches(path)

    /** The portable (backup) value: the validated relative path, or "" for none. Never image bytes. */
    fun portable(path: String): String = if (isValidPath(path)) path else ""

    fun file(context: Context, path: String): File? = if (isValidPath(path)) File(context.filesDir, path) else null

    /** Decoded image for the backdrop; null when missing (e.g. restored on another device) or unreadable. */
    fun load(context: Context, path: String): Bitmap? {
        val file = file(context, path)?.takeIf { it.isFile } ?: return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    /** Import, then persist; the previous image is deleted only after the new choice is durable. */
    suspend fun choose(context: Context, uri: Uri, store: TrainPilotSettingsStore,
        ticket: NativeDataReplacement.Continuation): Unit = withContext(Dispatchers.IO + NonCancellable) {
        val created = import(context, uri)
        val previous = try {
            store.setGlassBackdropImageForDataset(created, ticket)
        } catch (error: Exception) {
            file(context, created)?.delete()
            throw error
        }
        if (previous != created) file(context, previous)?.delete()
    }

    /** Remove the choice durably first, then the private file. */
    suspend fun clear(context: Context, store: TrainPilotSettingsStore, ticket: NativeDataReplacement.Continuation): Unit =
        withContext(Dispatchers.IO + NonCancellable) {
            val previous = store.setGlassBackdropImageForDataset("", ticket)
            file(context, previous)?.delete()
        }

    private fun import(context: Context, uri: Uri): String {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val name = "$DIR/backdrop_" + UUID.randomUUID().toString().replace("-", "").lowercase(Locale.ROOT) + ".jpg"
        val source = File.createTempFile("glass-backdrop-", ".img", context.cacheDir)
        val partial = File(dir, "partial-" + System.nanoTime() + ".jpg")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw IOException("Image not readable")
                FileOutputStream(source).use { out ->
                    val buffer = ByteArray(16384)
                    var total = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n == -1) break
                        total += n
                        if (total > SOURCE_LIMIT) throw IOException("Image too large")
                        out.write(buffer, 0, n)
                    }
                }
            }
            normalize(source, partial)
            val target = File(context.filesDir, name)
            if (!partial.renameTo(target)) throw IOException("Image not stored")
            return name
        } finally {
            source.delete()
            partial.delete()
        }
    }

    private fun normalize(src: File, dest: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Unsupported image")
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_EDGE * 2) sample *= 2
        var bitmap = BitmapFactory.decodeFile(src.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("Image not decodable")
        try {
            val rotation = runCatching {
                when (ExifInterface(src.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }.getOrDefault(0f)
            val scale = min(1f, MAX_EDGE.toFloat() / max(bitmap.width, bitmap.height))
            if (rotation != 0f || scale < 1f) {
                val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
                val next = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (next !== bitmap) { bitmap.recycle(); bitmap = next }
            }
            FileOutputStream(dest).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)) throw IOException("Image not encoded")
            }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    /** Pixel size after downscaling, for tests: the longer edge never exceeds [MAX_EDGE]. */
    fun storedSize(width: Int, height: Int): Pair<Int, Int> {
        val scale = min(1f, MAX_EDGE.toFloat() / max(width, height).coerceAtLeast(1))
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }
}
