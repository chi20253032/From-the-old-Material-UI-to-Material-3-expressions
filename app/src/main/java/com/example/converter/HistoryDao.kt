package com.example.converter

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM conversion_history ORDER BY conversionTime DESC")
    fun getAllConversions(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(history: HistoryEntity)

    @Delete
    suspend fun deleteConversion(history: HistoryEntity)

    @Query("DELETE FROM conversion_history")
    suspend fun clearHistory()
}
