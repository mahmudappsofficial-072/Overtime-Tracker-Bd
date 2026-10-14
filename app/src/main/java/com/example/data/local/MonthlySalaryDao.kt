package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MonthlySalarySettings
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySalaryDao {
    @Query("SELECT * FROM monthly_salary_settings WHERE yearMonth = :yearMonth LIMIT 1")
    fun getSettingsForMonth(yearMonth: String): Flow<MonthlySalarySettings?>

    @Query("SELECT * FROM monthly_salary_settings WHERE yearMonth = :yearMonth LIMIT 1")
    suspend fun getSettingsForMonthOnce(yearMonth: String): MonthlySalarySettings?

    @Query("SELECT * FROM monthly_salary_settings ORDER BY yearMonth DESC")
    suspend fun getAllSettings(): List<MonthlySalarySettings>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: MonthlySalarySettings)

    @Query("DELETE FROM monthly_salary_settings")
    suspend fun deleteAll()
}
