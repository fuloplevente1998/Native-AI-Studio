package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Health Connect lists Android 14+ apps only when the manifest exports the permission rationale. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthConnectManifestRegressionTest {
    @Suppress("DEPRECATION")
    @Test fun declaresHealthReadPermissionsAndSystemRationaleEntryPoints() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkg = context.packageManager.getPackageInfo(context.packageName,
            PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES)
        assertTrue(pkg.requestedPermissions.orEmpty().contains("android.permission.health.READ_HEART_RATE"))
        assertTrue(pkg.activities.orEmpty().any {
            it.name == "com.repforge.app.HealthPrivacyActivity" && it.exported
        })
        assertTrue(pkg.activities.orEmpty().any {
            it.name == "com.repforge.app.HealthPermissionsUsageActivity" && it.exported &&
                it.permission == "android.permission.START_VIEW_PERMISSION_USAGE"
        })
        val usage = Intent(Intent.ACTION_VIEW_PERMISSION_USAGE)
            .addCategory("android.intent.category.HEALTH_PERMISSIONS")
        assertTrue(context.packageManager.queryIntentActivities(usage, 0).any {
            it.activityInfo.name == "com.repforge.app.HealthPermissionsUsageActivity"
        })
    }
}
