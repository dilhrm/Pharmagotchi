package com.example.pharmagotchi

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmagotchi.database.PharmagotchiDatabase
import com.example.pharmagotchi.databinding.ActivityMedicationManagementBinding
import com.example.pharmagotchi.databinding.DialogAddMedicationBinding
import com.example.pharmagotchi.models.Medication
import kotlinx.coroutines.launch

class MedicationManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicationManagementBinding
    private lateinit var adapter: MedicationAdapter
    private val database by lazy { PharmagotchiDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        observeMedications()
    }

    private fun setupRecyclerView() {
        adapter = MedicationAdapter(
            onTakeClick = { medication ->
                takeMedication(medication)
            },
            onItemClick = { medication ->
                showEditMedicationDialog(medication)
            }
        )
        binding.medicationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.medicationsRecyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.addMedicationButton.setOnClickListener {
            showAddMedicationDialog()
        }
    }

    private fun observeMedications() {
        lifecycleScope.launch {
            database.medicationDao().getAllMedications().collect { medications ->
                adapter.submitList(medications)
            }
        }
    }

    private fun takeMedication(medication: Medication) {
        lifecycleScope.launch {
            val updatedMedication = medication.copy(lastTaken = System.currentTimeMillis())
            database.medicationDao().updateMedication(updatedMedication)
            Toast.makeText(this@MedicationManagementActivity, "Took ${medication.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddMedicationDialog() {
        val dialogBinding = DialogAddMedicationBinding.inflate(LayoutInflater.from(this))
        
        AlertDialog.Builder(this)
            .setTitle("Add Medication")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogBinding.nameInput.text.toString()
                val dosage = dialogBinding.dosageInput.text.toString()
                val frequency = dialogBinding.frequencyInput.text.toString()
                
                if (name.isNotEmpty()) {
                    val medication = Medication(name = name, dosage = dosage, frequency = frequency)
                    lifecycleScope.launch {
                        database.medicationDao().insertMedication(medication)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditMedicationDialog(medication: Medication) {
        val dialogBinding = DialogAddMedicationBinding.inflate(LayoutInflater.from(this))
        dialogBinding.nameInput.setText(medication.name)
        dialogBinding.dosageInput.setText(medication.dosage)
        dialogBinding.frequencyInput.setText(medication.frequency)

        AlertDialog.Builder(this)
            .setTitle("Edit Medication")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.nameInput.text.toString()
                val dosage = dialogBinding.dosageInput.text.toString()
                val frequency = dialogBinding.frequencyInput.text.toString()
                
                if (name.isNotEmpty()) {
                    val updatedMedication = medication.copy(name = name, dosage = dosage, frequency = frequency)
                    lifecycleScope.launch {
                        database.medicationDao().updateMedication(updatedMedication)
                    }
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    database.medicationDao().deleteMedication(medication)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
