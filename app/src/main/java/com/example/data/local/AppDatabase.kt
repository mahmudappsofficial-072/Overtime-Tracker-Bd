package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.MonthlySalarySettings
import com.example.data.model.OvertimeRecord
import com.example.data.model.PieceRateRecord
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [OvertimeRecord::class, PieceRateRecord::class, UserProfile::class, MonthlySalarySettings::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun overtimeDao(): OvertimeDao
    abstract fun pieceRateDao(): PieceRateDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun monthlySalaryDao(): MonthlySalaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN maxAbsentForBonus INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS monthly_salary_settings (
                        yearMonth TEXT NOT NULL PRIMARY KEY,
                        basicSalary REAL NOT NULL DEFAULT 0.0,
                        houseRent REAL NOT NULL DEFAULT 0.0,
                        medicalAllowance REAL NOT NULL DEFAULT 750.0,
                        conveyanceAllowance REAL NOT NULL DEFAULT 0.0,
                        foodAllowance REAL NOT NULL DEFAULT 0.0,
                        grossSalary REAL NOT NULL DEFAULT 0.0,
                        hourlyOtRate REAL NOT NULL DEFAULT 0.0,
                        attendanceBonus REAL NOT NULL DEFAULT 0.0,
                        maxAbsentForBonus INTEGER NOT NULL DEFAULT 1,
                        nightBillRate REAL NOT NULL DEFAULT 0.0,
                        tiffinBillRate REAL NOT NULL DEFAULT 0.0,
                        isCustomSalary INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN hasAcceptedPrivacyPolicy INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN privacyPolicyAcceptedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "overtime_bd_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate initial user profile
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.userProfileDao()?.insertOrUpdateProfile(UserProfile())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
