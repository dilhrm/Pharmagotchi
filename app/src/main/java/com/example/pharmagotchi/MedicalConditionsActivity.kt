package com.example.pharmagotchi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pharmagotchi.databinding.ActivityMedicalConditionsBinding
import com.google.android.material.chip.Chip

class MedicalConditionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMedicalConditionsBinding
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        preferencesManager = PreferencesManager(this)

        setupButtons()
    }

    private fun setupButtons() {
        binding.noConditionsButton.setOnClickListener {
            // Clear all selections
            binding.commonConditionsChipGroup.clearCheck()
            binding.customConditionsInput.setText("")
        }

        binding.nextButton.setOnClickListener {
            saveConditions()

            // Navigate to medications screen
            val intent = Intent(this, MedicationsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun saveConditions() {
        val selectedConditions = mutableListOf<String>()

        // Get selected chips
        val chipGroup = binding.commonConditionsChipGroup
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                selectedConditions.add(chip.text.toString())
            }
        }

        // Get custom conditions
        val customConditions = binding.customConditionsInput.text.toString().trim()
        if (customConditions.isNotEmpty()) {
            val customList = customConditions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            selectedConditions.addAll(customList)
        }

        // Save to preferences
        preferencesManager.saveMedicalConditions(selectedConditions)
    }
}
