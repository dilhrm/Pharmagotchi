package com.example.pharmagotchi

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmagotchi.databinding.ActivityMedicalConditionsManagementBinding

class MedicalConditionsManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicalConditionsManagementBinding
    private lateinit var adapter: ConditionAdapter
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalConditionsManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        setupRecyclerView()
        setupButtons()
        loadConditions()
    }

    private fun setupRecyclerView() {
        adapter = ConditionAdapter(
            onDeleteClick = { condition ->
                deleteCondition(condition)
            }
        )
        binding.conditionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.conditionsRecyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.addConditionButton.setOnClickListener {
            showAddConditionDialog()
        }
    }

    private fun loadConditions() {
        val conditions = prefsManager.getMedicalConditions()
        adapter.submitList(conditions)
    }

    private fun deleteCondition(condition: String) {
        val currentConditions = prefsManager.getMedicalConditions().toMutableList()
        currentConditions.remove(condition)
        prefsManager.saveMedicalConditions(currentConditions)
        loadConditions()
    }

    private fun showAddConditionDialog() {
        val input = EditText(this)
        input.hint = "Condition Name"
        
        AlertDialog.Builder(this)
            .setTitle("Add Condition")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val currentConditions = prefsManager.getMedicalConditions().toMutableList()
                    if (!currentConditions.contains(name)) {
                        currentConditions.add(name)
                        prefsManager.saveMedicalConditions(currentConditions)
                        loadConditions()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
