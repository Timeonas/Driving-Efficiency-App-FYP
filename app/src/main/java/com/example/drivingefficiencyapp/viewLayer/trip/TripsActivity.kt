package com.example.drivingefficiencyapp.viewLayer.trip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drivingefficiencyapp.modelLayer.obd.EfficiencyCalculator
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.modelLayer.trip.Trip
import com.example.drivingefficiencyapp.viewModel.TripViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TripsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var noTripsText: TextView
    private lateinit var adapter: TripAdapter

    private var viewModel = TripViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.trips_activity)

        setupViews()
        setupViewModel()
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

        // Only calculate efficiency scores for trips that don't have one
        trips.forEach { trip ->
            if (trip.efficiencyScore == -1) {
                trip.efficiencyScore = EfficiencyCalculator.calculateEfficiencyScore(trip)

                // Update the efficiency score in Firestore
                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    .collection("trips")
                    .document(trip.id)
                    .update("efficiencyScore", trip.efficiencyScore)
            }
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
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteTrip(trip.id) }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[TripViewModel::class.java]

        // Observe trips
        viewModel.trips.observe(this) { trips ->
            if (trips.isEmpty()) {
                showEmptyState()
            } else {
                showTrips(trips)
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        // Observe error state
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        // Load trips
        viewModel.loadTrips()
    }
}