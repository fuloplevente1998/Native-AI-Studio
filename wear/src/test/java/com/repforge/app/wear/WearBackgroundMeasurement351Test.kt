package com.repforge.app.wear

import com.repforge.shared.WearMeasurementRequest351
import org.junit.Assert.*
import org.junit.Test

class WearBackgroundMeasurement351Test {
    @Test fun usesVersionSpecificBackgroundGrant() {
        assertNull(WearBackgroundSensorAccess.permission(32))
        assertEquals("android.permission.BODY_SENSORS_BACKGROUND",WearBackgroundSensorAccess.permission(33))
        assertEquals("android.permission.BODY_SENSORS_BACKGROUND",WearBackgroundSensorAccess.permission(35))
        assertEquals("android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND",WearBackgroundSensorAccess.permission(36))
        assertEquals("android.permission.health.READ_HEART_RATE",WearBackgroundSensorAccess.heartPermission(36))
    }

    @Test fun requestIsScopedToCurrentRevisionEpochAndTime() {
        val request=WearMeasurementRequest351("w",42,3,100000)
        val restored=WearMeasurementRequest351.parse(request.encode())!!
        assertEquals(request,restored)
        assertTrue(restored.matches("w",43,3,101000))
        assertFalse(restored.matches("other",43,3,101000))
        assertFalse(restored.matches("w",41,3,101000))
        assertFalse(restored.matches("w",43,4,101000))
        assertFalse(restored.matches("w",43,3,116000))
        assertNull(WearMeasurementRequest351.parse(ByteArray(4097)))
    }
}
