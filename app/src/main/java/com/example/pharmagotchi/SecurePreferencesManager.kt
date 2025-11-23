package com.example.pharmagotchi

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_pharmagotchi_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
    }

    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_OPENROUTER_API_KEY, apiKey).apply()
    }

    fun getApiKey(): String? {
        return encryptedPrefs.getString(KEY_OPENROUTER_API_KEY, null)
    }

    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrEmpty()
    }

    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_OPENROUTER_API_KEY).apply()
    }

    fun getEmailCredentials(): Pair<String?, String?> {
        return Pair(EmailCredentials.EMAIL, EmailCredentials.PASSWORD)
    }

    fun hasEmailCredentials(): Boolean {
        return true
    }
}
