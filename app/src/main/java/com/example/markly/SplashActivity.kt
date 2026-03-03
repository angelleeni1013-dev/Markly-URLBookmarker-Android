package com.example.markly

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashDuration = 3000L
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val PREF_TERMS_ACCEPTED = "terms_accepted"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hide action bar
        supportActionBar?.hide()

        // Find logo and apply bounce animation
        val logo = findViewById<ImageView>(R.id.ivSplashLogo)
        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)
        logo.startAnimation(bounceAnimation)

        // SharedPreferences
        sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)

        // Check if terms already accepted
        val termsAccepted = sharedPreferences.getBoolean(PREF_TERMS_ACCEPTED, false)
        val splashEnabled = sharedPreferences.getBoolean(MainActivity.PREF_SPLASH_ENABLED, true)

        if (!termsAccepted) {
            // First time user - show terms after splash
            Handler(Looper.getMainLooper()).postDelayed({
                showTermsDialog()
            }, splashDuration)
        } else if (splashEnabled) {
            // Terms accepted, splash enabled - show splash then go to main
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMain()
            }, splashDuration)
        } else {
            // Terms accepted, splash disabled - go directly to main
            navigateToMain()
        }
    }

    private fun showTermsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_terms, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)  // User must accept or decline
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cbAgree = dialogView.findViewById<CheckBox>(R.id.cbAgree)
        val btnAccept = dialogView.findViewById<Button>(R.id.btnAccept)
        val btnDecline = dialogView.findViewById<Button>(R.id.btnDecline)

        // Enable Accept button only when checkbox is checked
        cbAgree.setOnCheckedChangeListener { _, isChecked ->
            btnAccept.isEnabled = isChecked
            btnAccept.alpha = if (isChecked) 1.0f else 0.5f
        }

        // Set initial alpha for disabled state
        btnAccept.alpha = 0.5f

        // Accept button
        btnAccept.setOnClickListener {
            // Save that terms are accepted
            sharedPreferences.edit()
                .putBoolean(PREF_TERMS_ACCEPTED, true)
                .apply()

            Toast.makeText(this, "Welcome to Markly!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            navigateToMain()
        }

        // Decline button - friendly goodbye message
        btnDecline.setOnClickListener {
            dialog.dismiss()

            AlertDialog.Builder(this)
                .setTitle("Goodbye!")
                .setMessage("We're sorry to see you go.\n\nYou can always come back and accept the terms later to use Markly.")
                .setPositiveButton("OK") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        }

        dialog.show()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}