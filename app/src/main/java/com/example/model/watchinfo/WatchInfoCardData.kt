package com.example.model.watchinfo

import org.json.JSONObject

/** UI-only state. The payload must come from NativeWatchDeviceInfo's node/requestId-checked reply. */
enum class WatchInfoStatus { IDLE, SUCCESS, ERROR, TIMEOUT }

data class WatchInfoCardData(
    val name: String? = null,
    val model: String? = null,
    val androidVersion: String? = null,
    val appVersion: String? = null,
    val appBuild: Long? = null,
    val batteryPercent: Int? = null,
    val connected: Boolean = false,
    val appAvailable: Boolean = false,
    val loading: Boolean = false,
    val status: String? = null,
    val statusKind: WatchInfoStatus = WatchInfoStatus.IDLE,
    val lastSync: String? = null
) {
    companion object {
        /**
         * Accepts the actual Wear JSON, not presentation text. Call only AFTER the existing
         * NativeWatchDeviceInfo request has validated the source node, path and requestId.
         * Missing/invalid values remain unknown; in particular battery is never guessed as 0%.
         */
        fun fromVerifiedReply(
            nodeName: String,
            reply: JSONObject,
            connected: Boolean = true,
            appAvailable: Boolean = true,
            loading: Boolean = false,
            status: String? = null,
            statusKind: WatchInfoStatus = WatchInfoStatus.SUCCESS,
            lastSync: String? = null
        ) = WatchInfoCardData(
            name = nodeName.trim().takeIf(String::isNotEmpty),
            model = reply.optionalText("model"),
            androidVersion = reply.optionalText("os"),
            appVersion = reply.optionalText("appVersion"),
            appBuild = reply.optionalBuild("appBuild"),
            batteryPercent = reply.optionalBattery("battery"),
            connected = connected,
            appAvailable = appAvailable,
            loading = loading,
            status = status,
            statusKind = statusKind,
            lastSync = lastSync
        )

        private fun JSONObject.optionalText(key: String): String? =
            (opt(key) as? String)?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

        private fun JSONObject.optionalBattery(key: String): Int? =
            (opt(key) as? Number)?.toDouble()?.takeIf {
                it.isFinite() && it >= 0.0 && it <= 100.0 && it % 1.0 == 0.0
            }?.toInt()

        private fun JSONObject.optionalBuild(key: String): Long? =
            (opt(key) as? Number)?.toDouble()?.takeIf {
                it.isFinite() && it >= 1.0 && it < Long.MAX_VALUE.toDouble() && it % 1.0 == 0.0
            }?.toLong()
    }
}
