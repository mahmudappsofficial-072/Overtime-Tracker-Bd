package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMissingOtDetector_findsYesterdayWhenMissing() {
    val cal = java.util.Calendar.getInstance()
    cal.set(2026, java.util.Calendar.OCTOBER, 19) // Oct 19, 2026

    val existing = listOf(
      com.example.data.model.OvertimeRecord(date = "2026-10-17", yearMonth = "2026-10", otHours = 2.0)
    )

    val missing = com.example.ui.screens.MissingOtDetector.getMissingDatesForCurrentMonth(
      existingRecords = existing,
      referenceDate = cal.time
    )

    // Yesterday (2026-10-18) should be the first missing date
    assertTrue(missing.contains("2026-10-18"))
    assertEquals("2026-10-18", missing[0])
    // 2026-10-17 is already entered, so it must not be in missing
    assertFalse(missing.contains("2026-10-17"))
  }

  @Test
  fun testMissingOtDetector_returnsEmptyOnFirstDayOfMonth() {
    val cal = java.util.Calendar.getInstance()
    cal.set(2026, java.util.Calendar.NOVEMBER, 1) // Nov 1, 2026

    val missing = com.example.ui.screens.MissingOtDetector.getMissingDatesForCurrentMonth(
      existingRecords = emptyList(),
      referenceDate = cal.time
    )

    assertTrue(missing.isEmpty())
  }
}

