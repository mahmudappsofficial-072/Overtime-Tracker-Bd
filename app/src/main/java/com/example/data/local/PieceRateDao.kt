package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PieceRateRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PieceRateDao {
    @Query("SELECT * FROM piece_rate_records ORDER BY date DESC, id DESC")
    fun getAllPieceRecords(): Flow<List<PieceRateRecord>>

    @Query("SELECT * FROM piece_rate_records WHERE yearMonth = :yearMonth ORDER BY date DESC, id DESC")
    fun getPieceRecordsByMonth(yearMonth: String): Flow<List<PieceRateRecord>>

    @Query("SELECT * FROM piece_rate_records WHERE id = :id LIMIT 1")
    suspend fun getPieceRecordById(id: Long): PieceRateRecord?

    @Query("SELECT * FROM piece_rate_records ORDER BY date ASC")
    suspend fun getAllPieceRecordsList(): List<PieceRateRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPieceRecord(record: PieceRateRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPieceRecords(records: List<PieceRateRecord>)

    @Update
    suspend fun updatePieceRecord(record: PieceRateRecord)

    @Delete
    suspend fun deletePieceRecord(record: PieceRateRecord)

    @Query("DELETE FROM piece_rate_records WHERE id = :id")
    suspend fun deletePieceRecordById(id: Long)

    @Query("SELECT SUM(totalAmount) FROM piece_rate_records WHERE yearMonth = :yearMonth")
    fun getTotalPieceAmountByMonth(yearMonth: String): Flow<Double?>
}
