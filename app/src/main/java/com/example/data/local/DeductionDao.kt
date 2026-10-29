package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DeductionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DeductionDao {
    @Query("SELECT * FROM deduction_records WHERE yearMonth = :yearMonth ORDER BY date DESC, id DESC")
    fun getDeductionsForMonth(yearMonth: String): Flow<List<DeductionRecord>>

    @Query("SELECT * FROM deduction_records WHERE yearMonth = :yearMonth ORDER BY date DESC, id DESC")
    suspend fun getDeductionsForMonthOnce(yearMonth: String): List<DeductionRecord>

    @Query("SELECT * FROM deduction_records ORDER BY date DESC")
    suspend fun getAllDeductions(): List<DeductionRecord>

    @Query("SELECT * FROM deduction_records WHERE id = :id LIMIT 1")
    suspend fun getDeductionById(id: Long): DeductionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DeductionRecord): Long

    @Query("DELETE FROM deduction_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM deduction_records")
    suspend fun deleteAll()
}
