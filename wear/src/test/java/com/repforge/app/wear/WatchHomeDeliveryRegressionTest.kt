package com.repforge.app.wear

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wearable.DataMap
import com.repforge.shared.WatchHomePayload
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WatchHomeDeliveryRegressionTest {
    private fun home(time: Long, id: String, theme: JSONObject) = JSONObject()
        .put("publishedAt", time).put("phoneVersion", "1.1-beta").put("journalSchema", 1)
        .put("recentWorkouts", JSONArray().put(JSONObject().put("workoutId", id)
            .put("started", "2026-11-13T20:39:00Z").put("finished", "2026-11-13T21:27:00Z")
            .put("dayName", "A").put("exercises", JSONArray())))
        .put("theme", theme).put("catalogPadding", "x".repeat(60000))
    private fun purple() = JSONObject().put("key", "phone-purple").put("accent", "#a58cc9")
        .put("accent2", "#8567b1").put("family", "matte").put("background", "#17141d")
        .put("surface", "#211d28").put("elevated", "#302938").put("text", "#ffffff")
        .put("muted", "#b5aabb").put("outline", "#625273").put("onAccent", "#15101b")

    @Test fun compressedNewHomeRefreshesJournalThemeAndVersionAndSurvivesRecreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("trainpilot_wear_home", Context.MODE_PRIVATE).edit().clear().commit()
        val map = DataMap(); WatchHomePayload.write(map, home(200, "newest", purple()).toString())
        val current = WatchHomeStore.save(context, WatchHomePayload.read(map), 200)!!
        assertEquals("newest", current.recentWorkouts.single().workoutId)
        assertEquals("1.1-beta", current.phoneVersion)
        assertEquals("phone-purple", current.theme.key)
        assertEquals(current.theme.accentColor, current.theme.materialColors().primary)
        assertEquals(current.theme.surfaceColor, current.theme.materialColors().surfaceContainer)
        assertEquals(current.theme.backgroundColor, current.theme.materialColors().background)
        val restored = WatchHomeStore.load(context)!!
        assertEquals(current, restored)
    }

    @Test fun delayedOrMalformedDataCannotBringBackOldJournalOrBlueTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("trainpilot_wear_home", Context.MODE_PRIVATE).edit().clear().commit()
        val latest = WatchHomeStore.save(context, home(200, "latest", purple()).toString())!!
        val old = DataMap(); WatchHomePayload.write(old, home(100, "old", WearHomeTheme.DEFAULT.toJson()).toString())
        assertEquals(latest, WatchHomeStore.save(context, WatchHomePayload.read(old)))
        assertNull(WatchHomeStore.save(context, WatchHomePayload.read(DataMap())))
        assertEquals(latest, WatchHomeStore.load(context))
    }
}
