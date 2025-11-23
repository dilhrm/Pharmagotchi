package com.example.pharmagotchi

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pharmagotchi.databinding.ActivityCustomizationBinding

class CustomizationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomizationBinding
    private lateinit var preferencesManager: PreferencesManager
    private var selectedColor: String = "#19A8AD" // Default main_blue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar
        supportActionBar?.hide()

        preferencesManager = PreferencesManager(this)

        // Start bounce animation
        startBounceAnimation()

        setupColorPicker()
        setupDoneButton()
    }

    private fun startBounceAnimation() {
        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce_animation)
        binding.pharmagotchiBody.startAnimation(bounceAnimation)
    }

    private fun setupColorPicker() {
        // Set up click listeners for each color option
        binding.colorBlue.setOnClickListener {
            selectColor("#2196F3", binding.colorBlue)
        }

        binding.colorPurple.setOnClickListener {
            val purpleColor = ContextCompat.getColor(this, R.color.purple_500)
            selectColor(String.format("#%06X", 0xFFFFFF and purpleColor), binding.colorPurple)
        }

        binding.colorTeal.setOnClickListener {
            val tealColor = ContextCompat.getColor(this, R.color.teal_700)
            selectColor(String.format("#%06X", 0xFFFFFF and tealColor), binding.colorTeal)
        }

        binding.colorPink.setOnClickListener {
            selectColor("#FF69B4", binding.colorPink)
        }

        binding.colorOrange.setOnClickListener {
            selectColor("#FF8C00", binding.colorOrange)
        }

        // Set default selection (blue)
        selectColor("#19A8AD", binding.colorBlue)
    }

    private fun selectColor(colorHex: String, selectedView: android.view.View) {
        selectedColor = colorHex

        // Reset all borders
        binding.colorBlue.setBackgroundColor(Color.parseColor("#2196F3"))
        binding.colorPurple.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
        binding.colorTeal.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
        binding.colorPink.setBackgroundColor(Color.parseColor("#FF69B4"))
        binding.colorOrange.setBackgroundColor(Color.parseColor("#FF8C00"))

        // Add scale effect to selected view
        selectedView.scaleX = 1.2f
        selectedView.scaleY = 1.2f

        // Reset scale for others
        listOf(binding.colorBlue, binding.colorPurple, binding.colorTeal,
               binding.colorPink, binding.colorOrange).forEach {
            if (it != selectedView) {
                it.scaleX = 1.0f
                it.scaleY = 1.0f
            }
        }

        // Apply color tint to body only (not feet)
        binding.pharmagotchiBody.setColorFilter(
            Color.parseColor(colorHex),
            PorterDuff.Mode.MULTIPLY
        )
    }

    private fun setupDoneButton() {
        binding.doneButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name for your Pharmagotchi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save preferences
            preferencesManager.savePharmagotchiName(name)
            preferencesManager.savePharmagotchiColor(selectedColor)

            // Navigate to MedicalConditionsActivity
            val intent = Intent(this, MedicalConditionsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
