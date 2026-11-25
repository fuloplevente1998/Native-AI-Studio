package com.example.ui.theme

import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.runtime.getValue
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * System "Remove animations" / animator duration scale 0 turns off glass travel, squash and
 * press scaling ([GlassMotion.enabled]). Re-read when the configuration changes (e.g. returning
 * from Settings recreates/reconfigures the activity).
 */
@Composable
internal fun rememberGlassMotionEnabled(): Boolean {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        GlassMotion.enabled(scale)
    }
}

/**
 * 2811/G5: system battery saver. Re-read whenever the screen's lifecycle state changes (returning from the
 * quick settings or Settings resumes the activity), so no broadcast receiver is needed.
 */
@Composable
internal fun rememberGlassPowerSave(): Boolean {
    val context = LocalContext.current
    val lifecycle by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return remember(context, lifecycle) {
        runCatching {
            (context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager)?.isPowerSaveMode == true
        }.getOrDefault(false)
    }
}
