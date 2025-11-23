package com.example.pharmagotchi.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pharmagotchi.R
import com.example.pharmagotchi.database.PharmagotchiDatabase
import java.util.concurrent.TimeUnit

import com.example.pharmagotchi.utils.PetStatusManager
import com.example.pharmagotchi.models.PetEmotion

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = PharmagotchiDatabase.getDatabase(applicationContext)
        val medications = database.medicationDao().getAllMedicationsSync()

        val currentTime = System.currentTimeMillis()
        var anyMissed = false

        medications.forEach { medication ->
            val intervalMillis = TimeUnit.HOURS.toMillis(medication.intervalHours.toLong())
            val timeSinceLastTaken = currentTime - medication.lastTaken

            if (medication.lastTaken > 0 && timeSinceLastTaken >= intervalMillis) {
                sendNotification(medication.name)
                anyMissed = true
            }
        }
        
        // Check pet status and notify if in trouble
        val petStatusManager = PetStatusManager(applicationContext)
        val status = petStatusManager.determineStatus()
        
        if (status.emotion != PetEmotion.HAPPY) {
            sendPetStatusNotification(status.emotion, status.reason)
        }

        return Result.success()
    }

    private fun sendPetStatusNotification(emotion: PetEmotion, reason: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pet_status"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pet Status",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val title = when(emotion) {
            PetEmotion.SAD -> "Pharmagotchi is Sad"
            PetEmotion.CONFUSED -> "Pharmagotchi is Confused"
            PetEmotion.IN_PAIN -> "Pharmagotchi is in Pain"
            else -> "Pharmagotchi Status"
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(reason)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(999, notification)
    }

    private fun sendNotification(medicationName: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medication_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp) // Ensure this icon exists
            .setContentTitle("Medication Reminder")
            .setContentText("It's time to take your $medicationName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(medicationName.hashCode(), notification)
    }
}
