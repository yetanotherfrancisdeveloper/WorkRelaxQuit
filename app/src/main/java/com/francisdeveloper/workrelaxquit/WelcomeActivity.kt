package com.francisdeveloper.workrelaxquit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if this is the first app launch
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch_splash", true)

        if (isFirstLaunch) {
            // If it's the first launch, show the splash screen for a few seconds
            setContentView(R.layout.welcome_splash)
            val startAppButton = findViewById<Button>(R.id.startAppButton)
            startAppButton.setOnClickListener {
                // Handle the button click action here, e.g., start the main activity
                val mainIntent = Intent(this, MainActivity::class.java)
                startActivity(mainIntent)
                finish()
            }
        } else {
            // If not the first launch, directly navigate to the main activity
            sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
