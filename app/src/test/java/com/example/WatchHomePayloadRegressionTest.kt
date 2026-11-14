package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wearable.DataMap
import com.repforge.app.WearSyncRepository
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotUserSettings
import com.example.ui.theme.TrainPilotPalettes
import com.repforge.shared.WatchHomePayload
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WatchHomePayloadRegressionTest {
    @Test fun realCatalogAndBoundedJournalExceedLegacyItemBudgetButRoundTripWithoutDroppingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).allowMainThreadQueries().build()
        try {
            val home = WearSyncRepository.buildWatchHomeSnapshot(context, database = db)
            val rows = JSONArray()
            repeat(8) { workout ->
                val exercises = JSONArray()
                repeat(14) { exercise ->
                    val sets = JSONArray()
                    repeat(12) { set -> sets.put(JSONObject().put("number", set + 1).put("reps", "15")
                        .put("weight", 19.7).put("leftSeconds", 20).put("rightSeconds", 20)) }
                    exercises.put(JSONObject().put("name", "Gyakorlat $exercise").put("sets", sets))
                }
                rows.put(JSONObject().put("workoutId", "recent-$workout").put("started", "2026-11-13T20:00:00Z")
                    .put("finished", "2026-11-13T20:48:00Z").put("dayName", "A").put("exercises", exercises))
            }
            home.put("recentWorkouts", rows)
            val raw = home.toString()
            assertTrue(raw.toByteArray(Charsets.UTF_8).size > 100 * 1024)
            val wire = DataMap()
            WatchHomePayload.write(wire, raw)
            assertFalse(wire.containsKey("snapshot"))
            assertTrue(wire.toByteArray().size < 100 * 1024)
            assertEquals(raw, WatchHomePayload.read(wire))
            val received = JSONObject(WatchHomePayload.read(wire)!!)
            assertEquals(home.getJSONArray("quickExercises").length(), received.getJSONArray("quickExercises").length())
            assertEquals(home.getJSONArray("calendarDays").length(), received.getJSONArray("calendarDays").length())
            assertEquals(8, received.getJSONArray("recentWorkouts").length())
            println("Watch home fixture: decoded=${raw.toByteArray(Charsets.UTF_8).size}, DataMap=${wire.toByteArray().size}")
        } finally { db.close() }
    }

    @Test fun everyPhonePaletteKeepsAllItsColorsInsideCompressedHome() {
        TrainPilotPalettes.allPalettes.forEach { palette ->
            val theme = TrainPilotUserSettings(themeKey = palette.key).themeJson()
            val home = JSONObject().put("theme", theme).put("padding", "x".repeat(60000))
            val map = DataMap(); WatchHomePayload.write(map, home.toString())
            assertEquals(theme.toString(), JSONObject(WatchHomePayload.read(map)!!).getJSONObject("theme").toString())
        }
    }

    @Test fun smallLegacySnapshotStillUsesItsOriginalWireField() {
        val raw = JSONObject().put("phoneVersion", "1.1-beta").put("recentWorkouts", JSONArray()).toString()
        val map = DataMap(); WatchHomePayload.write(map, raw)
        assertEquals(raw, map.getString("snapshot"))
        assertEquals(raw, WatchHomePayload.read(map))
    }

    @Test fun reusingAMapCannotReviveThePreviousEncodingOrTheme() {
        val map = DataMap()
        val large = JSONObject().put("theme", "purple").put("padding", "x".repeat(60000)).toString()
        val small = JSONObject().put("theme", "orange").toString()
        WatchHomePayload.write(map, large)
        WatchHomePayload.write(map, small)
        assertEquals(small, WatchHomePayload.read(map))
        assertFalse(map.containsKey("snapshotGzip"))
        WatchHomePayload.write(map, large)
        assertEquals(large, WatchHomePayload.read(map))
        assertFalse(map.containsKey("snapshot"))
    }

    @Test fun corruptUnknownAndOversizedCompressedPacketsAreRejected() {
        val corrupt = DataMap().apply { putInt("snapshotEncoding", 1); putByteArray("snapshotGzip", byteArrayOf(1, 2, 3)) }
        assertNull(WatchHomePayload.read(corrupt))
        val unknown = DataMap().apply { putInt("snapshotEncoding", 99); putByteArray("snapshotGzip", byteArrayOf()) }
        assertNull(WatchHomePayload.read(unknown))
        val tooLarge = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write(ByteArray(WatchHomePayload.RAW_LIMIT + 1)) }
        }.toByteArray()
        val bomb = DataMap().apply { putInt("snapshotEncoding", 1); putByteArray("snapshotGzip", tooLarge) }
        assertNull(WatchHomePayload.read(bomb))
    }
}
