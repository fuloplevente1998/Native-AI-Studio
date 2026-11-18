package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrainPilotStatusIconRegressionTest {
    @Test fun notificationDrawableRendersAsAWhiteMaskWithTransparentSurroundings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = requireNotNull(AppCompatResources.getDrawable(context, R.drawable.ic_stat_trainpilot))
        val bitmap = Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888)
        drawable.setBounds(0,0,96,96);drawable.draw(Canvas(bitmap))
        var painted=0
        for(y in 0 until 96) for(x in 0 until 96) {
            val pixel=bitmap.getPixel(x,y)
            if(Color.alpha(pixel)>0) {
                painted++
                assertEquals(255,Color.red(pixel));assertEquals(255,Color.green(pixel));assertEquals(255,Color.blue(pixel))
            }
        }
        assertTrue("The dumbbell must be visible",painted>500)
        assertTrue("A notification mask must not contain an opaque launcher tile",painted<96*96/2)
        for((x,y) in listOf(0 to 0,95 to 95,48 to 0,0 to 48)) assertEquals(0,Color.alpha(bitmap.getPixel(x,y)))
        assertEquals(255,Color.alpha(bitmap.getPixel(48,48)))
    }
}
