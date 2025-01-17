package com.example.drivingefficiencyapp

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
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {
    /**
     * Creates the main menu screen and sets up the buttons to start a drive or view past trips.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) //Re-create the saved state
        supportActionBar?.hide() //Hid the action bar which references the app name
        setContentView(R.layout.main_menu_activity) //Set the layout using main_menu_activity.xml layout

        //Set up the buttons to start a drive or view past trips, using the IDs from main_menu_activity.xml
        val startDriveButton = findViewById<Button>(R.id.startDriveButton)
        val viewTripsButton = findViewById<Button>(R.id.viewTripsButton)

        //Set up the button listeners on the buttons to start the StartDriveActivity or TripsActivity
        startDriveButton.setOnClickListener {
            val intent = Intent(this, StartDriveActivity::class.java)
            startActivity(intent)
        }

        viewTripsButton.setOnClickListener {
            val intent = Intent(this, TripsActivity::class.java)
            startActivity(intent)
        }
    }
}