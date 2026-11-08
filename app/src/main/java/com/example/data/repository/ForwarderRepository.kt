package com.example.data.repository

import com.example.data.local.FilterRuleDao
import com.example.data.local.ForwardLogDao
import com.example.data.local.PreferencesManager
import com.example.data.model.EmailSettings
import com.example.data.model.FilterAction
import com.example.data.model.FilterRule
import com.example.data.model.FilterType
import com.example.data.model.ForwardLog
import com.example.data.model.LogStatus
import kotlinx.coroutines.flow.Flow

class ForwarderRepository(
    private val ruleDao: FilterRuleDao,
    private val logDao: ForwardLogDao,
    private val preferencesManager: PreferencesManager
) {
    val allRules: Flow<List<FilterRule>> = ruleDao.getAllRules()
    val allLogs: Flow<List<ForwardLog>> = logDao.getAllLogs()
    val recentLogs: Flow<List<ForwardLog>> = logDao.getRecentLogs(10)
    val totalCount: Flow<Int> = logDao.getTotalCount()
    val successCount: Flow<Int> = logDao.getSuccessCount()
    val failedCount: Flow<Int> = logDao.getFailedCount()
    val filteredCount: Flow<Int> = logDao.getFilteredCount()
    val emailSettings: Flow<EmailSettings> = preferencesManager.emailSettingsFlow

    fun getEmailSettingsSync(): EmailSettings = preferencesManager.getEmailSettings()

    fun saveEmailSettings(settings: EmailSettings) {
        preferencesManager.saveEmailSettings(settings)
    }

    fun setForwarderEnabled(enabled: Boolean) {
        preferencesManager.setForwarderEnabled(enabled)
    }

    suspend fun insertRule(rule: FilterRule): Long = ruleDao.insertRule(rule)

    suspend fun updateRule(rule: FilterRule) = ruleDao.updateRule(rule)

    suspend fun deleteRule(rule: FilterRule) = ruleDao.deleteRule(rule)

    suspend fun deleteRuleById(id: Long) = ruleDao.deleteRuleById(id)

    suspend fun getActiveRules(): List<FilterRule> = ruleDao.getActiveRulesList()

    suspend fun insertLog(log: ForwardLog): Long = logDao.insertLog(log)

    suspend fun updateLog(log: ForwardLog) = logDao.updateLog(log)

    suspend fun getLogById(id: Long): ForwardLog? = logDao.getLogById(id)

    suspend fun clearLogs() = logDao.clearAllLogs()

    suspend fun ensureDefaultRules() {
        if (ruleDao.getRuleCount() == 0) {
            val defaults = listOf(
                FilterRule(
                    name = "Bank & 2FA Codes",
                    isEnabled = true,
                    filterType = FilterType.KEYWORD,
                    filterValue = "code, OTP, verification, security code, password",
                    caseSensitive = false,
                    action = FilterAction.FORWARD
                ),
                FilterRule(
                    name = "Ignore Spam / Marketing",
                    isEnabled = false,
                    filterType = FilterType.KEYWORD,
                    filterValue = "SALE, discount, unsubscribe, promo",
                    caseSensitive = false,
                    action = FilterAction.EXCLUDE
                )
            )
            ruleDao.insertRules(defaults)
        }
    }
}
