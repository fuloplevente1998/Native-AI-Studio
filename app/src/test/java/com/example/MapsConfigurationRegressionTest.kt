package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.NativeMapsConfiguration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MapsConfigurationRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun metadata(value: Any?) {
        val pkg = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA)
        pkg.applicationInfo!!.metaData = Bundle().apply {
            when (value) {
                is String -> putString("com.google.android.geo.API_KEY", value)
                is Int -> putInt("com.google.android.geo.API_KEY", value)
            }
        }
        shadowOf(context.packageManager).installPackage(pkg)
    }

    @Test fun missingBlankAndUnexpandedKeysAreUnavailable() {
        listOf(null, "", " ", "\${TRAINPILOT_MAPS_API_KEY}", "YOUR_API_KEY").forEach {
            metadata(it)
            assertFalse(NativeMapsConfiguration.configured(context))
        }
    }

    @Test fun literalAndStringResourceMetadataAreRecognized() {
        metadata("test-only-restricted-maps-key")
        assertTrue(NativeMapsConfiguration.configured(context))
        metadata(com.repforge.app.R.string.app_name)
        assertTrue(NativeMapsConfiguration.configured(context))
    }
}
