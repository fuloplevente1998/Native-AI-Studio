package com.example

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.StudioPlatformAccess
import com.example.ui.viewmodel.TrainPilotViewModel
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlatformAccessRegressionTest {
    @Test fun locationSwitchReadsActualGrantAndRevocation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertFalse(StudioPlatformAccess.snapshot(app).getBoolean("gps"))
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertTrue(StudioPlatformAccess.snapshot(app).getBoolean("gps"))
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertFalse(StudioPlatformAccess.snapshot(app).getBoolean("gps"))
    }

    @Test fun noNodesAndNoGrantsCannotLeaveConnectedOrEnabledUiState() {
        val model = TrainPilotViewModel()
        val granted = JSONObject().put("gps", true).put("healthGranted", 2).put("healthTotal", 5)
        model.updatePlatformAccess(granted, listOf("Actual watch name"))
        assertTrue(model.uiState.value.watchConnected)
        model.updatePlatformAccess(JSONObject().put("healthTotal", 5), emptyList())
        val state = model.uiState.value
        assertFalse(state.watchConnected)
        assertFalse(state.permissionGpsEnabled)
        assertFalse(state.permissionHealthConnectEnabled)
        assertEquals(0, state.healthGrantedCount)
        assertEquals(-1, state.watchBatteryPercent)
        assertFalse(state.watchName.contains("Galaxy"))
    }
}
