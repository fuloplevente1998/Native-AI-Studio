package com.repforge.app

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import com.google.android.gms.common.api.CommonStatusCodes
import java.io.IOException

internal fun requiredGoogleCloudScopes(operation: String): List<String> = when {
    operation.startsWith("drive") -> listOf("https://www.googleapis.com/auth/drive.appdata")
    operation.startsWith("calendar") -> listOf(
        "https://www.googleapis.com/auth/calendar.app.created",
        "https://www.googleapis.com/auth/calendar.calendarlist.readonly"
    )
    else -> emptyList()
}

internal fun hasRequiredGoogleCloudScopes(operation: String, granted: Collection<String>?): Boolean =
    granted.orEmpty().containsAll(requiredGoogleCloudScopes(operation))

internal fun googleAuthorizationErrorMessage(statusCode: Int): Int = when (statusCode) {
    CommonStatusCodes.NETWORK_ERROR -> R.string.native_google_auth_network
    CommonStatusCodes.DEVELOPER_ERROR -> R.string.native_google_auth_configuration
    CommonStatusCodes.CANCELED -> R.string.native_google_consent_incomplete
    else -> R.string.native_google_auth_failed
}

internal fun <T> parseGoogleConsentResult(
    response: ActivityResult,
    strings: Resources,
    parse: (Intent) -> T
): T {
    val data = response.data
    if (data?.action == StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST &&
        data.hasExtra(StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION)
    ) {
        throw IOException(strings.getString(R.string.native_google_launch_failed))
    }
    if (data == null && response.resultCode == Activity.RESULT_OK) {
        throw IOException(strings.getString(R.string.native_google_consent_missing_result))
    }
    // Google may supply an error status in the result intent even when the activity did not return OK.
    val result = data?.let(parse)
    if (response.resultCode != Activity.RESULT_OK || result == null) {
        throw IOException(strings.getString(R.string.native_google_consent_incomplete))
    }
    return result
}
