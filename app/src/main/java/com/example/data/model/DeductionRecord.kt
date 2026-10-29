package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deduction_records")
data class DeductionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "ADVANCE" or "APON_BAZAR"
    val date: String, // YYYY-MM-DD
    val yearMonth: String, // YYYY-MM
    val amount: Double,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
