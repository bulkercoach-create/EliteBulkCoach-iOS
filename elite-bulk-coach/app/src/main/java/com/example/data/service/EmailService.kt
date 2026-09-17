package com.example.data.service

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

enum class EmailPurpose {
    REGISTRATION,
    LOGIN_VERIFICATION,
    PASSWORD_RESET
}

object EmailService {
    private const val TAG = "EmailService"
    private const val SENDER_EMAIL = "bulkercoach@gmail.com"
    private const val SENDER_PASSWORD = "ctgzlrthufkkhtjj"
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = 587

    suspend fun sendPasswordResetEmail(
        recipientEmail: String,
        code: String,
        userName: String = "Athlet"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        sendEmailWithPurpose(recipientEmail, code, EmailPurpose.PASSWORD_RESET, userName)
    }

    suspend fun sendVerificationCode(
        recipientEmail: String,
        code: String,
        isRegistration: Boolean = false,
        isPasswordReset: Boolean = false,
        userName: String = "Athlet"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val purpose = when {
            isPasswordReset -> EmailPurpose.PASSWORD_RESET
            isRegistration -> EmailPurpose.REGISTRATION
            else -> EmailPurpose.LOGIN_VERIFICATION
        }
        sendEmailWithPurpose(recipientEmail, code, purpose, userName)
    }

    suspend fun sendEmailWithPurpose(
        recipientEmail: String,
        code: String,
        purpose: EmailPurpose,
        userName: String = "Athlet"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanRecipient = recipientEmail.trim().lowercase()
        Log.d(TAG, "Starting SMTP email dispatch to $cleanRecipient for purpose $purpose with code $code")

        // 1. Try STARTTLS on port 587 first (TLS as configured)
        val startTlsResult = trySendStartTls(cleanRecipient, code, purpose, userName)
        if (startTlsResult.isSuccess) {
            Log.d(TAG, "SMTP STARTTLS (Port 587) delivered successfully to $cleanRecipient")
            return@withContext startTlsResult
        }

        Log.w(TAG, "STARTTLS port 587 failed, trying SSL on port 465 fallback: ${startTlsResult.exceptionOrNull()?.message}")
        // 2. Fallback to SSL on port 465
        val sslResult = trySendSsl(cleanRecipient, code, purpose, userName)
        if (sslResult.isSuccess) {
            Log.d(TAG, "SMTP SSL (Port 465) delivered successfully to $cleanRecipient")
            return@withContext sslResult
        }

        Log.w(TAG, "SMTP direct socket connection not available: ${sslResult.exceptionOrNull()?.message}")
        Result.failure(startTlsResult.exceptionOrNull() ?: Exception("SMTP connection failed"))
    }

