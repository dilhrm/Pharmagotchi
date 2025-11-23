package com.example.pharmagotchi

import android.content.Context
import android.content.SharedPreferences
import com.example.pharmagotchi.models.VitalSign
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "pharmagotchi_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PHARMAGOTCHI_NAME = "pharmagotchi_name"
        private const val KEY_PHARMAGOTCHI_COLOR = "pharmagotchi_color"
        private const val KEY_MEDICAL_CONDITIONS = "medical_conditions"
        private const val KEY_MEDICATIONS = "medications"
        private const val KEY_VITAL_SIGNS = "vital_signs"
        private const val KEY_VITAL_SIGNS_PARSED = "vital_signs_parsed"
        private const val KEY_CACHED_CONDITIONS_EXPLANATION = "cached_conditions_explanation"
        private const val KEY_CACHED_MEDICATIONS_EXPLANATION = "cached_medications_explanation"
        private const val KEY_CACHED_HEALTH_EXPLANATION = "cached_health_explanation"
        private const val KEY_HEALTH_STATUS = "health_status"
        private const val KEY_HEALTH_MESSAGE = "health_message"
        private const val DEFAULT_COLOR = "#19A8AD" // main_blue color
    }

    fun saveHealthStatus(status: String, message: String) {
        prefs.edit()
            .putString(KEY_HEALTH_STATUS, status)
            .putString(KEY_HEALTH_MESSAGE, message)
            .apply()
    }

    fun getHealthStatus(): Pair<String, String> {
        val status = prefs.getString(KEY_HEALTH_STATUS, "NORMAL") ?: "NORMAL"
        val message = prefs.getString(KEY_HEALTH_MESSAGE, "No health data analyzed yet.") ?: "No health data analyzed yet."
        return Pair(status, message)
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun savePharmagotchiName(name: String) {
        prefs.edit().putString(KEY_PHARMAGOTCHI_NAME, name).apply()
    }

    fun getPharmagotchiName(): String {
        return prefs.getString(KEY_PHARMAGOTCHI_NAME, "Pharmagotchi") ?: "Pharmagotchi"
    }

    fun savePharmagotchiColor(colorHex: String) {
        prefs.edit().putString(KEY_PHARMAGOTCHI_COLOR, colorHex).apply()
    }

    fun getPharmagotchiColor(): String {
        return prefs.getString(KEY_PHARMAGOTCHI_COLOR, DEFAULT_COLOR) ?: DEFAULT_COLOR
    }

    fun saveMedicalConditions(conditions: List<String>) {
        val conditionsString = conditions.joinToString(",")
        prefs.edit().putString(KEY_MEDICAL_CONDITIONS, conditionsString).apply()
    }

    fun getMedicalConditions(): List<String> {
        val conditionsString = prefs.getString(KEY_MEDICAL_CONDITIONS, "") ?: ""
        return if (conditionsString.isEmpty()) {
            emptyList()
        } else {
            conditionsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    fun saveMedications(medications: List<String>) {
        val medicationsString = medications.joinToString(",")
        prefs.edit().putString(KEY_MEDICATIONS, medicationsString).apply()
    }

    fun getMedications(): List<String> {
        val medicationsString = prefs.getString(KEY_MEDICATIONS, "") ?: ""
        return if (medicationsString.isEmpty()) {
            emptyList()
        } else {
            medicationsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    fun saveVitalSigns(vitalSigns: List<VitalSign>) {
        val json = gson.toJson(vitalSigns)
        prefs.edit().putString(KEY_VITAL_SIGNS, json).apply()
    }

    fun getVitalSigns(): List<VitalSign> {
        val json = prefs.getString(KEY_VITAL_SIGNS, null) ?: return emptyList()
        val type = object : TypeToken<List<VitalSign>>() {}.type
        return gson.fromJson(json, type)
    }

    fun setVitalSignsParsed(parsed: Boolean) {
        prefs.edit().putBoolean(KEY_VITAL_SIGNS_PARSED, parsed).apply()
    }

    fun areVitalSignsParsed(): Boolean {
        return prefs.getBoolean(KEY_VITAL_SIGNS_PARSED, false)
    }

    // Cache AI explanations to reduce API calls
    fun saveCachedConditionsExplanation(explanation: String) {
        prefs.edit().putString(KEY_CACHED_CONDITIONS_EXPLANATION, explanation).apply()
    }

    fun getCachedConditionsExplanation(): String? {
        return prefs.getString(KEY_CACHED_CONDITIONS_EXPLANATION, null)
    }

    fun saveCachedMedicationsExplanation(explanation: String) {
        prefs.edit().putString(KEY_CACHED_MEDICATIONS_EXPLANATION, explanation).apply()
    }

    fun getCachedMedicationsExplanation(): String? {
        return prefs.getString(KEY_CACHED_MEDICATIONS_EXPLANATION, null)
    }

    fun saveCachedHealthExplanation(explanation: String) {
        prefs.edit().putString(KEY_CACHED_HEALTH_EXPLANATION, explanation).apply()
    }

    fun getCachedHealthExplanation(): String? {
        return prefs.getString(KEY_CACHED_HEALTH_EXPLANATION, null)
    }
}
