package com.repforge.app

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer

/** JSON trees remain shared; never materialize a second whole-document string/byte array. */
internal object BackupJsonIO {
    fun read(reader: Reader): Any = JsonReader(reader).use { json ->
        // Historical org.json exports accepted lenient JSON. Preserve that compatibility.
        json.isLenient = true
        val value = readValue(json, 0)
        if (json.peek() != JsonToken.END_DOCUMENT) throw IOException("Unexpected trailing backup data")
        value
    }

    private fun readValue(json: JsonReader, depth: Int): Any {
        if (depth > 128) throw IOException("Backup nesting is too deep")
        return when (json.peek()) {
            JsonToken.BEGIN_OBJECT -> JSONObject().also { out ->
                json.beginObject()
                while (json.hasNext()) out.put(json.nextName(), readValue(json, depth + 1))
                json.endObject()
            }
            JsonToken.BEGIN_ARRAY -> JSONArray().also { out ->
                json.beginArray()
                while (json.hasNext()) out.put(readValue(json, depth + 1))
                json.endArray()
            }
            JsonToken.STRING -> json.nextString()
            JsonToken.NUMBER -> JSONTokener(json.nextString()).nextValue()
            JsonToken.BOOLEAN -> json.nextBoolean()
            JsonToken.NULL -> { json.nextNull(); JSONObject.NULL }
            else -> throw IOException("Invalid backup JSON")
        }
    }

    fun readFile(file: File, maxBytes: Int): Any {
        if (file.length() !in 1..maxBytes.toLong()) throw IOException("Invalid backup size (maximum 20 MB)")
        return read(InputStreamReader(FileInputStream(file), Charsets.UTF_8).buffered())
    }

    fun write(value: Any, writer: Writer) {
        val json = JsonWriter(writer)
        json.serializeNulls = true
        writeValue(json, value, 0)
        json.flush()
    }

    private fun writeValue(json: JsonWriter, value: Any?, depth: Int) {
        if (depth > 128) throw IOException("Backup nesting is too deep")
        when {
            value == null || value === JSONObject.NULL -> json.nullValue()
            value is JSONObject -> {
                json.beginObject()
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    json.name(key)
                    writeValue(json, value.get(key), depth + 1)
                }
                json.endObject()
            }
            value is JSONArray -> {
                json.beginArray()
                for (index in 0 until value.length()) writeValue(json, value.get(index), depth + 1)
                json.endArray()
            }
            value is Boolean -> json.value(value)
            value is Number -> json.value(value)
            value is String -> json.value(value)
            else -> throw IOException("Unsupported backup JSON value")
        }
    }

    fun writeFile(file: File, value: Any, maxBytes: Int) {
        FileOutputStream(file).use { stream ->
            // Check bytes as they are written, before a staged document can be installed.
            val bounded = object : java.io.FilterOutputStream(stream) {
                var count = 0L
                override fun write(value: Int) { checkSize(1); out.write(value) }
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    checkSize(length); out.write(bytes, offset, length)
                }
                fun checkSize(length: Int) {
                    count += length
                    if (count > maxBytes) throw IOException("Backup exceeds maximum size")
                }
            }
            val writer = OutputStreamWriter(bounded, Charsets.UTF_8).buffered()
            write(value, writer)
            writer.flush()
            stream.fd.sync()
        }
    }

    fun requireWithinCharacters(value: Any, maximum: Int) {
        write(value, object : Writer() {
            var count = 0L
            override fun write(chars: CharArray, offset: Int, length: Int) {
                count += length
                if (count > maximum) throw IOException("Health backup exceeds 18 MB")
            }
            override fun write(text: String, offset: Int, length: Int) {
                count += length
                if (count > maximum) throw IOException("Health backup exceeds 18 MB")
            }
            override fun flush() = Unit
            override fun close() = Unit
        })
    }
}
