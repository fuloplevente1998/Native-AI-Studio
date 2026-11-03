package com.repforge.shared

import org.json.JSONObject

/** Read-only request/reply; never an ownership, workout or health command. */
object WatchDeviceInfo {
    const val REQUEST = "/trainpilot/device-info/request"
    const val REPLY = "/trainpilot/device-info/reply"
    fun requestId(raw: ByteArray): String? = if (raw.size > 2048) null else runCatching {
        JSONObject(raw.toString(Charsets.UTF_8)).optString("requestId").takeIf {
            it.matches(Regex("[a-f0-9-]{36}"))
        }
    }.getOrNull()
    fun accepts(raw: ByteArray, expected: String): Boolean = requestId(raw) == expected
    fun battery(value: Int): Int? = value.takeIf { it in 0..100 }
}
