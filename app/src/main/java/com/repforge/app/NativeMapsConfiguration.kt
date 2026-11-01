package com.repforge.app

import android.content.Context
import android.content.pm.PackageManager

/** Never expose the Android-restricted Maps key to UI, logs or backup metadata. */
internal object NativeMapsConfiguration {
    fun configured(context: Context): Boolean = runCatching {
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val metadata = info.metaData ?: return@runCatching false
        val value = when (val raw = metadata.get("com.google.android.geo.API_KEY")) {
            is String -> raw
            is Int -> context.getString(raw)
            else -> ""
        }.trim()
        value.isNotEmpty() && !value.startsWith("\${") && value !in setOf("YOUR_API_KEY", "DEFAULT_API_KEY")
    }.getOrDefault(false)
}
