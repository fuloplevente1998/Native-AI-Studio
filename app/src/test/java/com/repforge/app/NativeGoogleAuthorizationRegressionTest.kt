package com.repforge.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeGoogleAuthorizationRegressionTest {
    private val strings get() = ApplicationProvider.getApplicationContext<Context>().resources

    @Test fun senderFailureAndMissingResultAreDistinctFromUncompletedConsent() {
        val launchFailure = Intent(StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST)
            .putExtra(StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION, IntentSender.SendIntentException())
        val cases = listOf(
            ActivityResult(Activity.RESULT_CANCELED, launchFailure) to R.string.native_google_launch_failed,
            ActivityResult(Activity.RESULT_OK, null) to R.string.native_google_consent_missing_result,
            ActivityResult(Activity.RESULT_CANCELED, null) to R.string.native_google_consent_incomplete
        )

        for ((response, message) in cases) {
            val error = assertThrows(IOException::class.java) {
                parseGoogleConsentResult(response, strings) { error("Should not parse a missing or synthetic result") }
            }
            assertEquals(strings.getString(message), error.message)
        }
    }

    @Test fun nonOkPayloadCanReportGoogleErrorButCannotCompleteAuthorization() {
        val data = Intent("test-consent-result")
        val rejected = ActivityResult(Activity.RESULT_CANCELED, data)
        val googleError = ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR))
        val reported = assertThrows(ApiException::class.java) {
            parseGoogleConsentResult(rejected, strings) { throw googleError }
        }
        assertEquals(R.string.native_google_auth_configuration, googleAuthorizationErrorMessage(reported.statusCode))
        assertEquals(R.string.native_google_auth_network, googleAuthorizationErrorMessage(CommonStatusCodes.NETWORK_ERROR))

        val incomplete = assertThrows(IOException::class.java) {
            parseGoogleConsentResult(rejected, strings) { it }
        }
        assertEquals(strings.getString(R.string.native_google_consent_incomplete), incomplete.message)
        assertSame(data, parseGoogleConsentResult(ActivityResult(Activity.RESULT_OK, data), strings) { it })
    }

    @Test fun driveAndCalendarRequireTheirOwnGrantedScopes() {
        val drive = "https://www.googleapis.com/auth/drive.appdata"
        val calendar = "https://www.googleapis.com/auth/calendar.app.created"
        val list = "https://www.googleapis.com/auth/calendar.calendarlist.readonly"

        assertTrue(hasRequiredGoogleCloudScopes("connect", null))
        assertFalse(hasRequiredGoogleCloudScopes("drive", null))
        assertFalse(hasRequiredGoogleCloudScopes("drive", listOf(calendar, list)))
        assertTrue(hasRequiredGoogleCloudScopes("drive", listOf(drive)))
        assertFalse(hasRequiredGoogleCloudScopes("calendar", listOf(calendar, drive)))
        assertFalse(hasRequiredGoogleCloudScopes("calendar-delete", listOf(list)))
        assertTrue(hasRequiredGoogleCloudScopes("calendar", listOf(calendar, list)))
    }
}
