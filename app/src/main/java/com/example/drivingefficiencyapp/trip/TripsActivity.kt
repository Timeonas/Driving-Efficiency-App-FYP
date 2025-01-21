package com.example.drivingefficiencyapp.trip

/**
 * Activity that displays a list of all recorded driving trips.
 *
 * This activity uses a RecyclerView to display trip data in a scrollable list format.
 *
 * @author Tim Samoska
 * @since January 17, 2025
 */

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.example.drivingefficiencyapp.databinding.TripsActivityBinding

class TripsActivity : AppCompatActivity() {
    //Views that need to be accessed throughout the activity lifecycle
    private lateinit var recyclerView: RecyclerView
    private lateinit var tripAdapter: TripAdapter

    private lateinit var binding: TripsActivityBinding // View Binding

    /**
     * Initializes the activity, sets up the RecyclerView, and loads trip data.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) //Re-create the saved state
        supportActionBar?.hide() //Hid the action bar which references the app name

        binding = TripsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root) //Set the layout using trips_activity.xml view binding

        //Set up the RecyclerView to display the list of trips
        recyclerView = binding.tripsRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        //Create and set the adapter for the RecyclerView, based on the list of trips saved in memory
        tripAdapter = TripAdapter(Trip.tripsList)
        recyclerView.adapter = tripAdapter
    }
}