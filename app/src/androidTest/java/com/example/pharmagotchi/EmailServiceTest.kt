package com.example.pharmagotchi

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pharmagotchi.utils.EmailService
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class EmailServiceTest {
    @Test
    fun testSendEmail() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val emailService = EmailService(appContext)
        
        // Use the configured email as recipient for testing
        val (fromEmail, _) = com.example.pharmagotchi.SecurePreferencesManager(appContext).getEmailCredentials()
        assertNotNull("Email credentials should be configured", fromEmail)
        
        val result = emailService.sendEmail(
            toEmail = fromEmail!!, 
            subject = "Test Email from Pharmagotchi Debugger", 
            body = "This is a test email to verify credentials and network."
        )
        
        assertTrue("Email sending failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
    }
}
