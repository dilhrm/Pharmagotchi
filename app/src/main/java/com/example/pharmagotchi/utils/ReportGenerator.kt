package com.example.pharmagotchi.utils

import android.content.Context
import com.example.pharmagotchi.PreferencesManager
import com.example.pharmagotchi.database.PharmagotchiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.pharmagotchi.SecurePreferencesManager
import com.example.pharmagotchi.api.OpenRouterService
import com.example.pharmagotchi.models.ChatMessage

class ReportGenerator(private val context: Context) {

    private val database = PharmagotchiDatabase.getDatabase(context)
    private val prefsManager = PreferencesManager(context)
    private val securePrefs = SecurePreferencesManager(context)

    suspend fun generateComprehensiveReport(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        // Gather Data
        val (status, message) = prefsManager.getHealthStatus()
        val conditions = prefsManager.getMedicalConditions()
        val medications = database.medicationDao().getAllMedicationsSync()
        val graphs = database.graphDao().getVisibleGraphsSync()
        
        val dataBuilder = StringBuilder()
        dataBuilder.append("Date: $currentDate\n")
        dataBuilder.append("Current Status: $status ($message)\n")
        dataBuilder.append("Conditions: ${if (conditions.isNotEmpty()) conditions.joinToString(", ") else "None"}\n")
        dataBuilder.append("Medications: ${if (medications.isNotEmpty()) medications.joinToString(", ") { "${it.name} (${it.dosage})" } else "None"}\n")
        
        dataBuilder.append("Recent Data:\n")
        if (graphs.isNotEmpty()) {
            for (graph in graphs) {
                val points = database.graphDao().getRecentDataPoints(graph.id, 5)
                if (points.isNotEmpty()) {
                    dataBuilder.append("- ${graph.name} (${graph.unit}): ${points.joinToString(", ") { "${it.value} @ ${dateFormat.format(Date(it.timestamp))}" }}\n")
                }
            }
        } else {
            dataBuilder.append("No recent data recorded.\n")
        }

        val rawData = dataBuilder.toString()

        // Try AI Generation
        val apiKey = securePrefs.getApiKey()
        if (!apiKey.isNullOrEmpty()) {
            try {
                val openRouterService = OpenRouterService(apiKey)
                val prompt = """
                    You are a professional medical assistant for the Pharmagotchi app.
                    Generate a comprehensive, easy-to-read health report for a patient based on the following data.
                    
                    $rawData
                    
                    The report should:
                    1. Summarize the patient's current status and conditions.
                    2. List current medications.
                    3. Analyze the recent data trends (e.g., is blood pressure stable? is weight increasing?).
                    4. Highlight any potential concerns or "CRITICAL" statuses.
                    5. Be formatted clearly with sections.
                    6. Be written in a professional yet caring tone, suitable for sending to a doctor or caregiver.
                """.trimIndent()

                val result = openRouterService.sendMessage(
                    messages = listOf(ChatMessage(role = "user", content = prompt)),
                    systemPrompt = "You are a helpful medical assistant."
                )

                if (result.isSuccess) {
                    return@withContext result.getOrNull() ?: rawData
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to raw data report if AI fails
        sb.append("PHARMAGOTCHI HEALTH REPORT (Raw Data)\n")
        sb.append(rawData)
        return@withContext sb.toString()
    }
}
