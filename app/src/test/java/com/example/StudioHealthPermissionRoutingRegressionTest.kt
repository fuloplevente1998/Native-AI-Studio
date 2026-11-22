package com.example

import android.content.Intent
import android.content.Context
import android.health.connect.HealthConnectManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.StudioHealthPermissionRouting
import com.repforge.app.HealthJournalStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Device-reported SecurityException: do not open the privileged health permissions controller. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudioHealthPermissionRoutingRegressionTest {
    private val steps = "android.permission.health.READ_STEPS"
    private val pulse = "android.permission.health.READ_HEART_RATE"
    private val sleep = "android.permission.health.READ_SLEEP"
    private val declared = listOf(steps, pulse, sleep)

    @Test fun noHealthGrantsRequestsEveryDeclaredRead() {
        assertEquals(declared, StudioHealthPermissionRouting.missingReads(declared) { false })
    }

    @Test fun partialHealthGrantsRequestOnlyMissingReads() {
        assertEquals(listOf(pulse, sleep),
            StudioHealthPermissionRouting.missingReads(declared) { it == steps })
        assertEquals(listOf(sleep),
            StudioHealthPermissionRouting.missingReads(declared) { it != sleep })
    }

    @Test fun allGrantedOpensSettingsInsteadOfRequestingAgain() {
        assertTrue(StudioHealthPermissionRouting.missingReads(declared) { true }.isEmpty())
        val intent = StudioHealthPermissionRouting.settingsIntent()
        assertEquals("android.health.connect.action.HEALTH_HOME_SETTINGS", intent.action)
        assertFalse(intent.action == HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS)
        assertFalse(intent.hasExtra(Intent.EXTRA_PACKAGE_NAME))
    }

    @Test fun healthSourcesUseTheirProviderPermissionManager() {
        assertEquals("samsung_manage", StudioHealthPermissionRouting.sourceManageAction(HealthJournalStore.SAMSUNG))
        assertEquals("hc_manage", StudioHealthPermissionRouting.sourceManageAction(HealthJournalStore.HC))
    }

    @Test fun generalPermissionManagementOpensOwnAppSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fallback = StudioHealthPermissionRouting.fallbackIntent(context)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, fallback.action)
        assertEquals("package:${context.packageName}", fallback.data.toString())
        assertFalse(fallback.action == HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS)
    }
}
