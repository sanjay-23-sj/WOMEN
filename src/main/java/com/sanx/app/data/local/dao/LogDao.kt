package com.sanx.app.data.local.dao

import androidx.room.*
import com.sanx.app.data.local.entity.EmergencyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: EmergencyLog): Long

    @Query("SELECT * FROM emergency_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsForSession(sessionId: String): Flow<List<EmergencyLog>>

    @Query("SELECT * FROM emergency_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<EmergencyLog>>

    @Query("DELETE FROM emergency_logs WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    @Query("DELETE FROM emergency_logs WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
