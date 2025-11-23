package com.example.pharmagotchi.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmagotchi.databinding.ItemChatMessageBinding
import com.example.pharmagotchi.models.ChatMessage

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.messageText.text = message.content

            val params = binding.messageCard.layoutParams as ConstraintLayout.LayoutParams

            if (message.role == "user") {
                // User message - align right, blue background
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.startToStart = ConstraintLayout.LayoutParams.UNSET
                params.horizontalBias = 1f
                binding.messageCard.setCardBackgroundColor(Color.parseColor("#19A8AD"))
                binding.messageText.setTextColor(Color.WHITE)
            } else {
                // Assistant message - align left, gray background
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.UNSET
                params.horizontalBias = 0f
                binding.messageCard.setCardBackgroundColor(Color.parseColor("#F0F0F0"))
                binding.messageText.setTextColor(Color.BLACK)
            }

            binding.messageCard.layoutParams = params
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}
