package com.example.pharmagotchi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmagotchi.databinding.ItemConditionBinding

class ConditionAdapter(
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<String, ConditionAdapter.ConditionViewHolder>(ConditionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConditionViewHolder {
        val binding = ItemConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConditionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConditionViewHolder, position: Int) {
        val condition = getItem(position)
        holder.bind(condition)
    }

    inner class ConditionViewHolder(private val binding: ItemConditionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(condition: String) {
            binding.conditionName.text = condition
            binding.deleteButton.setOnClickListener {
                onDeleteClick(condition)
            }
        }
    }

    class ConditionDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
