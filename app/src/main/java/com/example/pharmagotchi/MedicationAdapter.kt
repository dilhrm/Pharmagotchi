package com.example.pharmagotchi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmagotchi.models.Medication
import com.example.pharmagotchi.databinding.ItemMedicationBinding

class MedicationAdapter(
    private val onTakeClick: (Medication) -> Unit,
    private val onItemClick: (Medication) -> Unit
) : ListAdapter<Medication, MedicationAdapter.MedicationViewHolder>(MedicationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val binding = ItemMedicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = getItem(position)
        holder.bind(medication)
    }

    inner class MedicationViewHolder(private val binding: ItemMedicationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(medication: Medication) {
            binding.medicationName.text = medication.name
            binding.medicationDosage.text = medication.dosage
            binding.medicationFrequency.text = medication.frequency

            binding.takeButton.setOnClickListener {
                onTakeClick(medication)
            }

            binding.root.setOnClickListener {
                onItemClick(medication)
            }
        }
    }

    class MedicationDiffCallback : DiffUtil.ItemCallback<Medication>() {
        override fun areItemsTheSame(oldItem: Medication, newItem: Medication): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medication, newItem: Medication): Boolean {
            return oldItem == newItem
        }
    }
}
