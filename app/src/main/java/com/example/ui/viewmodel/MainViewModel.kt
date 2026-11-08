package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmsForwarderApplication
import com.example.data.model.EmailSettings
import com.example.data.model.FilterRule
import com.example.data.model.ForwardLog
import com.example.network.SmtpClient
import com.example.service.ForwardingEngine
import com.example.service.SmsForwarderService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SmsForwarderApplication).repository
    private val forwardingEngine = ForwardingEngine(application)
    private val smtpClient = SmtpClient()

    val rules: StateFlow<List<FilterRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<ForwardLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<ForwardLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val successCount: StateFlow<Int> = repository.successCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedCount: StateFlow<Int> = repository.failedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val filteredCount: StateFlow<Int> = repository.filteredCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val emailSettings: StateFlow<EmailSettings> = repository.emailSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getEmailSettingsSync())

    private val _isTestingSmtp = MutableStateFlow(false)
    val isTestingSmtp: StateFlow<Boolean> = _isTestingSmtp.asStateFlow()

    private val _smtpTestResult = MutableStateFlow<String?>(null)
    val smtpTestResult: StateFlow<String?> = _smtpTestResult.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _simulationMessage = MutableStateFlow<String?>(null)
    val simulationMessage: StateFlow<String?> = _simulationMessage.asStateFlow()

    fun toggleForwarder(enabled: Boolean) {
        repository.setForwarderEnabled(enabled)
        val context = getApplication<Application>()
        if (enabled) {
            SmsForwarderService.startService(context)
        } else {
            SmsForwarderService.stopService(context)
        }
    }

    fun saveEmailSettings(settings: EmailSettings) {
        repository.saveEmailSettings(settings)
    }

    fun testSmtpConnection(settings: EmailSettings) {
        viewModelScope.launch {
            _isTestingSmtp.value = true
            _smtpTestResult.value = null
            val result = smtpClient.testConnection(settings)
            _isTestingSmtp.value = false
            _smtpTestResult.value = if (result.isSuccess) {
                result.getOrNull() ?: "Success"
            } else {
                "Failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearSmtpTestResult() {
        _smtpTestResult.value = null
    }

    fun addRule(rule: FilterRule) {
        viewModelScope.launch {
            repository.insertRule(rule)
        }
    }

    fun updateRule(rule: FilterRule) {
        viewModelScope.launch {
            repository.updateRule(rule)
        }
    }

    fun toggleRule(rule: FilterRule) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteRule(rule: FilterRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
        }
    }

    fun retryForward(log: ForwardLog) {
        viewModelScope.launch {
            forwardingEngine.processIncomingSms(
                sender = log.sender,
                messageBody = log.messageBody,
                timestamp = log.receivedAt
            )
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun simulateIncomingSms(sender: String, message: String) {
        viewModelScope.launch {
            _isSimulating.value = true
            _simulationMessage.value = null
            val resultLog = forwardingEngine.processIncomingSms(
                sender = sender.ifBlank { "+1 (555) 019-2834" },
                messageBody = message.ifBlank { "Your Bank verification code is 482910. Valid for 5 minutes." },
                timestamp = System.currentTimeMillis()
            )
            _isSimulating.value = false
            _simulationMessage.value = "Processed SMS from ${resultLog.sender}: Status [${resultLog.status}] - ${resultLog.statusMessage}"
        }
    }

    fun clearSimulationMessage() {
        _simulationMessage.value = null
    }
}
