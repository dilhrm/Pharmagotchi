package com.example.pharmagotchi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmagotchi.database.PharmagotchiDatabase
import com.example.pharmagotchi.databinding.ActivityHealthContactManagementBinding
import com.example.pharmagotchi.databinding.DialogAddContactBinding
import com.example.pharmagotchi.models.HealthContact
import com.example.pharmagotchi.utils.EmailService
import com.example.pharmagotchi.utils.ReportGenerator
import kotlinx.coroutines.launch

class HealthContactManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHealthContactManagementBinding
    private lateinit var adapter: HealthContactAdapter
    private val database by lazy { PharmagotchiDatabase.getDatabase(this) }
    private lateinit var securePrefs: SecurePreferencesManager
    private lateinit var emailService: EmailService
    private lateinit var reportGenerator: ReportGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthContactManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        securePrefs = SecurePreferencesManager(this)
        emailService = EmailService(this)
        reportGenerator = ReportGenerator(this)

        setupRecyclerView()
        setupButtons()
        observeContacts()
    }

    private fun setupRecyclerView() {
        adapter = HealthContactAdapter(
            onDeleteClick = { contact ->
                deleteContact(contact)
            },
            onContactClick = { contact ->
                // Optional: View details
            },
            onSendReportClick = { contact ->
                sendComprehensiveReport(contact)
            }
        )
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.contactsRecyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.addContactButton.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun observeContacts() {
        lifecycleScope.launch {
            database.healthContactDao().getAllContacts().collect { contacts ->
                adapter.submitList(contacts)
            }
        }
    }

    private fun deleteContact(contact: HealthContact) {
        lifecycleScope.launch {
            database.healthContactDao().deleteContact(contact)
        }
    }

    private fun sendComprehensiveReport(contact: HealthContact) {
        lifecycleScope.launch {
            android.widget.Toast.makeText(this@HealthContactManagementActivity, "Generating report...", android.widget.Toast.LENGTH_SHORT).show()
            val reportBody = reportGenerator.generateComprehensiveReport()
            val subject = "Health Report from Pharmagotchi"

            if (securePrefs.hasEmailCredentials()) {
                android.widget.Toast.makeText(this@HealthContactManagementActivity, "Sending email...", android.widget.Toast.LENGTH_SHORT).show()
                val result = emailService.sendEmail(contact.email, subject, reportBody)
                if (result.isSuccess) {
                    android.widget.Toast.makeText(this@HealthContactManagementActivity, "Report sent successfully!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(this@HealthContactManagementActivity, "Failed to send automatically. Opening email app...", android.widget.Toast.LENGTH_LONG).show()
                    openEmailIntent(contact, subject, reportBody)
                }
            } else {
                openEmailIntent(contact, subject, reportBody)
            }
        }
    }

    private fun openEmailIntent(contact: HealthContact, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(contact.email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
             android.widget.Toast.makeText(this, "No email app found.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddContactDialog() {
        val dialogBinding = DialogAddContactBinding.inflate(LayoutInflater.from(this))
        
        AlertDialog.Builder(this)
            .setTitle("Add Health Contact")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogBinding.nameInput.text.toString().trim()
                val role = dialogBinding.roleInput.text.toString().trim()
                val email = dialogBinding.emailInput.text.toString().trim()
                
                if (name.isNotEmpty() && email.isNotEmpty()) {
                    val contact = HealthContact(name = name, role = role, email = email)
                    lifecycleScope.launch {
                        database.healthContactDao().insertContact(contact)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
