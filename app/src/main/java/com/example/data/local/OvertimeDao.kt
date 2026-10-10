package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OvertimeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface OvertimeDao {
    @Query("SELECT * FROM overtime_records ORDER BY date DESC, id DESC")
    fun getAllRecords(): Flow<List<OvertimeRecord>>

    @Query("SELECT * FROM overtime_records WHERE yearMonth = :yearMonth ORDER BY date DESC, id DESC")
    fun getRecordsByMonth(yearMonth: String): Flow<List<OvertimeRecord>>

    @Query("SELECT * FROM overtime_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): OvertimeRecord?

    @Query("SELECT * FROM overtime_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): OvertimeRecord?

    @Query("SELECT * FROM overtime_records ORDER BY date ASC")
    suspend fun getAllRecordsList(): List<OvertimeRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: OvertimeRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<OvertimeRecord>)

    @Update
    suspend fun updateRecord(record: OvertimeRecord)

    @Delete
    suspend fun deleteRecord(record: OvertimeRecord)

    @Query("DELETE FROM overtime_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM overtime_records WHERE id NOT IN (SELECT MAX(id) FROM overtime_records GROUP BY date)")
    suspend fun cleanupDuplicateDates()

    @Query("SELECT SUM(otHours) FROM overtime_records WHERE yearMonth = :yearMonth")
    fun getTotalOtHoursByMonth(yearMonth: String): Flow<Double?>

    @Query("DELETE FROM overtime_records")
    suspend fun deleteAllRecords()
}
