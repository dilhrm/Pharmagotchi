package com.example.pharmagotchi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmagotchi.databinding.ItemHealthContactBinding
import com.example.pharmagotchi.models.HealthContact

class HealthContactAdapter(
    private val onDeleteClick: (HealthContact) -> Unit,
    private val onContactClick: (HealthContact) -> Unit,
    private val onSendReportClick: (HealthContact) -> Unit
) : ListAdapter<HealthContact, HealthContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemHealthContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)
        holder.bind(contact)
    }

    inner class ContactViewHolder(private val binding: ItemHealthContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: HealthContact) {
            binding.contactName.text = contact.name
            binding.contactRole.text = contact.role
            binding.contactEmail.text = contact.email

            binding.deleteButton.setOnClickListener {
                onDeleteClick(contact)
            }

            binding.sendReportButton.setOnClickListener {
                onSendReportClick(contact)
            }
            
            binding.root.setOnClickListener {
                onContactClick(contact)
            }
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<HealthContact>() {
        override fun areItemsTheSame(oldItem: HealthContact, newItem: HealthContact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HealthContact, newItem: HealthContact): Boolean {
            return oldItem == newItem
        }
    }
}
