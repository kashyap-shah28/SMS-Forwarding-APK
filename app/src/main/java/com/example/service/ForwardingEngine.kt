package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.EmailSettings
import com.example.data.model.FilterAction
import com.example.data.model.FilterRule
import com.example.data.model.FilterType
import com.example.data.model.ForwardLog
import com.example.data.model.LogStatus
import com.example.network.SmtpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ForwardingEngine(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val preferencesManager: PreferencesManager = PreferencesManager(context),
    private val smtpClient: SmtpClient = SmtpClient()
) {
    companion object {
        private const val TAG = "ForwardingEngine"
    }

    suspend fun processIncomingSms(
        sender: String,
        messageBody: String,
        timestamp: Long = System.currentTimeMillis()
    ): ForwardLog = withContext(Dispatchers.IO) {
        val settings = preferencesManager.getEmailSettings()

        if (!settings.isForwarderEnabled) {
            val log = ForwardLog(
                sender = sender,
                messageBody = messageBody,
                receivedAt = timestamp,
                status = LogStatus.FILTERED_OUT,
                statusMessage = "SMS Forwarder is paused/disabled in settings.",
                matchedRuleName = null,
                targetEmail = null
            )
            val logId = database.forwardLogDao().insertLog(log)
            return@withContext log.copy(id = logId)
        }

        val activeRules = database.filterRuleDao().getActiveRulesList()
        val matchResult = evaluateRules(activeRules, sender, messageBody, settings)

        when (matchResult) {
            is MatchDecision.Excluded -> {
                val log = ForwardLog(
                    sender = sender,
                    messageBody = messageBody,
                    receivedAt = timestamp,
                    status = LogStatus.FILTERED_OUT,
                    statusMessage = "Blocked by rule: ${matchResult.rule.name}",
                    matchedRuleName = matchResult.rule.name,
                    targetEmail = null
                )
                val logId = database.forwardLogDao().insertLog(log)
                return@withContext log.copy(id = logId)
            }
            is MatchDecision.Forward -> {
                val recipient = matchResult.targetEmail
                val subject = "SMS from $sender - [SMS Forwarder]"
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
                val emailContent = buildString {
                    appendLine("═══════════════════════════════════════")
                    appendLine("SMS FORWARDER - NEW MESSAGE RECEIVED")
                    appendLine("═══════════════════════════════════════")
                    appendLine("From: $sender")
                    appendLine("Date: $formattedDate")
                    appendLine("Matched Rule: ${matchResult.ruleName}")
                    appendLine("Target: $recipient")
                    appendLine("───────────────────────────────────────")
                    appendLine("MESSAGE BODY:")
                    appendLine(messageBody)
                    appendLine("═══════════════════════════════════════")
                    appendLine("Forwarded securely by Android SMS Forwarder.")
                }

                val sendResult = smtpClient.sendEmail(
                    settings = settings,
                    toEmail = recipient,
                    subject = subject,
                    bodyText = emailContent
                )

                val log = if (sendResult.isSuccess) {
                    ForwardLog(
                        sender = sender,
                        messageBody = messageBody,
                        receivedAt = timestamp,
                        status = LogStatus.SUCCESS,
                        statusMessage = sendResult.getOrNull() ?: "Delivered to $recipient",
                        matchedRuleName = matchResult.ruleName,
                        targetEmail = recipient
                    )
                } else {
                    ForwardLog(
                        sender = sender,
                        messageBody = messageBody,
                        receivedAt = timestamp,
                        status = LogStatus.FAILED,
                        statusMessage = "Delivery error: ${sendResult.exceptionOrNull()?.message}",
                        matchedRuleName = matchResult.ruleName,
                        targetEmail = recipient
                    )
                }

                val logId = database.forwardLogDao().insertLog(log)

                // Update notification/broadcast if service is active
                SmsForwarderService.updateStatus(context, "Last SMS from $sender forwarded")

                return@withContext log.copy(id = logId)
            }
            is MatchDecision.NoMatch -> {
                val log = ForwardLog(
                    sender = sender,
                    messageBody = messageBody,
                    receivedAt = timestamp,
                    status = LogStatus.FILTERED_OUT,
                    statusMessage = "Ignored: No filter rules matched and 'Forward All' is disabled.",
                    matchedRuleName = null,
                    targetEmail = null
                )
                val logId = database.forwardLogDao().insertLog(log)
                return@withContext log.copy(id = logId)
            }
        }
    }

    private fun evaluateRules(
        rules: List<FilterRule>,
        sender: String,
        body: String,
        settings: EmailSettings
    ): MatchDecision {
        for (rule in rules) {
            val matches = isRuleMatched(rule, sender, body)
            if (matches) {
                return if (rule.action == FilterAction.EXCLUDE) {
                    MatchDecision.Excluded(rule)
                } else {
                    val target = rule.targetEmailOverride?.takeIf { it.isNotBlank() }
                        ?: settings.defaultRecipient
                    MatchDecision.Forward(rule.name, target)
                }
            }
        }

        return if (settings.forwardAllByDefault) {
            MatchDecision.Forward("Default (Forward All)", settings.defaultRecipient)
        } else {
            MatchDecision.NoMatch
        }
    }

    private fun isRuleMatched(rule: FilterRule, sender: String, body: String): Boolean {
        return when (rule.filterType) {
            FilterType.ALL -> true
            FilterType.KEYWORD -> {
                // Supports comma-separated list of keywords: e.g. "OTP, Code, 2FA"
                val tokens = rule.filterValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (tokens.isEmpty()) return false

                tokens.any { token ->
                    if (rule.caseSensitive) {
                        body.contains(token)
                    } else {
                        body.contains(token, ignoreCase = true)
                    }
                }
            }
            FilterType.SENDER -> {
                val cleanFilter = rule.filterValue.replace(" ", "").trim()
                val cleanSender = sender.replace(" ", "").trim()
                if (cleanFilter.isBlank()) return false

                val tokens = cleanFilter.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                tokens.any { token ->
                    cleanSender.contains(token, ignoreCase = !rule.caseSensitive) ||
                            cleanSender.endsWith(token) ||
                            cleanSender.equals(token, ignoreCase = true)
                }
            }
            FilterType.REGEX -> {
                try {
                    val options = if (rule.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                    val regex = Regex(rule.filterValue.trim(), options)
                    regex.containsMatchIn(body) || regex.containsMatchIn(sender)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid regex '${rule.filterValue}': ${e.message}")
                    false
                }
            }
        }
    }

    private sealed interface MatchDecision {
        data class Forward(val ruleName: String, val targetEmail: String) : MatchDecision
        data class Excluded(val rule: FilterRule) : MatchDecision
        data object NoMatch : MatchDecision
    }
}
