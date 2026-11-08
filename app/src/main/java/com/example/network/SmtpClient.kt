package com.example.network

import android.util.Base64
import com.example.data.model.EmailSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class SmtpClient {

    suspend fun testConnection(settings: EmailSettings): Result<String> = withContext(Dispatchers.IO) {
        if (settings.smtpHost.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("SMTP Host cannot be empty"))
        }
        if (settings.smtpUser.isBlank() || settings.smtpPassword.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Please provide SMTP Username/Email and App Password")
            )
        }

        try {
            val session = openSmtpSession(settings)
            session.close()
            Result.success("Connected & Authenticated successfully with ${settings.smtpHost}!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEmail(
        settings: EmailSettings,
        toEmail: String,
        subject: String,
        bodyText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (toEmail.isBlank() || !toEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid recipient email: $toEmail"))
        }

        // If credentials are not yet configured, provide a clear simulated delivery result so user can test UI
        if (settings.smtpUser.isBlank() || settings.smtpPassword.isBlank()) {
            return@withContext Result.success(
                "SIMULATED: Ready for inbox delivery! Enter your SMTP App Password in Settings to send live emails to $toEmail."
            )
        }

        try {
            val session = openSmtpSession(settings)
            val fromEmail = if (settings.smtpUser.contains("@")) settings.smtpUser else "sms-forwarder@${settings.smtpHost}"
            val senderName = settings.senderDisplayName.ifBlank { "SMS Forwarder" }

            session.sendCommand("MAIL FROM:<$fromEmail>", 250)
            session.sendCommand("RCPT TO:<$toEmail>", 250)
            session.sendCommand("DATA", 354)

            val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
            val dateStr = dateFormat.format(Date())

            val headers = StringBuilder().apply {
                append("From: \"$senderName\" <$fromEmail>\r\n")
                append("To: <$toEmail>\r\n")
                append("Subject: $subject\r\n")
                append("Date: $dateStr\r\n")
                append("MIME-Version: 1.0\r\n")
                append("Content-Type: text/plain; charset=UTF-8\r\n")
                append("X-Mailer: Android SMS Forwarder\r\n")
                append("\r\n")
            }

            // Dot-stuffing for RFC 821/822
            val safeBody = bodyText.replace("\n.", "\n..")
            session.writer.write(headers.toString())
            session.writer.write(safeBody)
            session.writer.write("\r\n.\r\n")
            session.writer.flush()

            val dataResponse = session.readResponse()
            if (!dataResponse.startsWith("250")) {
                throw Exception("Failed to send DATA: $dataResponse")
            }

            session.close()
            Result.success("Delivered to $toEmail")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openSmtpSession(settings: EmailSettings): SmtpSession {
        val host = settings.smtpHost.trim()
        val port = settings.smtpPort
        val isExplicitSsl = settings.encryptionType.equals("SSL", ignoreCase = true) || port == 465
        val timeoutMs = 15000

        var socket: Socket = if (isExplicitSsl) {
            val sslSocket = SSLSocketFactory.getDefault().createSocket() as SSLSocket
            sslSocket.connect(InetSocketAddress(host, port), timeoutMs)
            sslSocket.soTimeout = timeoutMs
            sslSocket.startHandshake()
            sslSocket
        } else {
            val plainSocket = Socket()
            plainSocket.connect(InetSocketAddress(host, port), timeoutMs)
            plainSocket.soTimeout = timeoutMs
            plainSocket
        }

        var reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        var writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

        var session = SmtpSession(socket, reader, writer)

        // Read greeting (220)
        val greeting = session.readResponse()
        if (!greeting.startsWith("220")) {
            session.close()
            throw Exception("Invalid SMTP greeting from $host: $greeting")
        }

        // Send EHLO
        session.sendCommand("EHLO localhost", 250)

        // Upgrade if STARTTLS
        if (!isExplicitSsl && settings.encryptionType.equals("STARTTLS", ignoreCase = true)) {
            session.sendCommand("STARTTLS", 220)
            val sslSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                .createSocket(socket, host, port, true) as SSLSocket
            sslSocket.soTimeout = timeoutMs
            sslSocket.startHandshake()

            socket = sslSocket
            reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
            session = SmtpSession(socket, reader, writer)

            // Re-send EHLO after TLS handshake
            session.sendCommand("EHLO localhost", 250)
        }

        // Perform AUTH LOGIN if credentials provided
        if (settings.smtpUser.isNotBlank() && settings.smtpPassword.isNotBlank()) {
            session.sendCommand("AUTH LOGIN", 334)
            val userB64 = Base64.encodeToString(settings.smtpUser.trim().toByteArray(), Base64.NO_WRAP)
            session.sendCommand(userB64, 334)

            val passB64 = Base64.encodeToString(settings.smtpPassword.trim().toByteArray(), Base64.NO_WRAP)
            val authResp = session.sendCommandAndGetResponse(passB64)
            if (!authResp.startsWith("235")) {
                session.close()
                throw Exception("Authentication failed ($authResp). If using Gmail, please use a Google App Password.")
            }
        }

        return session
    }

    private class SmtpSession(
        val socket: Socket,
        val reader: BufferedReader,
        val writer: BufferedWriter
    ) {
        fun sendCommand(cmd: String, expectedCode: Int) {
            writer.write("$cmd\r\n")
            writer.flush()
            val resp = readResponse()
            val codePrefix = expectedCode.toString()
            if (!resp.startsWith(codePrefix)) {
                throw Exception("SMTP command '$cmd' failed: $resp")
            }
        }

        fun sendCommandAndGetResponse(cmd: String): String {
            writer.write("$cmd\r\n")
            writer.flush()
            return readResponse()
        }

        fun readResponse(): String {
            val firstLine = reader.readLine() ?: throw Exception("SMTP server closed connection unexpectedly")
            val fullResponse = StringBuilder(firstLine)
            // Multi-line SMTP response: "250-something\r\n250 OK"
            if (firstLine.length >= 4 && firstLine[3] == '-') {
                val code = firstLine.substring(0, 3)
                while (true) {
                    val nextLine = reader.readLine() ?: break
                    fullResponse.append("\n").append(nextLine)
                    if (nextLine.startsWith(code) && (nextLine.length == 3 || nextLine[3] == ' ')) {
                        break
                    }
                }
            }
            return fullResponse.toString()
        }

        fun close() {
            try {
                sendCommandAndGetResponse("QUIT")
            } catch (_: Exception) {}
            try { socket.close() } catch (_: Exception) {}
        }
    }
}
