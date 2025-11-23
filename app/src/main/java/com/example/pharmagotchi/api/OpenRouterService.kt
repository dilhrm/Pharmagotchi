package com.example.pharmagotchi.api

import com.example.pharmagotchi.models.ChatMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenRouterService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mediaType = "application/json".toMediaType()

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1"
        private const val CHAT_ENDPOINT = "$BASE_URL/chat/completions"
        private const val MODEL = "anthropic/claude-3.5-sonnet" // You can change this
    }

    suspend fun sendMessage(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestMessages = mutableListOf<Map<String, String>>()

            if (systemPrompt != null) {
                requestMessages.add(mapOf("role" to "system", "content" to systemPrompt))
            }

            messages.forEach { msg ->
                requestMessages.add(mapOf("role" to msg.role, "content" to msg.content))
            }

            val requestBody = JsonObject().apply {
                addProperty("model", MODEL)
                add("messages", gson.toJsonTree(requestMessages))
            }

            val request = Request.Builder()
                .url(CHAT_ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("API call failed: ${response.code} ${response.message}")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val content = jsonResponse
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseVitalSigns(
        conditions: List<String>,
        medications: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
You are a medical knowledge assistant. Given the following medical conditions and medications, identify the key vital signs and health metrics that should be tracked.

Medical Conditions: ${conditions.joinToString(", ")}
Medications: ${medications.joinToString(", ")}

For each vital sign, provide:
1. Name of the vital sign
2. Unit of measurement
3. Category (cardiovascular, metabolic, respiratory, etc.)
4. Normal range (if applicable)
5. Which conditions/medications it relates to

Return your response as a JSON array of vital signs in this exact format:
[
  {
    "name": "Blood Pressure",
    "unit": "mmHg",
    "category": "cardiovascular",
    "normalRange": "120/80",
    "relatedConditions": ["Hypertension"],
    "relatedMedications": ["Lisinopril"]
  }
]

Only include vital signs that are truly relevant to the given conditions and medications.
        """.trimIndent()

        sendMessage(
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            systemPrompt = "You are a medical assistant that provides structured JSON responses."
        )
    }

    suspend fun analyzeHealthStatus(
        conditions: List<String>,
        medications: List<String>,
        recentData: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val prompt = """
Analyze the following health data in the context of the user's conditions and medications.
Conditions: ${conditions.joinToString(", ")}
Medications: ${medications.joinToString(", ")}
Recent Data: $recentData

Determine the health status (NORMAL, WARNING, or CRITICAL) and provide a concise 1-sentence summary for the user.
Return ONLY a JSON object in this format:
{
  "status": "NORMAL",
  "message": "Your heart rate is slightly elevated, consider resting."
}
"""
        try {
            val requestBody = JsonObject().apply {
                addProperty("model", MODEL)
                add("messages", gson.toJsonTree(listOf(
                    mapOf("role" to "system", "content" to "You are a medical analysis AI. Output only JSON."),
                    mapOf("role" to "user", "content" to prompt)
                )))
            }

            val request = Request.Builder()
                .url(CHAT_ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val content = jsonResponse
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString

            // Parse the JSON content from the response
            // Sometimes LLMs wrap JSON in markdown code blocks, strip them
            val cleanJson = content.replace("```json", "").replace("```", "").trim()
            val resultJson = gson.fromJson(cleanJson, JsonObject::class.java)

            val status = resultJson.get("status").asString
            val message = resultJson.get("message").asString

            Result.success(Pair(status, message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractDosages(
        medications: List<String>
    ): Result<List<Map<String, String>>> = withContext(Dispatchers.IO) {
        val prompt = """
Extract dosage and frequency information for the following medications:
${medications.joinToString(", ")}

Return ONLY a JSON array of objects with these fields:
- name: (medication name)
- dosage: (e.g., "10mg", "500mg")
- frequency: (e.g., "Once daily", "Twice a day")
- intervalHours: (integer, estimated hours between doses. e.g., 24 for once daily, 12 for twice, 8 for 3 times. Default to 24 if unknown)

Example:
[
  {"name": "Lisinopril", "dosage": "10mg", "frequency": "Once daily", "intervalHours": "24"}
]
If dosage/frequency is unknown, use "Unknown".
"""
        try {
            val requestBody = JsonObject().apply {
                addProperty("model", MODEL)
                add("messages", gson.toJsonTree(listOf(
                    mapOf("role" to "system", "content" to "You are a medical data extractor. Output only JSON."),
                    mapOf("role" to "user", "content" to prompt)
                )))
            }

            val request = Request.Builder()
                .url(CHAT_ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val content = jsonResponse
                .getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString

            val cleanJson = content.replace("```json", "").replace("```", "").trim()
            val type = object : com.google.gson.reflect.TypeToken<List<Map<String, String>>>() {}.type
            val resultList: List<Map<String, String>> = gson.fromJson(cleanJson, type)

            Result.success(resultList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
