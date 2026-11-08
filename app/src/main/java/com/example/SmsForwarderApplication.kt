package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.repository.ForwarderRepository
import com.example.service.SmsForwarderService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsForwarderApplication : Application() {

    lateinit var repository: ForwarderRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        val preferencesManager = PreferencesManager(this)
        repository = ForwarderRepository(
            ruleDao = database.filterRuleDao(),
            logDao = database.forwardLogDao(),
            preferencesManager = preferencesManager
        )

        createNotificationChannels()

        applicationScope.launch {
            repository.ensureDefaultRules()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val forwarderChannel = NotificationChannel(
                SmsForwarderService.CHANNEL_ID,
                getString(R.string.channel_name_forwarder),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc_forwarder)
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                "sms_forwarder_alerts",
                getString(R.string.channel_name_alerts),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_desc_alerts)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(forwarderChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }
}
