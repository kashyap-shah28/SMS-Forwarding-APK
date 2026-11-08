package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.service.ForwardingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages: Array<SmsMessage>? = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "No SMS messages found in intent")
            return
        }

        val pendingResult = goAsync()

        scope.launch {
            try {
                // Group multi-part SMS messages by originating address
                val messagesBySender = mutableMapOf<String, StringBuilder>()
                var latestTimestamp = System.currentTimeMillis()

                for (sms in messages) {
                    val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: "Unknown"
                    val body = sms.displayMessageBody ?: sms.messageBody ?: ""
                    latestTimestamp = sms.timestampMillis

                    val builder = messagesBySender.getOrPut(sender) { StringBuilder() }
                    builder.append(body)
                }

                val forwardingEngine = ForwardingEngine(context)

                for ((sender, bodyBuilder) in messagesBySender) {
                    val fullMessage = bodyBuilder.toString()
                    Log.d(TAG, "Processing SMS from $sender: length=${fullMessage.length}")
                    forwardingEngine.processIncomingSms(
                        sender = sender,
                        messageBody = fullMessage,
                        timestamp = latestTimestamp
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
