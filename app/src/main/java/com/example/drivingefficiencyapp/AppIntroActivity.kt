package com.example.drivingefficiencyapp

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

class AppIntroActivity : AppCompatActivity() {
    /**
     * Creates the splash screen and sets up transition to main menu.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) //Re-create the saved state
        supportActionBar?.hide() //Hid the action bar which references the app name
        setContentView(R.layout.app_intro_activity) //Set the layout using app_intro.xml layout

        //Delay for 2 seconds before starting the MainMenuActivity and finishing the current activity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }, 2000)
    }
}