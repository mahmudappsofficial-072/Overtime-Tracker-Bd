package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TrashItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun getAllTrashItems(): Flow<List<TrashItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashItem): Long

    @Delete
    suspend fun deleteTrashItem(item: TrashItem)

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteTrashItemById(id: Long)

    @Query("DELETE FROM trash_items")
    suspend fun clearAllTrash()

    @Query("DELETE FROM trash_items WHERE deletedAt < :cutoffTimestamp")
    suspend fun purgeExpiredTrash(cutoffTimestamp: Long): Int

    @Query("SELECT MIN(deletedAt) FROM trash_items")
    suspend fun getEarliestDeletedTimestamp(): Long?
}
