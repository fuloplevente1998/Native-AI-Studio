package com.repforge.app.wear

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Optional background access, requested only after the ordinary sensor permission. */
internal object WearBackgroundSensorAccess {
    fun permission(sdk: Int): String? = when {
        sdk>=36 -> "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
        sdk>=33 -> "android.permission.BODY_SENSORS_BACKGROUND"
        else -> null
    }
    fun heartPermission(sdk: Int): String = if(sdk>=36)
        "android.permission.health.READ_HEART_RATE" else "android.permission.BODY_SENSORS"

    private fun granted(context: Context, permission: String) =
        context.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED

    fun allowed(context: Context): Boolean =
        granted(context,heartPermission(Build.VERSION.SDK_INT)) &&
            (permission(Build.VERSION.SDK_INT)?.let { granted(context,it) } ?: true)

    fun offer(activity: Activity, request: (String)->Unit) {
        val permission=permission(Build.VERSION.SDK_INT) ?: return
        if(!granted(activity,heartPermission(Build.VERSION.SDK_INT)) || granted(activity,permission))return
        val prefs=activity.getSharedPreferences("trainpilot_wear_background351",Context.MODE_PRIVATE)
        if(prefs.getBoolean("offered:$permission",false) || activity.isFinishing || activity.isDestroyed)return
        AlertDialog.Builder(activity)
            .setTitle("Automatikus órás edzésmérés")
            .setMessage("Ha a telefonon indítasz edzést, a TrainPilot az órán is megpróbálja elindítani a pulzusmérést anélkül, hogy megnyitnád az alkalmazást. Ehhez külön háttér-szenzorengedély szükséges. Ez opcionális; elutasítás esetén továbbra is mérhetsz a megnyitott óraalkalmazással. Az engedélyt később a rendszerbeállításokban módosíthatod.")
            .setPositiveButton("Engedélyezés") { _,_ -> request(permission) }
            .setNegativeButton("Most nem",null)
            .show()
        prefs.edit().putBoolean("offered:$permission",true).apply()
    }
}
