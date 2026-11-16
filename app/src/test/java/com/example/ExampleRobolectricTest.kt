package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Novie", appName)
  }

  @Test
  fun `launch MainActivity renders welcome screen and enters private dashboard`() {
    composeTestRule.onNodeWithTag("welcome_private_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("welcome_private_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_dashboard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("import_excel_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("import_excel_button").performClick()
    composeTestRule.waitForIdle()

    val nextIntent = Shadows.shadowOf(composeTestRule.activity).nextStartedActivity
    assertNotNull(nextIntent)
    assertEquals(Intent.ACTION_OPEN_DOCUMENT, nextIntent.action)
    val mimeTypes = nextIntent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toList().orEmpty()
    assertTrue(mimeTypes.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    assertTrue(mimeTypes.contains("text/csv"))
  }
}
