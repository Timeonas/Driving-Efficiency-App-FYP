package com.example.drivingefficiencyapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drivingefficiencyapp.EfficiencyCalculator
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.trip.Trip
import com.example.drivingefficiencyapp.trip.TripAdapter
import com.example.drivingefficiencyapp.trip.TripRepository
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch

class TripsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var noTripsText: TextView
    private lateinit var adapter: TripAdapter
    private val tripRepository = TripRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.trips_activity)

        setupViews()
        loadTrips()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.tripsRecyclerView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        noTripsText = findViewById(R.id.noTripsText)

        adapter = TripAdapter(
            onDeleteClicked = { trip -> confirmDeleteTrip(trip) },
            onTripClicked = { trip -> showTripDetails(trip) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadTrips() {
        showLoading(true)

        lifecycleScope.launch {
            tripRepository.getTrips()
                .onSuccess { trips ->
                    if (trips.isEmpty()) {
                        showEmptyState()
                    } else {
                        showTrips(trips)
                    }
                }
                .onFailure {
                    showError("Failed to load trips")
                }

            showLoading(false)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            recyclerView.visibility = View.GONE
            noTripsText.visibility = View.GONE
        }
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        noTripsText.visibility = View.VISIBLE
    }

    private fun showTrips(trips: List<Trip>) {
        recyclerView.visibility = View.VISIBLE
        noTripsText.visibility = View.GONE

        // Calculate efficiency scores for each trip
        trips.forEach { trip ->
            trip.efficiencyScore = EfficiencyCalculator.calculateEfficiencyScore(trip)
        }
        adapter.updateTrips(trips)
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmDeleteTrip(trip: Trip) {
        AlertDialog.Builder(this)
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete this trip?")
            .setPositiveButton("Delete") { _, _ -> deleteTrip(trip) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTrip(trip: Trip) {
        lifecycleScope.launch {
            showLoading(true)

            tripRepository.deleteTrip(trip.id)
                .onSuccess {
                    val position = adapter.trips.indexOf(trip)
                    adapter.removeTrip(position)

                    if (adapter.itemCount == 0) {
                        showEmptyState()
                    }
                }
                .onFailure {
                    showError("Failed to delete trip")
                }

            showLoading(false)
        }
    }

    private fun showTripDetails(trip: Trip) {
        val intent = Intent(this, TripDetailActivity::class.java).apply {
            putExtra("date", trip.date)
            putExtra("duration", trip.duration)
            putExtra("averageSpeed", trip.averageSpeed)
            putExtra("distanceTraveled", trip.distanceTraveled)
            putExtra("averageFuelConsumption", trip.averageFuelConsumption)
            putExtra("fuelUsed", trip.fuelUsed)
            putExtra("maxRPM", trip.maxRPM)
            putExtra("avgRPM", trip.avgRPM)
            putExtra("efficiencyScore", trip.efficiencyScore)
        }
        startActivity(intent)
    }
}