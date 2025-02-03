package com.example.drivingefficiencyapp.trip

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.example.drivingefficiencyapp.databinding.TripsActivityBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TripsActivity : AppCompatActivity() {
    private lateinit var binding: TripsActivityBinding
    private lateinit var tripAdapter: TripAdapter
    private val tripRepository = TripRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = TripsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadTrips()
    }

    private fun setupRecyclerView() {
        tripAdapter = TripAdapter(emptyList())
        binding.tripsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TripsActivity)
            adapter = tripAdapter
            visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        binding.tripsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE

        //Show "No trips found" message if there are no trips and not loading
        if (!show && tripAdapter.itemCount == 0) {
            binding.noTripsText.visibility = View.VISIBLE
        } else {
            binding.noTripsText.visibility = View.GONE
        }
    }

    private fun loadTrips() {
        showLoading(true)

        lifecycleScope.launch {
            tripRepository.getTripsFlow()
                .catch { exception ->
                    showLoading(false)
                    Toast.makeText(
                        this@TripsActivity,
                        "Error loading trips: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .collect { trips ->
                    tripAdapter = TripAdapter(trips)
                    binding.tripsRecyclerView.adapter = tripAdapter
                    showLoading(false)
                }
        }
    }
}