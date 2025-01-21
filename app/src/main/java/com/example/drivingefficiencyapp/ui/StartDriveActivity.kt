package com.example.drivingefficiencyapp.ui

/**
 * The start drive activity for when a user starts a new drive (trip). Right now, it displays
 * the current date and a timer that starts when the user starts the drive. More functionality
 * will be added later. Once the user ends the drive, they will be taken back to the main menu.
 *
 * @author Tim Samoska
 * @since January 17, 2025
 */

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.drivingefficiencyapp.trip.Trip
import com.example.drivingefficiencyapp.databinding.StartDriveActivityBinding
import java.text.SimpleDateFormat
import java.util.*

class StartDriveActivity : AppCompatActivity() {
    private lateinit var binding: StartDriveActivityBinding // View Binding

    //Variable to store the start time of the drive
    private var startTime: Long = 0
    //Handler to post the timerRunnable object
    private val handler = Handler(Looper.getMainLooper())
    //Boolean to check if the timer is running
    private var isRunning = false

    //Runnable object to update the timer text view
    private val timerRunnable = object : Runnable {
        /**
         * Creates a timer that runs every second to update the timer text view with the current
         */
        override fun run() {
            //Parse current time and calculate elapsed time
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - startTime

            //Calculate the seconds, minutes, and hours from the elapsed time
            val seconds = (elapsedTime / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60

            //Format the time string to display in the timer text view
            val timeString = String.format(Locale.getDefault(),"%02d:%02d:%02d",
                hours,
                minutes % 60,
                seconds % 60)

            //Set the time string in the timer text view
            binding.timerTextView.text = timeString

            //If the timer is running, delay the timerRunnable object by 1 second
            // (update the timer once per second)
            if (isRunning) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    /**
     * Creates the start drive screen and sets up the timer to track the drive duration.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)//Re-create the saved state
        supportActionBar?.hide()//Hid the action bar which references the app name

        binding = StartDriveActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)//Set the layout using start_drive_activity.xml layout

        //Set current date in a variable and display it as the text in the dateTextView
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        binding.dateTextView.text = dateFormat.format(Date())

        //Start timer and start the isRunning boolean to true
        startTime = System.currentTimeMillis()
        isRunning = true
        //Post the timerRunnable object to the handler
        handler.post(timerRunnable)

        //Once the user clicks the endDriveButton, the activity is finished and the trip data is saved
        binding.endDriveButton.setOnClickListener {
            //Get current date and elapsed time
            val date = dateFormat.format(Date())

            //Calculate final duration to save to the trip activity screen
            val elapsedTime = System.currentTimeMillis() - startTime
            val seconds = (elapsedTime / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60
            //Format the duration string based on the hours, minutes, and seconds using Kotlin's
            //when expression
            val duration = when {
                //if the hours are greater than 0, format the duration string with hours and
                //minutes, and seconds if applicable
                hours > 0 -> {
                    val mins = minutes % 60
                    if (mins > 0) "$hours hours $mins minutes" else "$hours hours"
                }
                //if hours are 0, just format the minutes
                minutes > 0 -> "$minutes minutes"
                //if minutes are 0, just format the seconds
                else -> "$seconds seconds"
            }

            val trip = Trip(date, duration)
            Trip.tripsList.add(trip)
            finish() //Returns to previous activity (MainMenuActivity)
        }
    }

    /**
     * Stops the timer and sets the isRunning boolean to false when the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false //Set the isRunning boolean to false
        //Remove any pending posts of the timerRunnable object from the handler
        handler.removeCallbacks(timerRunnable)
    }
}