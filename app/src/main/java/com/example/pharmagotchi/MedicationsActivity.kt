package com.example.pharmagotchi

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pharmagotchi.api.OpenRouterService
import com.example.pharmagotchi.database.PharmagotchiDatabase
import com.example.pharmagotchi.databinding.ActivityMedicationsBinding
import com.example.pharmagotchi.models.GraphMetadata
import com.example.pharmagotchi.models.VitalSign
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class MedicationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMedicationsBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var securePrefs: SecurePreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        preferencesManager = PreferencesManager(this)
        securePrefs = SecurePreferencesManager(this)

        setupButtons()
    }

    private fun setupButtons() {
        binding.noMedicationsButton.setOnClickListener {
            // Clear all selections
            binding.commonMedicationsChipGroup.clearCheck()
            binding.customMedicationsInput.setText("")
        }

        binding.doneButton.setOnClickListener {
            saveMedications()
            parseVitalSignsAndFinish()
        }
    }

    private fun saveMedications() {
        val selectedMedications = mutableListOf<String>()

        // Get selected chips
        val chipGroup = binding.commonMedicationsChipGroup
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                selectedMedications.add(chip.text.toString())
            }
        }

        // Get custom medications
        val customMedications = binding.customMedicationsInput.text.toString().trim()
        if (customMedications.isNotEmpty()) {
            val customList = customMedications.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            selectedMedications.addAll(customList)
        }

        // Save to preferences
        preferencesManager.saveMedications(selectedMedications)
    }

    @Suppress("DEPRECATION")
    private fun parseVitalSignsAndFinish() {
        // Check if we already parsed vital signs
        if (preferencesManager.areVitalSignsParsed()) {
            finishOnboarding()
            return
        }

        val conditions = preferencesManager.getMedicalConditions()
        val medications = preferencesManager.getMedications()

        // If no conditions or medications, skip parsing
        if (conditions.isEmpty() && medications.isEmpty()) {
            preferencesManager.setVitalSignsParsed(true)
            finishOnboarding()
            return
        }

        var apiKey = securePrefs.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            // Use hardcoded API key if no key is stored
            apiKey = ApiKeys.OPENROUTER_API_KEY
            if (apiKey.isNotEmpty()) {
                // Save it for future use
                securePrefs.saveApiKey(apiKey)
            } else {
                // Prompt for API key if hardcoded key is also empty
                promptForApiKey()
                return
            }
        }

        val progressDialog = ProgressDialog(this).apply {
            setMessage("Analyzing your health profile...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {
                val openRouterService = OpenRouterService(apiKey)
                val result = openRouterService.parseVitalSigns(conditions, medications)

                result.onSuccess { jsonResponse ->
                    val vitalSigns = parseVitalSignsJson(jsonResponse)
                    if (vitalSigns.isNotEmpty()) {
                        // Save vital signs to preferences
                        preferencesManager.saveVitalSigns(vitalSigns)

                        // Create graphs for each vital sign
                        createGraphsFromVitalSigns(vitalSigns)

                        preferencesManager.setVitalSignsParsed(true)
                        Toast.makeText(
                            this@MedicationsActivity,
                            "Created ${vitalSigns.size} health metrics to track",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        preferencesManager.setVitalSignsParsed(true)
                    }
                }.onFailure {
                    Toast.makeText(
                        this@MedicationsActivity,
                        "Couldn't analyze your health profile. You can add graphs manually.",
                        Toast.LENGTH_SHORT
                    ).show()
                    preferencesManager.setVitalSignsParsed(true)
                }
            } finally {
                progressDialog.dismiss()
                finishOnboarding()
            }
        }
    }

    private fun parseVitalSignsJson(jsonResponse: String): List<VitalSign> {
        return try {
            // Extract JSON array from the response (it might be wrapped in markdown code blocks)
            val jsonStart = jsonResponse.indexOf('[')
            val jsonEnd = jsonResponse.lastIndexOf(']') + 1
            val jsonArray = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonResponse.substring(jsonStart, jsonEnd)
            } else {
                jsonResponse
            }

            val gson = Gson()
            val type = object : TypeToken<List<VitalSign>>() {}.type
            gson.fromJson(jsonArray, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            emptyList()
        }
    }

    private suspend fun createGraphsFromVitalSigns(vitalSigns: List<VitalSign>) {
        val database = PharmagotchiDatabase.getDatabase(this)
        val graphDao = database.graphDao()

        vitalSigns.forEach { vitalSign ->
            val graph = GraphMetadata(
                name = vitalSign.name,
                vitalSignName = vitalSign.name,
                unit = vitalSign.unit,
                isVisible = true,
                isCustom = false
            )
            graphDao.insertGraph(graph)
        }
    }

    private fun promptForApiKey() {
        val builder = android.app.AlertDialog.Builder(this)
        val input = android.widget.EditText(this)
        input.hint = "Enter your OpenRouter API key"

        builder.setTitle("API Key Setup (Optional)")
            .setMessage("Enter your OpenRouter API key to enable AI-powered health insights and PharmAI chat.\n\nYou can skip this and add it later.")
            .setView(input)
            .setPositiveButton("Save & Analyze") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    securePrefs.saveApiKey(key)
                    // Retry parsing with the new key
                    parseVitalSignsAndFinish()
                } else {
                    Toast.makeText(this, "No API key provided", Toast.LENGTH_SHORT).show()
                    preferencesManager.setVitalSignsParsed(true)
                    finishOnboarding()
                }
            }
            .setNegativeButton("Skip") { _, _ ->
                preferencesManager.setVitalSignsParsed(true)
                finishOnboarding()
            }
            .setCancelable(false)
            .show()
    }

    private fun finishOnboarding() {
        // Mark first launch as completed
        preferencesManager.setFirstLaunchCompleted()

        // Navigate to explanation screen if we have health data, otherwise go to MainActivity
        val conditions = preferencesManager.getMedicalConditions()
        val medications = preferencesManager.getMedications()

        val intent = if (conditions.isNotEmpty() || medications.isNotEmpty()) {
            Intent(this, HealthExplanationActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
