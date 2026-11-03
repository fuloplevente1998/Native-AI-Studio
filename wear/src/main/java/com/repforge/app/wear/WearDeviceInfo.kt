package com.repforge.app.wear

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.google.android.gms.wearable.Wearable
import com.repforge.shared.WatchDeviceInfo
import org.json.JSONObject

internal object WearDeviceInfo {
    fun reply(context: Context, nodeId: String, bytes: ByteArray) {
        val requestId = WatchDeviceInfo.requestId(bytes) ?: return
        val capacity = context.getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
        val result = JSONObject().put("requestId", requestId)
            .put("battery", WatchDeviceInfo.battery(capacity) ?: JSONObject.NULL)
            .put("model", Build.MODEL).put("os", Build.VERSION.RELEASE)
            .put("appVersion", version ?: JSONObject.NULL)
        Wearable.getMessageClient(context).sendMessage(nodeId, WatchDeviceInfo.REPLY,
            result.toString().toByteArray(Charsets.UTF_8))
    }
}