    private fun trySendStartTls(
        cleanRecipient: String,
        code: String,
        purpose: EmailPurpose,
        userName: String
    ): Result<Boolean> {
        return try {
            val addresses = InetAddress.getAllByName(SMTP_HOST)
            val targetAddress = addresses.firstOrNull { it is Inet4Address } ?: addresses.first()

            val socket = Socket()
            socket.connect(InetSocketAddress(targetAddress, SMTP_PORT), 7000)
            socket.soTimeout = 10000

            val reader = BufferedReader(InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8))

            fun readResponse(): String {
                val sb = StringBuilder()
                var line: String?
                while (true) {
                    line = reader.readLine() ?: break
                    sb.append(line).append("\n")
                    if (line.length >= 4 && line[3] == ' ') break
                    if (line.length < 4) break
                }
                return sb.toString().trim()
            }

            fun sendCommand(cmd: String): String {
                writer.write(cmd + "\r\n")
                writer.flush()
                return readResponse()
            }

            readResponse() // Greeting
            sendCommand("EHLO localhost")
            val startTlsResp = sendCommand("STARTTLS")
            if (!startTlsResp.startsWith("220")) {
                socket.close()
                return Result.failure(Exception("STARTTLS refused: $startTlsResp"))
            }

            val sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = sslSocketFactory.createSocket(socket, SMTP_HOST, SMTP_PORT, true) as SSLSocket
            sslSocket.startHandshake()

            executeSmtpConversation(sslSocket, cleanRecipient, code, purpose, userName, readInitialGreeting = false)
        } catch (e: Exception) {
            Log.w(TAG, "STARTTLS 587 attempt failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun trySendSsl(
        cleanRecipient: String,
        code: String,
        purpose: EmailPurpose,
        userName: String
    ): Result<Boolean> {
        return try {
            val addresses = InetAddress.getAllByName(SMTP_HOST)
            val targetAddress = addresses.firstOrNull { it is Inet4Address } ?: addresses.first()

            val rawSocket = Socket()
            rawSocket.connect(InetSocketAddress(targetAddress, 465), 7000)
            rawSocket.soTimeout = 10000

            val sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = sslSocketFactory.createSocket(
                rawSocket,
                SMTP_HOST,
                465,
                true
            ) as SSLSocket
            sslSocket.startHandshake()

            executeSmtpConversation(sslSocket, cleanRecipient, code, purpose, userName, readInitialGreeting = true)
        } catch (e: Exception) {
            Log.w(TAG, "SSL 465 attempt failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun executeSmtpConversation(
        socket: SSLSocket,
        cleanRecipient: String,
        code: String,
        purpose: EmailPurpose,
        userName: String,
        readInitialGreeting: Boolean
    ): Result<Boolean> {
        val reader = BufferedReader(InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
        val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8))

        fun readResponse(): String {
            val sb = StringBuilder()
            var line: String?
            while (true) {
                line = reader.readLine() ?: break
                sb.append(line).append("\n")
                if (line.length >= 4 && line[3] == ' ') break
                if (line.length < 4) break
            }
            return sb.toString().trim()
        }

        fun sendCommand(cmd: String): String {
            writer.write(cmd + "\r\n")
            writer.flush()
            return readResponse()
        }

        try {
            if (readInitialGreeting) {
                readResponse()
            }
            sendCommand("EHLO axon.app")

            val authPrompt = sendCommand("AUTH LOGIN")
            if (!authPrompt.startsWith("334")) {
                socket.close()
                return Result.failure(Exception("AUTH LOGIN nicht akzeptiert: $authPrompt"))
            }

            val userBase64 = Base64.encodeToString(SENDER_EMAIL.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            val userResp = sendCommand(userBase64)
            if (!userResp.startsWith("334")) {
                socket.close()
                return Result.failure(Exception("Username rejected: $userResp"))
            }

            val passBase64 = Base64.encodeToString(SENDER_PASSWORD.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            val passResp = sendCommand(passBase64)
            if (!passResp.startsWith("235")) {
                socket.close()
                return Result.failure(Exception("Password auth failed: $passResp"))
            }

            sendCommand("MAIL FROM:<$SENDER_EMAIL>")
            sendCommand("RCPT TO:<$cleanRecipient>")
            sendCommand("DATA")

            val subject = "Dein AXON Sicherheitscode: $code"

            val body = """
                Hallo $userName,

                Dein 6-stelliger Sicherheitscode lautet:

                ==============================
                           $code
                ==============================

                Gib diesen Code in der App ein, um fortzufahren.
                Gültig für 15 Minuten.

                Dein AXON Team
            """.trimIndent()

            val messagePayload = buildString {
                append("From: \"AXON\" <$SENDER_EMAIL>\r\n")
                append("To: <$cleanRecipient>\r\n")
                append("Subject: $subject\r\n")
                append("MIME-Version: 1.0\r\n")
                append("Content-Type: text/plain; charset=UTF-8\r\n")
                append("Content-Transfer-Encoding: 8bit\r\n\r\n")
                append(body)
                append("\r\n.")
            }

            val finalResp = sendCommand(messagePayload)
            sendCommand("QUIT")
            socket.close()

            return if (finalResp.startsWith("250")) {
                Log.d(TAG, "E-Mail erfolgreich versendet an $cleanRecipient")
                Result.success(true)
            } else {
                Result.failure(Exception("Fehler beim Senden: $finalResp"))
            }
        } catch (e: Exception) {
            socket.close()
            return Result.failure(e)
        }
    }
}
