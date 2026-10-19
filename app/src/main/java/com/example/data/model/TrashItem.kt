package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject

@Entity(tableName = "trash_items")
data class TrashItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemType: String, // "OT" or "PIECE"
    val originalId: Long,
    val title: String,
    val subtitle: String,
    val date: String,
    val jsonData: String,
    val deletedAt: Long = System.currentTimeMillis()
) {
    fun toOvertimeRecord(): OvertimeRecord? {
        if (itemType != "OT") return null
        return try {
            val json = JSONObject(jsonData)
            OvertimeRecord(
                id = json.optLong("id", 0L),
                date = json.optString("date", ""),
                yearMonth = json.optString("yearMonth", ""),
                otHours = json.optDouble("otHours", 0.0),
                hasNightBill = json.optBoolean("hasNightBill", false),
                hasTiffinBill = json.optBoolean("hasTiffinBill", false),
                isHoliday = json.optBoolean("isHoliday", false),
                shiftType = json.optString("shiftType", "REGULAR"),
                note = json.optString("note", ""),
                hourlyRateSnapshot = json.optDouble("hourlyRateSnapshot", 0.0),
                calculatedAmount = json.optDouble("calculatedAmount", 0.0),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }

    fun toPieceRateRecord(): PieceRateRecord? {
        if (itemType != "PIECE") return null
        return try {
            val json = JSONObject(jsonData)
            PieceRateRecord(
                id = json.optLong("id", 0L),
                date = json.optString("date", ""),
                yearMonth = json.optString("yearMonth", ""),
                itemName = json.optString("itemName", ""),
                pieceCount = json.optInt("pieceCount", 0),
                ratePerPiece = json.optDouble("ratePerPiece", 0.0),
                totalAmount = json.optDouble("totalAmount", 0.0),
                note = json.optString("note", ""),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun fromOvertimeRecord(record: OvertimeRecord): TrashItem {
            val json = JSONObject().apply {
                put("id", record.id)
                put("date", record.date)
                put("yearMonth", record.yearMonth)
                put("otHours", record.otHours)
                put("hasNightBill", record.hasNightBill)
                put("hasTiffinBill", record.hasTiffinBill)
                put("isHoliday", record.isHoliday)
                put("shiftType", record.shiftType)
                put("note", record.note)
                put("hourlyRateSnapshot", record.hourlyRateSnapshot)
                put("calculatedAmount", record.calculatedAmount)
                put("createdAt", record.createdAt)
            }
            val title = "ওভারটাইম: ${record.date}"
            val subtitle = "${record.otHours} ঘণ্টা • ৳${String.format("%.2f", record.calculatedAmount)}"
            return TrashItem(
                itemType = "OT",
                originalId = record.id,
                title = title,
                subtitle = subtitle,
                date = record.date,
                jsonData = json.toString(),
                deletedAt = System.currentTimeMillis()
            )
        }

        fun fromPieceRateRecord(record: PieceRateRecord): TrashItem {
            val json = JSONObject().apply {
                put("id", record.id)
                put("date", record.date)
                put("yearMonth", record.yearMonth)
                put("itemName", record.itemName)
                put("pieceCount", record.pieceCount)
                put("ratePerPiece", record.ratePerPiece)
                put("totalAmount", record.totalAmount)
                put("note", record.note)
                put("createdAt", record.createdAt)
            }
            val title = "পিস রেট: ${record.itemName}"
            val subtitle = "${record.pieceCount} পিস • ৳${String.format("%.2f", record.totalAmount)}"
            return TrashItem(
                itemType = "PIECE",
                originalId = record.id,
                title = title,
                subtitle = subtitle,
                date = record.date,
                jsonData = json.toString(),
                deletedAt = System.currentTimeMillis()
            )
        }
    }
}
