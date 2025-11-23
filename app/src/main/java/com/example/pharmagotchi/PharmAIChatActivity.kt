package com.example.pharmagotchi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmagotchi.adapters.ChatAdapter
import com.example.pharmagotchi.api.OpenRouterService
import com.example.pharmagotchi.database.PharmagotchiDatabase
import com.example.pharmagotchi.databinding.ActivityPharmAiChatBinding
import com.example.pharmagotchi.models.ChatMessage
import com.example.pharmagotchi.models.HealthContact
import com.example.pharmagotchi.utils.EmailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PharmAIChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPharmAiChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var openRouterService: OpenRouterService
    private lateinit var securePrefs: SecurePreferencesManager
    private lateinit var prefsManager: PreferencesManager
    private val chatMessages = mutableListOf<ChatMessage>()
    private val database by lazy { PharmagotchiDatabase.getDatabase(this) }
    private lateinit var emailService: EmailService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPharmAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        securePrefs = SecurePreferencesManager(this)
        prefsManager = PreferencesManager(this)
        emailService = EmailService(this)

        setupRecyclerView()
        setupClickListeners()
        checkApiKey()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PharmAIChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            processUserMessage(text)
        }
    }

    private fun checkApiKey() {
        var apiKey = securePrefs.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            // Use hardcoded API key if no key is stored
            apiKey = ApiKeys.OPENROUTER_API_KEY
            if (apiKey.isNotEmpty()) {
                // Save it for future use
                securePrefs.saveApiKey(apiKey)
                openRouterService = OpenRouterService(apiKey)
                addWelcomeMessage()
                checkInitialPrompt()
            } else {
                showApiKeyDialog()
            }
        } else {
            openRouterService = OpenRouterService(apiKey)
            addWelcomeMessage()
            checkInitialPrompt()
        }
    }

    private fun checkInitialPrompt() {
        val initialPrompt = intent.getStringExtra("INITIAL_PROMPT")
        if (!initialPrompt.isNullOrEmpty()) {
            processUserMessage(initialPrompt)
            intent.removeExtra("INITIAL_PROMPT")
        }
    }

    private fun showApiKeyDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        val input = android.widget.EditText(this)
        input.hint = "Enter your OpenRouter API key"

        builder.setTitle("API Key Required")
            .setMessage("Please enter your OpenRouter API key to use PharmAI")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val apiKey = input.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    securePrefs.saveApiKey(apiKey)
                    openRouterService = OpenRouterService(apiKey)
                    addWelcomeMessage()
                    checkInitialPrompt()
                } else {
                    Toast.makeText(this, "API key is required", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun addWelcomeMessage() {
        val name = prefsManager.getPharmagotchiName()
        val conditions = prefsManager.getMedicalConditions()
        val medications = prefsManager.getMedications()

        val welcomeText = buildString {
            append("Hi! I'm $name, your virtual pharmacist buddy. ")
            append("I'm here to help you understand your health better.\n\n")

            if (conditions.isNotEmpty()) {
                append("I see you have: ${conditions.joinToString(", ")}\n")
            }
            if (medications.isNotEmpty()) {
                append("And you're taking: ${medications.joinToString(", ")}\n")
            }

            append("\nFeel free to ask me anything about your medications, conditions, or general health questions!")
        }

        chatMessages.add(ChatMessage(role = "assistant", content = welcomeText))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun processUserMessage(messageText: String) {
        if (messageText.isEmpty()) return

        // Add user message
        chatMessages.add(ChatMessage(role = "user", content = messageText))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)

        // Clear input if it matches (only if called from UI)
        if (binding.messageInput.text.toString() == messageText) {
            binding.messageInput.text?.clear()
        }

        // Show loading
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.sendButton.isEnabled = false

        // Send to API
        lifecycleScope.launch {
            try {
                // Check for email intent
                if (messageText.lowercase().contains("email") || messageText.lowercase().contains("contact")) {
                    handleEmailRequest(messageText)
                    binding.loadingIndicator.visibility = View.GONE
                    binding.sendButton.isEnabled = true
                    return@launch
                }

                val systemPrompt = buildSystemPrompt()

                // Optimization: Only send last 10 messages to save tokens
                val messagesToSend = if (chatMessages.size > 10) {
                    chatMessages.takeLast(10)
                } else {
                    chatMessages
                }

                val result = openRouterService.sendMessage(
                    messages = messagesToSend,
                    systemPrompt = systemPrompt
                )

                result.onSuccess { response ->
                    chatMessages.add(ChatMessage(role = "assistant", content = response))
                    chatAdapter.notifyItemInserted(chatMessages.size - 1)
                    binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                }.onFailure { error ->
                    Toast.makeText(
                        this@PharmAIChatActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                binding.loadingIndicator.visibility = View.GONE
                binding.sendButton.isEnabled = true
            }
        }
    }

    private fun buildSystemPrompt(): String {
        val conditions = prefsManager.getMedicalConditions()
        val medications = prefsManager.getMedications()
        val name = prefsManager.getPharmagotchiName()

        return """
You are $name, a friendly and knowledgeable virtual pharmacist assistant. You provide helpful, accurate information about medications, medical conditions, and general health topics.

User's Medical Profile:
- Medical Conditions: ${conditions.joinToString(", ").ifEmpty { "None specified" }}
- Medications: ${medications.joinToString(", ").ifEmpty { "None specified" }}

Guidelines:
1. Be friendly, supportive, and easy to understand
2. Explain medical concepts in simple terms
3. Always remind users to consult their healthcare provider for medical decisions
4. Provide accurate, evidence-based information
5. If you're unsure about something, say so
6. Keep responses concise but informative
        """.trimIndent()
    }

    private suspend fun handleEmailRequest(messageText: String) {
        val contacts = database.healthContactDao().getAllContactsSync()
        if (contacts.isEmpty()) {
            addBotMessage("You don't have any health contacts saved yet. Please add them in the Manage Contacts section.")
            return
        }

        // Simple heuristic: check if any contact name is in the message
        val targetContact = contacts.find { contact ->
            messageText.contains(contact.name, ignoreCase = true)
        }

        if (targetContact != null) {
            // Ask for message content if not provided (simplified for now, just open email)
            // In a real chat flow, we'd maintain state to ask "What do you want to say?"
            // For now, let's assume the user wants to send an email and open the intent.

            withContext(Dispatchers.Main) {
                sendEmail(targetContact)
                addBotMessage("I've opened your email app to send a message to ${targetContact.name}.")
            }
        } else {
            // Ask user to select a contact
            withContext(Dispatchers.Main) {
                showContactSelectionDialog(contacts)
            }
        }
    }

    private fun showContactSelectionDialog(contacts: List<HealthContact>) {
        val names = contacts.map { "${it.name} (${it.role})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Who do you want to contact?")
            .setItems(names) { _, which ->
                val selectedContact = contacts[which]
                sendEmail(selectedContact)
                addBotMessage("I've opened your email app to send a message to ${selectedContact.name}.")
            }
            .setNegativeButton("Cancel") { _, _ ->
                addBotMessage("Okay, I cancelled the email request.")
            }
            .show()
    }

    private fun sendEmail(contact: HealthContact) {
        if (securePrefs.hasEmailCredentials()) {
            // Send directly
            addBotMessage("Sending email to ${contact.name}...")
            lifecycleScope.launch {
                val result = emailService.sendEmail(
                    contact.email,
                    "Health Update from Pharmagotchi",
                    "This is an automated message from Pharmagotchi." // In a real app, we'd ask for content
                )
                if (result.isSuccess) {
                    addBotMessage("Email sent successfully to ${contact.name}!")
                } else {
                    addBotMessage("Failed to send email: ${result.exceptionOrNull()?.message}. Opening email app instead.")
                    openEmailIntent(contact)
                }
            }
        } else {
            openEmailIntent(contact)
        }
    }

    private fun openEmailIntent(contact: HealthContact) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(contact.email))
            putExtra(Intent.EXTRA_SUBJECT, "Health Update from Pharmagotchi")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addBotMessage(content: String) {
        chatMessages.add(ChatMessage(role = "assistant", content = content))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        binding.chatRecyclerView.scrollToPosition(chatMessages.size - 1)
    }
}
