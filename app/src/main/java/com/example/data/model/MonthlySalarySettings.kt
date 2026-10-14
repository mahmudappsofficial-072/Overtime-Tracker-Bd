package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_salary_settings")
data class MonthlySalarySettings(
    @PrimaryKey
    val yearMonth: String, // e.g. "2026-10"
    val basicSalary: Double = 0.0,
    val houseRent: Double = 0.0,
    val medicalAllowance: Double = 750.0,
    val conveyanceAllowance: Double = 0.0,
    val foodAllowance: Double = 0.0,
    val grossSalary: Double = 0.0,
    val hourlyOtRate: Double = 0.0,
    val attendanceBonus: Double = 0.0,
    val maxAbsentForBonus: Int = 1,
    val nightBillRate: Double = 0.0,
    val tiffinBillRate: Double = 0.0,
    val isCustomSalary: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
