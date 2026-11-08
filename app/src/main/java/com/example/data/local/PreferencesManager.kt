package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.EmailSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_USER = "smtp_user"
        private const val KEY_SMTP_PASSWORD = "smtp_password"
        private const val KEY_SENDER_NAME = "sender_name"
        private const val KEY_DEFAULT_RECIPIENT = "default_recipient"
        private const val KEY_ENCRYPTION_TYPE = "encryption_type"
        private const val KEY_IS_FORWARDER_ENABLED = "is_forwarder_enabled"
        private const val KEY_FORWARD_ALL_BY_DEFAULT = "forward_all_by_default"
        private const val KEY_NOTIFY_ON_FORWARD = "notify_on_forward"
    }

    fun getEmailSettings(): EmailSettings {
        return EmailSettings(
            smtpHost = prefs.getString(KEY_SMTP_HOST, "smtp.gmail.com") ?: "smtp.gmail.com",
            smtpPort = prefs.getInt(KEY_SMTP_PORT, 587),
            smtpUser = prefs.getString(KEY_SMTP_USER, "") ?: "",
            smtpPassword = prefs.getString(KEY_SMTP_PASSWORD, "") ?: "",
            senderDisplayName = prefs.getString(KEY_SENDER_NAME, "SMS Forwarder") ?: "SMS Forwarder",
            defaultRecipient = prefs.getString(KEY_DEFAULT_RECIPIENT, "kashyapjshah28@gmail.com") ?: "kashyapjshah28@gmail.com",
            encryptionType = prefs.getString(KEY_ENCRYPTION_TYPE, "STARTTLS") ?: "STARTTLS",
            isForwarderEnabled = prefs.getBoolean(KEY_IS_FORWARDER_ENABLED, true),
            forwardAllByDefault = prefs.getBoolean(KEY_FORWARD_ALL_BY_DEFAULT, true),
            notifyOnForward = prefs.getBoolean(KEY_NOTIFY_ON_FORWARD, true)
        )
    }

    val emailSettingsFlow: Flow<EmailSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getEmailSettings())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getEmailSettings())
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()

    fun saveEmailSettings(settings: EmailSettings) {
        prefs.edit()
            .putString(KEY_SMTP_HOST, settings.smtpHost)
            .putInt(KEY_SMTP_PORT, settings.smtpPort)
            .putString(KEY_SMTP_USER, settings.smtpUser)
            .putString(KEY_SMTP_PASSWORD, settings.smtpPassword)
            .putString(KEY_SENDER_NAME, settings.senderDisplayName)
            .putString(KEY_DEFAULT_RECIPIENT, settings.defaultRecipient)
            .putString(KEY_ENCRYPTION_TYPE, settings.encryptionType)
            .putBoolean(KEY_IS_FORWARDER_ENABLED, settings.isForwarderEnabled)
            .putBoolean(KEY_FORWARD_ALL_BY_DEFAULT, settings.forwardAllByDefault)
            .putBoolean(KEY_NOTIFY_ON_FORWARD, settings.notifyOnForward)
            .apply()
    }

    fun setForwarderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_FORWARDER_ENABLED, enabled).apply()
    }

    fun isForwarderEnabled(): Boolean {
        return prefs.getBoolean(KEY_IS_FORWARDER_ENABLED, true)
    }
}
