package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionLogDao {
    @Query("SELECT * FROM connection_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<ConnectionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConnectionLogEntity)

    @Query("DELETE FROM connection_history")
    suspend fun clearHistory()
}
