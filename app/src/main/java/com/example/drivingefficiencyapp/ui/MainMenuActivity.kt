package com.example.drivingefficiencyapp.ui

/**
 * Main Menu activity for the Driving Efficiency App.
 *
 * This activity displays the main menu activity for the app. Right now, it has two buttons:
 * one to start a drive and one to view past trips. More functionality will be added later.
 *
 * @author Tim Samoska
 * @since January 17, 2025
 */

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.drivingefficiencyapp.ObdConnectActivity
import com.example.drivingefficiencyapp.databinding.MainMenuActivityBinding
import com.example.drivingefficiencyapp.profile.ProfileActivity
import com.example.drivingefficiencyapp.trip.TripsActivity

class MainMenuActivity : AppCompatActivity() {
    private lateinit var binding: MainMenuActivityBinding //View Binding
    /**
     * Creates the main menu screen and sets up the buttons to start a drive or view past trips.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) //Re-create the saved state
        supportActionBar?.hide() //Hid the action bar which references the app name

        binding = MainMenuActivityBinding.inflate(layoutInflater)

        setContentView(binding.root) //Set the layout using main_menu_activity.xml layout

        //Set up the button listeners on the buttons to start the StartDriveActivity or TripsActivity
        binding.startDriveButton.setOnClickListener {
            val intent = Intent(this, StartDriveActivity::class.java)
            startActivity(intent)
        }

        binding.viewTripsButton.setOnClickListener {
            val intent = Intent(this, TripsActivity::class.java)
            startActivity(intent)
        }
        //Button to navigate to the profile page activity
        binding.profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        //Temp button to bring the user to OBD connectivity testing
        binding.obdButton.setOnClickListener {
            val intent = Intent(this, ObdConnectActivity::class.java)
            startActivity(intent)
        }
    }
}