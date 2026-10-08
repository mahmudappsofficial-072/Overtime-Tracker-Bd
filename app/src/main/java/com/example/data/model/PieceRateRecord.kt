package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "piece_rate_records")
data class PieceRateRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,            // "YYYY-MM-DD"
    val yearMonth: String,       // "YYYY-MM"
    val itemName: String,        // e.g. "Pocket Join", "Collar Attach", "Button Stitch"
    val pieceCount: Int,         // e.g. 150
    val ratePerPiece: Double,    // e.g. 2.5
    val totalAmount: Double,     // pieceCount * ratePerPiece
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
