package com.example.pharmagotchi

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.pharmagotchi.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager(this)

        // Check if this is the first launch
        if (!preferencesManager.isFirstLaunch()) {
            // Not first launch, go directly to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // First launch, show welcome screen
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar for splash screen
        supportActionBar?.hide()

        // Start bounce animation
        startBounceAnimation()

        binding.beginButton.setOnClickListener {
            // Navigate to customization screen
            val intent = Intent(this, CustomizationActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun startBounceAnimation() {
        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce_animation)
        binding.pharmagotchiBody.startAnimation(bounceAnimation)
    }
}
