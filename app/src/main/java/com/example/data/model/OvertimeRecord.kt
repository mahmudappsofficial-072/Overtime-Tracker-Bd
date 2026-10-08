package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "overtime_records")
data class OvertimeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,            // "YYYY-MM-DD" e.g. "2026-10-06"
    val yearMonth: String,       // "YYYY-MM" e.g. "2026-10"
    val otHours: Double,         // e.g. 2.0, 3.5
    val hasNightBill: Boolean = false,
    val hasTiffinBill: Boolean = false,
    val isHoliday: Boolean = false,
    val shiftType: String = "REGULAR", // REGULAR, DAY, NIGHT, HOLIDAY, LEAVE, ABSENT, WEEKLY_OFF
    val note: String = "",
    val hourlyRateSnapshot: Double = 0.0,
    val calculatedAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
