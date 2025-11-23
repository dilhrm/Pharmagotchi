package com.example.pharmagotchi.utils

import android.content.Context
import com.example.pharmagotchi.SecurePreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailService(context: Context) {

    private val securePrefs = SecurePreferencesManager(context)

    suspend fun sendEmail(toEmail: String, subject: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
        val (fromEmail, password) = securePrefs.getEmailCredentials()

        if (fromEmail.isNullOrEmpty() || password.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("Email credentials not configured"))
        }

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        try {
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(fromEmail, password)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                setSubject(subject)
                setText(body)
            }

            Transport.send(message)
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }
}
