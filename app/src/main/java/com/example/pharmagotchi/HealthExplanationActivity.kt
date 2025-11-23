package com.example.pharmagotchi

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pharmagotchi.api.OpenRouterService
import com.example.pharmagotchi.databinding.ActivityHealthExplanationBinding
import com.example.pharmagotchi.models.ChatMessage
import kotlinx.coroutines.launch

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.pharmagotchi.workers.MedicationReminderWorker
import java.util.concurrent.TimeUnit
import com.example.pharmagotchi.database.PharmagotchiDatabase
import com.example.pharmagotchi.models.Medication

class HealthExplanationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHealthExplanationBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var securePrefs: SecurePreferencesManager
    private lateinit var database: PharmagotchiDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthExplanationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        prefsManager = PreferencesManager(this)
        securePrefs = SecurePreferencesManager(this)
        database = PharmagotchiDatabase.getDatabase(this)

        binding.continueButton.setOnClickListener {
            finishExplanation()
        }

        loadExplanation()
    }

    private fun loadExplanation() {
        val apiKey = securePrefs.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            // No API key, skip explanation
            finishExplanation()
            return
        }

        val conditions = prefsManager.getMedicalConditions()
        val medications = prefsManager.getMedications()
        val vitalSigns = prefsManager.getVitalSigns()

        if (conditions.isEmpty() && medications.isEmpty()) {
            // Nothing to explain
            finishExplanation()
            return
        }

        // Check cache first
        val cachedExplanation = prefsManager.getCachedHealthExplanation()
        if (!cachedExplanation.isNullOrEmpty()) {
            binding.loadingIndicator.visibility = View.GONE
            binding.explanationText.visibility = View.VISIBLE
            binding.explanationText.text = cachedExplanation
            binding.continueButton.visibility = View.VISIBLE
            return
        }

        binding.loadingIndicator.visibility = View.VISIBLE
        binding.explanationText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val openRouterService = OpenRouterService(apiKey)

                val prompt = buildExplanationPrompt(conditions, medications, vitalSigns)
                val result = openRouterService.sendMessage(
                    messages = listOf(ChatMessage(role = "user", content = prompt)),
                    systemPrompt = "You are a friendly health educator. Provide clear, concise explanations."
                )

                // Extract and save dosages in parallel
                if (medications.isNotEmpty()) {
                    launch {
                        val dosageResult = openRouterService.extractDosages(medications)
                        dosageResult.onSuccess { dosageList ->
                            dosageList.forEach { item ->
                                val interval = item["intervalHours"]?.toIntOrNull() ?: 24
                                val medication = Medication(
                                    name = item["name"] ?: "Unknown",
                                    dosage = item["dosage"] ?: "Unknown",
                                    frequency = item["frequency"] ?: "Unknown",
                                    intervalHours = interval
                                )
                                database.medicationDao().insertMedication(medication)
                            }
                            scheduleMedicationReminders()
                        }
                    }
                }

                result.onSuccess { explanation ->
                    binding.loadingIndicator.visibility = View.GONE
                    binding.explanationText.visibility = View.VISIBLE
                    binding.explanationText.text = explanation
                    binding.continueButton.visibility = View.VISIBLE

                    // Cache the explanation to reduce future API calls
                    prefsManager.saveCachedHealthExplanation(explanation)
                }.onFailure {
                    // If API fails, just continue
                    finishExplanation()
                }
            } catch (e: Exception) {
                finishExplanation()
            }
        }
    }

    private fun buildExplanationPrompt(
        conditions: List<String>,
        medications: List<String>,
        vitalSigns: List<com.example.pharmagotchi.models.VitalSign>
    ): String {
        return buildString {
            append("Briefly explain my medical conditions and medications.\n")
            
            if (conditions.isNotEmpty()) {
                append("Conditions: ${conditions.joinToString(", ")}\n")
            }

            if (medications.isNotEmpty()) {
                append("Medications: ${medications.joinToString(", ")}\n")
            }

            if (vitalSigns.isNotEmpty()) {
                append("Metrics to track: ${vitalSigns.joinToString(", ") { it.name }}\n")
            }

            append("\nAlso, please explain the dosage instructions for my medications clearly.")
            append("\nThen, conclude by saying exactly: 'Based on these, I have set up [list metrics] for you to track.'")
            append("\nKeep the entire response under 150 words.")
        }
    }

    private fun scheduleMedicationReminders() {
        val reminderRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
            15, TimeUnit.MINUTES // Check every 15 minutes
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "medication_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }

    private fun finishExplanation() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
