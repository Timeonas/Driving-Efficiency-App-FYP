package com.example.drivingefficiencyapp.viewLayer.ui

/**
 * Intro screen activity for the Driving Efficiency App.
 *
 * This activity displays an intro screen with "app branding (In Progress)" for 2 seconds
 * before transitioning to the main menu.
 *
 * @author Tim Samoska
 * @since January 17, 2025
 */

import android.content.Intent
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import com.example.drivingefficiencyapp.viewLayer.auth.LoginActivity
import com.example.drivingefficiencyapp.databinding.AppIntroActivityBinding
import com.google.firebase.auth.FirebaseAuth

class AppIntroActivity : AppCompatActivity() {
    private lateinit var binding: AppIntroActivityBinding // View Binding
    /**
     * Creates the splash screen and sets up transition to main menu.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) //Re-create the saved state
        supportActionBar?.hide() //Hid the action bar which references the app name

        binding = AppIntroActivityBinding.inflate(layoutInflater)

        setContentView(binding.root) //Set the layout using app_intro_activity.xml view binding

        //Delay for 2 seconds before starting the MainMenuActivity or login screen
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is signed in
            val user = FirebaseAuth.getInstance().currentUser
            val intent = if (user != null) {
                Intent(this, MainMenuActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 2000)
    }
}