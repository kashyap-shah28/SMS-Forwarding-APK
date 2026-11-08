package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FilterType {
    ALL,
    KEYWORD,
    SENDER,
    REGEX
}

enum class FilterAction {
    FORWARD,
    EXCLUDE
}

enum class LogStatus {
    SUCCESS,
    FAILED,
    FILTERED_OUT,
    PENDING
}

@Entity(tableName = "filter_rules")
data class FilterRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val filterType: FilterType = FilterType.KEYWORD,
    val filterValue: String = "",
    val caseSensitive: Boolean = false,
    val action: FilterAction = FilterAction.FORWARD,
    val targetEmailOverride: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "forward_logs")
data class ForwardLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val messageBody: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val status: LogStatus = LogStatus.PENDING,
    val statusMessage: String = "",
    val matchedRuleName: String? = null,
    val targetEmail: String? = null
)

data class EmailSettings(
    val smtpHost: String = "smtp.gmail.com",
    val smtpPort: Int = 587,
    val smtpUser: String = "",
    val smtpPassword: String = "",
    val senderDisplayName: String = "SMS Forwarder",
    val defaultRecipient: String = "kashyapjshah28@gmail.com",
    val encryptionType: String = "STARTTLS", // "STARTTLS", "SSL", "NONE"
    val isForwarderEnabled: Boolean = true,
    val forwardAllByDefault: Boolean = true,
    val notifyOnForward: Boolean = true
)
