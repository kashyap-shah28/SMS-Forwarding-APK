package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FilterRule
import com.example.data.model.ForwardLog
import com.example.data.model.LogStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterRuleDao {
    @Query("SELECT * FROM filter_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<FilterRule>>

    @Query("SELECT * FROM filter_rules WHERE isEnabled = 1 ORDER BY id ASC")
    fun getActiveRules(): Flow<List<FilterRule>>

    @Query("SELECT * FROM filter_rules WHERE isEnabled = 1 ORDER BY id ASC")
    suspend fun getActiveRulesList(): List<FilterRule>

    @Query("SELECT COUNT(*) FROM filter_rules")
    suspend fun getRuleCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: FilterRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<FilterRule>)

    @Update
    suspend fun updateRule(rule: FilterRule)

    @Delete
    suspend fun deleteRule(rule: FilterRule)

    @Query("DELETE FROM filter_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}

@Dao
interface ForwardLogDao {
    @Query("SELECT * FROM forward_logs ORDER BY receivedAt DESC")
    fun getAllLogs(): Flow<List<ForwardLog>>

    @Query("SELECT * FROM forward_logs ORDER BY receivedAt DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 10): Flow<List<ForwardLog>>

    @Query("SELECT * FROM forward_logs WHERE status = :status ORDER BY receivedAt DESC")
    fun getLogsByStatus(status: LogStatus): Flow<List<ForwardLog>>

    @Query("SELECT COUNT(*) FROM forward_logs")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM forward_logs WHERE status = 'SUCCESS'")
    fun getSuccessCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM forward_logs WHERE status = 'FAILED'")
    fun getFailedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM forward_logs WHERE status = 'FILTERED_OUT'")
    fun getFilteredCount(): Flow<Int>

    @Query("SELECT * FROM forward_logs WHERE id = :id")
    suspend fun getLogById(id: Long): ForwardLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ForwardLog): Long

    @Update
    suspend fun updateLog(log: ForwardLog)

    @Query("DELETE FROM forward_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM forward_logs")
    suspend fun clearAllLogs()
}
