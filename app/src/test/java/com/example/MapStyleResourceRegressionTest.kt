package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.R
import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MapStyleResourceRegressionTest {
    @Test fun darkStyleIsValidGoogleMapsStyleJson() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val json = context.resources.openRawResource(R.raw.trainpilot_map_dark)
            .bufferedReader().use { it.readText() }
        val styles = JSONArray(json)
        assertTrue(styles.length() > 0)
        assertTrue((0 until styles.length()).any { i ->
            styles.getJSONObject(i).optString("elementType") == "geometry"
        })
        assertTrue((0 until styles.length()).any { i ->
            styles.getJSONObject(i).optString("featureType") == "water"
        })
    }
}
