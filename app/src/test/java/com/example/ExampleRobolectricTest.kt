package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("Overtime Tracker BD", appName)
  }

  @Test
  fun `verify Bangladesh Labor Law double overtime rate calculation`() {
    val basicSalary = 10400.0
    val autoResult = com.example.util.LaborLawCalculator.calculateAutoAllowances(basicSalary)
    // Basic 10,400 / 104 = 100.0 per hour
    assertEquals(100.0, autoResult.otHourlyRate, 0.01)
    // House rent is 50%
    assertEquals(5200.0, autoResult.houseRent, 0.01)
  }
}
