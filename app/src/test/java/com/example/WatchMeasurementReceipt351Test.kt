package com.example

import com.repforge.app.PhoneMeasurementReconnect351
import com.repforge.shared.WearMeasurementRequest351
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class WatchMeasurementReceipt351Test {
    private fun receipt()=JSONObject().put("workoutId","w").put("sourceNode351","watch")
        .put("ownerEpoch351",3).put("updatedAt",100000).put("state","active")

    @Test fun receiptRequiresCurrentNodeEpochAndFreshness() {
        val value=receipt()
        assertNotNull(PhoneMeasurementReconnect351.freshReceipt(value,"w",3,"watch",101000))
        assertNull(PhoneMeasurementReconnect351.freshReceipt(value,"old",3,"watch",101000))
        assertNull(PhoneMeasurementReconnect351.freshReceipt(value,"w",4,"watch",101000))
        assertNull(PhoneMeasurementReconnect351.freshReceipt(value,"w",3,"other",101000))
        assertNull(PhoneMeasurementReconnect351.freshReceipt(value,"w",3,null,101000))
        assertNull(PhoneMeasurementReconnect351.freshReceipt(value,"w",3,"watch",146000))
        assertNull(PhoneMeasurementReconnect351.freshReceipt(value,"w",3,"watch",94000))
    }

    @Test fun reconnectRequestCannotStartClosedPausedOrWatchOwnedWorkout() {
        fun frame()=JSONObject().put("workoutId","w").put("active",true).put("revision",42)
            .put("ownerEpoch193",3).put("timerOwner","phone")
        assertNotNull(WearMeasurementRequest351.fromFrame(frame(),100000))
        assertNull(WearMeasurementRequest351.fromFrame(frame().put("active",false),100000))
        assertNull(WearMeasurementRequest351.fromFrame(frame().put("workoutPaused",true),100000))
        assertNull(WearMeasurementRequest351.fromFrame(frame().put("finalizationPending",true),100000))
        assertNull(WearMeasurementRequest351.fromFrame(frame().put("discardPending",true),100000))
        assertNull(WearMeasurementRequest351.fromFrame(frame().put("timerOwner","watch"),100000))
    }
}
