package com.example.pharmagotchi.ui.pharmai

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.pharmagotchi.PharmAIChatActivity
import com.example.pharmagotchi.PreferencesManager
import com.example.pharmagotchi.databinding.FragmentHomeBinding

import android.content.SharedPreferences

class PharmAIFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefsManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        prefsManager = PreferencesManager(requireContext())
        
        // Register listener for preference changes
        val prefs = requireContext().getSharedPreferences("pharmagotchi_prefs", android.content.Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)

        setupUI()
        setupClickListeners()

        return root
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "health_status" || key == "health_message") {
            activity?.runOnUiThread {
                updateHealthStatus()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateHealthStatus()
    }

    private fun updateHealthStatus() {
        val (status, message) = prefsManager.getHealthStatus()
        
        binding.statusTitle.text = "Health Status: $status"
        binding.statusMessage.text = message

        val color = when (status.uppercase()) {
            "WARNING" -> Color.parseColor("#FFA500") // Orange
            "CRITICAL" -> Color.RED
            else -> Color.parseColor("#19A8AD") // Main Blue
        }
        
        binding.statusTitle.setTextColor(color)
        binding.statusCard.strokeColor = color
        binding.statusCard.strokeWidth = if (status == "NORMAL") 0 else 4
    }

    private fun setupUI() {
        // No longer applying color filter to the physician icon as it should keep its original colors
        // or if it's a monochrome icon, we can tint it. Assuming physicianpng is a full color image.
        // If it's an icon that needs tinting, uncomment below:
        /*
        val colorHex = prefsManager.getPharmagotchiColor()
        binding.chatIcon.setColorFilter(
            Color.parseColor(colorHex),
            PorterDuff.Mode.MULTIPLY
        )
        */
    }

    private fun setupClickListeners() {
        binding.manageContactsButton.setOnClickListener {
            val intent = Intent(requireContext(), com.example.pharmagotchi.HealthContactManagementActivity::class.java)
            startActivity(intent)
        }

        binding.startChatButton.setOnClickListener {
            val intent = Intent(requireContext(), PharmAIChatActivity::class.java)
            startActivity(intent)
        }

        binding.conditionsButton.setOnClickListener {
            val intent = Intent(requireContext(), PharmAIChatActivity::class.java)
            intent.putExtra("INITIAL_PROMPT", "Please explain my medical conditions in simple terms.")
            startActivity(intent)
        }

        binding.medicationsButton.setOnClickListener {
            val intent = Intent(requireContext(), PharmAIChatActivity::class.java)
            intent.putExtra("INITIAL_PROMPT", "Please explain my medications, how they work, and potential side effects.")
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val prefs = requireContext().getSharedPreferences("pharmagotchi_prefs", android.content.Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        _binding = null
    }
}