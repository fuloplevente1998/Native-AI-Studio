package com.repforge.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import com.repforge.app.NativeGoogleConsentBridge

/** A distinct registry key for each attempt prevents a late result completing a newer request. */
@Composable
internal fun NativeGoogleConsentLauncher(
    prompt: NativeGoogleConsentBridge.Prompt?,
    take: (String) -> NativeGoogleConsentBridge.Prompt?,
    complete: (String, ActivityResult) -> Unit,
    fail: (String) -> Unit
) {
    if (prompt != null) key(prompt.id) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            complete(prompt.id, result)
        }
        LaunchedEffect(prompt.id) {
            val pending = take(prompt.id) ?: return@LaunchedEffect
            try { launcher.launch(IntentSenderRequest.Builder(pending.pendingIntent.intentSender).build()) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { fail(prompt.id) }
        }
    }
}
