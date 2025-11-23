package com.example.pharmagotchi.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.pharmagotchi.MedicationManagementActivity
import com.example.pharmagotchi.MedicalConditionsManagementActivity
import com.example.pharmagotchi.PreferencesManager
import com.example.pharmagotchi.R
import com.example.pharmagotchi.databinding.FragmentDashboardBinding

import androidx.lifecycle.lifecycleScope
import com.example.pharmagotchi.utils.PetStatusManager
import com.example.pharmagotchi.models.PetEmotion
import kotlinx.coroutines.launch
import android.app.AlertDialog

class HomeFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var petStatusManager: PetStatusManager

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        preferencesManager = PreferencesManager(requireContext())
        petStatusManager = PetStatusManager(requireContext())

        // Load customized pharmagotchi
        loadPharmagotchi()
        
        // Update status
        updatePetStatus()

        // Start bounce animation
        startBounceAnimation()

        binding.manageMedicationsButton.setOnClickListener {
            startActivity(Intent(requireContext(), MedicationManagementActivity::class.java))
        }

        binding.manageConditionsButton.setOnClickListener {
            startActivity(Intent(requireContext(), MedicalConditionsManagementActivity::class.java))
        }

        return root
    }
    
    override fun onResume() {
        super.onResume()
        updatePetStatus()
    }

    private fun updatePetStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            val status = petStatusManager.determineStatus()
            val name = preferencesManager.getPharmagotchiName()
            
            val emotionText = when (status.emotion) {
                PetEmotion.HAPPY -> "is Happy"
                PetEmotion.SAD -> "is Sad"
                PetEmotion.CONFUSED -> "is Confused"
                PetEmotion.IN_PAIN -> "is in Pain"
            }
            
            binding.pharmagotchiNameText.text = "$name $emotionText"
            binding.pharmagotchiStatusSubtitle.text = status.reason
            
            // Update visuals based on emotion
            val baseColor = preferencesManager.getPharmagotchiColor()
            val colorToApply = when (status.emotion) {
                PetEmotion.HAPPY -> baseColor // Original color
                PetEmotion.SAD -> "#808080" // Grey
                PetEmotion.CONFUSED -> "#FFA500" // Orange
                PetEmotion.IN_PAIN -> "#FF0000" // Red
            }
            
            applyPetColor(colorToApply)
            
            // Click listener for explanation
            binding.pharmagotchiContainer.setOnClickListener {
                showStatusExplanation(name, status.reason)
            }
        }
    }
    
    private fun showStatusExplanation(name: String, reason: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("How is $name feeling?")
            .setMessage(reason)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadPharmagotchi() {
        // Initial load, will be updated by updatePetStatus
        val name = preferencesManager.getPharmagotchiName()
        binding.pharmagotchiNameText.text = name
        applyPetColor(preferencesManager.getPharmagotchiColor())
    }
    
    private fun applyPetColor(colorHex: String) {
        binding.pharmagotchiBody.setColorFilter(
            Color.parseColor(colorHex),
            PorterDuff.Mode.MULTIPLY
        )
        binding.pharmagotchiFeet.setColorFilter(
            Color.parseColor(colorHex),
            PorterDuff.Mode.MULTIPLY
        )
        
        // Apply background color (lightened version)
        val color = Color.parseColor(colorHex)
        val lightenedColor = lightenColor(color, 0.85f)
        binding.homeBackground.setBackgroundColor(lightenedColor)
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        val newRed = (red + (255 - red) * factor).toInt()
        val newGreen = (green + (255 - green) * factor).toInt()
        val newBlue = (blue + (255 - blue) * factor).toInt()

        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun startBounceAnimation() {
        val bounceAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_animation)
        binding.pharmagotchiBody.startAnimation(bounceAnimation)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}