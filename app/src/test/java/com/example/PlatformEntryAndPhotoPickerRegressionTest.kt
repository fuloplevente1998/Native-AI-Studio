package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.WorkoutPhotoRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PlatformEntryAndPhotoPickerRegressionTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test @Config(sdk=[34]) fun android13AndLaterUseSystemImagePicker() {
        val intent=WorkoutPhotoRepository(context).createPickImageIntent()
        assertEquals(MediaStore.ACTION_PICK_IMAGES,intent.action)
        assertEquals("image/*",intent.type)
    }
    @Test @Config(sdk=[28]) fun olderDevicesKeepTheOpenableImageFallback() {
        val intent=WorkoutPhotoRepository(context).createPickImageIntent()
        assertEquals(Intent.ACTION_GET_CONTENT,intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertEquals("image/*",intent.type)
    }
    @Test @Config(sdk=[34]) fun onlyMigrationLauncherIsExternallyLaunchable() {
        val pm=context.packageManager
        assertFalse(pm.getActivityInfo(ComponentName(context,com.example.MainActivity::class.java),0).exported)
        assertTrue(pm.getActivityInfo(ComponentName(context,com.repforge.app.MainActivity::class.java),0).exported)
    }
}
