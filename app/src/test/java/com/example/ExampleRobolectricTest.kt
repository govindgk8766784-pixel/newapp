package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PixelPro", appName)
  }

  @Test
  fun `verify passport presets exist and have valid aspect ratios`() {
    val presets = com.example.data.model.PassportPreset.ALL
    assertTrue(presets.isNotEmpty())
    val indiaPassport = presets.first { it.id == "in_pass" }
    assertEquals(35f, indiaPassport.widthMm)
    assertEquals(45f, indiaPassport.heightMm)
    assertTrue(indiaPassport.aspectRatio > 0.7f && indiaPassport.aspectRatio < 0.8f)
  }

  @Test
  fun `verify id card presets exist`() {
    val presets = com.example.data.model.IdCardPreset.ALL
    assertTrue(presets.isNotEmpty())
    val aadhaar = presets.first { it.id == "aadhaar" }
    assertEquals(85.6f, aadhaar.widthMm)
    assertTrue(aadhaar.isDualSided)
  }
}
