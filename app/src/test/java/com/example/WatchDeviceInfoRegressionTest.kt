package com.example

import com.repforge.shared.WatchDeviceInfo
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class WatchDeviceInfoRegressionTest {
    private val id = "12345678-1234-1234-1234-123456789012"
    @Test fun replyMustMatchTheActualRequest() {
        val bytes = "{\"requestId\":\"$id\",\"battery\":75}".toByteArray()
        assertTrue(WatchDeviceInfo.accepts(bytes, id))
        assertFalse(WatchDeviceInfo.accepts(bytes, "another-request"))
    }
    @Test fun oversizedMalformedAndUnsolicitedRepliesAreRejected() {
        assertNull(WatchDeviceInfo.requestId(ByteArray(3000)))
        assertNull(WatchDeviceInfo.requestId("not json".toByteArray()))
        assertNull(WatchDeviceInfo.requestId("{}".toByteArray()))
    }
    @Test fun unavailableBatteryIsNotFabricatedAsZeroOrOneHundred() {
        assertNull(WatchDeviceInfo.battery(-1)); assertNull(WatchDeviceInfo.battery(Int.MIN_VALUE))
        assertNull(WatchDeviceInfo.battery(101)); assertEquals(0, WatchDeviceInfo.battery(0))
        assertEquals(100, WatchDeviceInfo.battery(100))
    }
}
