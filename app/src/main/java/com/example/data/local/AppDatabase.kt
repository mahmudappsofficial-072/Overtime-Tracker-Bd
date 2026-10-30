package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DeductionRecord
import com.example.data.model.MonthlySalarySettings
import com.example.data.model.OvertimeRecord
import com.example.data.model.PieceRateRecord
import com.example.data.model.TrashItem
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [OvertimeRecord::class, PieceRateRecord::class, UserProfile::class, MonthlySalarySettings::class, TrashItem::class, DeductionRecord::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun overtimeDao(): OvertimeDao
    abstract fun pieceRateDao(): PieceRateDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun monthlySalaryDao(): MonthlySalaryDao
    abstract fun trashDao(): TrashDao
    abstract fun deductionDao(): DeductionDao

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

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trash_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemType TEXT NOT NULL,
                        originalId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        subtitle TEXT NOT NULL,
                        date TEXT NOT NULL,
                        jsonData TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS deduction_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        date TEXT NOT NULL,
                        yearMonth TEXT NOT NULL,
                        amount REAL NOT NULL,
                        description TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN maxLeaveForBonus INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE monthly_salary_settings ADD COLUMN maxLeaveForBonus INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "overtime_bd_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration(true)
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
