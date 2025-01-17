package com.example.drivingefficiencyapp

/**
 * Activity that displays a list of all recorded driving trips.
 *
 * This activity uses a RecyclerView to display trip data in a scrollable list format.
 * Currently using sample data, but will be updated to use actual trip records.
 *
 * @author Tim Samoska
 * @since January 17, 2025
 */

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*

class TripsActivity : AppCompatActivity() {
    //Views that need to be accessed throughout the activity lifecycle
    private lateinit var recyclerView: RecyclerView
    private lateinit var tripAdapter: TripAdapter

    /**
     * Initializes the activity, sets up the RecyclerView, and loads trip data.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) //Re-create the saved state
        supportActionBar?.hide() //Hid the action bar which references the app name
        setContentView(R.layout.trips_activity) //Set the layout using trips_activity.xml layout

        //Set up the RecyclerView to display the list of trips
        recyclerView = findViewById(R.id.tripsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        //Sample data, will replace with actual trip data later
        val sampleTrips = listOf(
            Trip("January 17, 2025", "45 minutes"),
            Trip("January 16, 2025", "30 minutes"),
            Trip("January 15, 2025", "1 hour 15 minutes")
        )

        //Create and set the adapter for the RecyclerView
        tripAdapter = TripAdapter(sampleTrips)
        recyclerView.adapter = tripAdapter
    }
}