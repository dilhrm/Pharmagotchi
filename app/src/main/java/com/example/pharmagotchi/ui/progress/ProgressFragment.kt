package com.example.pharmagotchi.ui.progress

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmagotchi.adapters.GraphBlockAdapter
import com.example.pharmagotchi.adapters.GraphWithData
import com.example.pharmagotchi.database.PharmagotchiDatabase
import com.example.pharmagotchi.databinding.FragmentNotificationsBinding
import com.example.pharmagotchi.models.DataPoint
import com.example.pharmagotchi.models.GraphMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.pharmagotchi.PreferencesManager
import com.example.pharmagotchi.SecurePreferencesManager
import com.example.pharmagotchi.api.OpenRouterService
import com.example.pharmagotchi.ApiKeys
import com.example.pharmagotchi.utils.EmailService
import com.example.pharmagotchi.utils.ReportGenerator

class ProgressFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: PharmagotchiDatabase
    private lateinit var graphAdapter: GraphBlockAdapter
    private lateinit var securePrefs: SecurePreferencesManager
    private lateinit var prefsManager: PreferencesManager
    private lateinit var emailService: EmailService
    private lateinit var reportGenerator: ReportGenerator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val progressViewModel = ViewModelProvider(this).get(ProgressViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        securePrefs = SecurePreferencesManager(requireContext())
        prefsManager = PreferencesManager(requireContext())
        emailService = EmailService(requireContext())
        database = PharmagotchiDatabase.getDatabase(requireContext())
        reportGenerator = ReportGenerator(requireContext())

        setupRecyclerView()
        setupClickListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            progressViewModel.graphsWithData.collectLatest { graphsWithData ->
                if (graphsWithData.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.graphsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                    binding.graphsRecyclerView.visibility = View.VISIBLE

                    graphAdapter = GraphBlockAdapter(
                        graphsWithData,
                        onAddDataClick = { graph -> showAddDataDialog(graph) },
                        onDeleteClick = { graph -> showDeleteGraphDialog(graph) }
                    )
                    binding.graphsRecyclerView.adapter = graphAdapter
                }
            }
        }

        return root
    }

    private fun setupRecyclerView() {
        binding.graphsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        binding.addGraphFab.setOnClickListener {
            showAddCustomGraphDialog()
        }
    }



    private fun showAddDataDialog(graph: GraphMetadata) {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        input.hint = "Enter value (${graph.unit})"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        builder.setTitle("Add Data for ${graph.name}")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val valueText = input.text.toString().trim()
                if (valueText.isNotEmpty()) {
                    val value = valueText.toDoubleOrNull()
                    if (value != null) {
                        // Use activity scope for the entire operation to prevent cancellation
                        requireActivity().lifecycleScope.launch {
                            try {
                                val dataPoint = DataPoint(
                                    graphId = graph.id,
                                    value = value,
                                    timestamp = System.currentTimeMillis()
                                )
                                database.graphDao().insertDataPoint(dataPoint)
                                Toast.makeText(
                                    requireContext(),
                                    "Data added successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                // Trigger analysis
                                triggerHealthAnalysis()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    requireContext(),
                                    "Error adding data: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Invalid value",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun triggerHealthAnalysis() {
        val conditions = prefsManager.getMedicalConditions()
        val medications = database.medicationDao().getAllMedicationsSync().map { it.name }
        val apiKey = securePrefs.getApiKey()

        if (apiKey.isNullOrEmpty()) return

        val openRouterService = OpenRouterService(apiKey)
        
        // Gather all recent data
        val visibleGraphs = database.graphDao().getVisibleGraphsSync()
        val dataSummary = StringBuilder()
        
        for (graph in visibleGraphs) {
            val recentPoints = database.graphDao().getRecentDataPoints(graph.id, 1)
            if (recentPoints.isNotEmpty()) {
                val point = recentPoints.first()
                dataSummary.append("${graph.name}: ${point.value} ${graph.unit}, ")
            }
        }
        
        val recentData = if (dataSummary.isNotEmpty()) {
            dataSummary.toString().removeSuffix(", ")
        } else {
            "No recent data"
        }

        try {
            val result = openRouterService.analyzeHealthStatus(conditions, medications, recentData)
            result.onSuccess { (status, message) ->
                prefsManager.saveHealthStatus(status, message)
                
                if (status == "CRITICAL") {
                    withContext(Dispatchers.Main) {
                        val contacts = database.healthContactDao().getAllContactsSync()
                        if (contacts.isNotEmpty()) {
                            sendCriticalAlertEmail(contacts, message)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendCriticalAlertEmail(contacts: List<com.example.pharmagotchi.models.HealthContact>, message: String) {
        val subject = "CRITICAL Health Alert from Pharmagotchi"
        
        // Use activity lifecycle to ensure email sends even if user navigates away
        requireActivity().lifecycleScope.launch {
            val fullReport = reportGenerator.generateComprehensiveReport()
            val body = "Pharmagotchi has detected a CRITICAL health status.\n\nAnalysis:\n$message\n\n$fullReport\n\nPlease contact the patient immediately."

            if (securePrefs.hasEmailCredentials()) {
                var successCount = 0
                for (contact in contacts) {
                    val result = emailService.sendEmail(contact.email, subject, body)
                    if (result.isSuccess) successCount++
                }
                
                if (successCount > 0) {
                    Toast.makeText(requireContext(), "Critical alert sent to $successCount contacts", Toast.LENGTH_LONG).show()
                } else {
                    // Fallback if all fail
                    openEmailIntent(contacts, subject, body)
                }
            } else {
                openEmailIntent(contacts, subject, body)
            }
        }
    }

    private fun openEmailIntent(contacts: List<com.example.pharmagotchi.models.HealthContact>, subject: String, body: String) {
        val emails = contacts.map { it.email }.toTypedArray()
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, emails)
            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            putExtra(android.content.Intent.EXTRA_TEXT, body)
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun showDeleteGraphDialog(graph: GraphMetadata) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Graph")
            .setMessage("Are you sure you want to delete ${graph.name}? All data will be lost.")
            .setPositiveButton("Delete") { _, _ ->
                requireActivity().lifecycleScope.launch {
                    try {
                        database.graphDao().deleteAllDataPointsForGraph(graph.id)
                        database.graphDao().deleteGraph(graph)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(requireContext(), "Error deleting graph: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddCustomGraphDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val nameInput = EditText(requireContext()).apply {
            hint = "Metric name (e.g., Weight)"
        }
        val unitInput = EditText(requireContext()).apply {
            hint = "Unit (e.g., kg, lbs)"
        }

        layout.addView(nameInput)
        layout.addView(unitInput)

        builder.setTitle("Add Custom Metric")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput.text.toString().trim()
                val unit = unitInput.text.toString().trim()

                if (name.isNotEmpty() && unit.isNotEmpty()) {
                    lifecycleScope.launch {
                        val graph = GraphMetadata(
                            name = name,
                            vitalSignName = name,
                            unit = unit,
                            isVisible = true,
                            isCustom = true
                        )
                        database.graphDao().insertGraph(graph)
                        Toast.makeText(
                            requireContext(),
                            "Custom metric created",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please fill in all fields",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAllDataForGraph(graph: GraphMetadata) {
        // TODO: Navigate to a detailed view showing all data points for this graph
        Toast.makeText(
            requireContext(),
            "View all data for ${graph.name} - Coming soon!",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}