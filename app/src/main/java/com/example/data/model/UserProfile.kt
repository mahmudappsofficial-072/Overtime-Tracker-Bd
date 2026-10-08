package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val userName: String = "",
    val companyName: String = "",
    val designation: String = "",
    val workerId: String = "",
    val grade: String = "",
    val isCustomSalary: Boolean = false,
    val basicSalary: Double = 0.0,
    val houseRent: Double = 0.0,
    val medicalAllowance: Double = 750.0, // Fixed 750 as per BD RMG standard
    val conveyanceAllowance: Double = 0.0,
    val foodAllowance: Double = 0.0,
    val attendanceBonus: Double = 0.0,
    val nightBillRate: Double = 0.0,
    val tiffinBillRate: Double = 0.0,
    val customOtRate: Double = 0.0,
    val monthlyOtTargetHours: Double = 0.0,
    val language: String = "bn",
    val supportWhatsapp: String = "",
    val lastCloudSyncTime: Long = 0L,
    val cloudSyncStatus: String = "অফলাইন (লোকাল)"
)
