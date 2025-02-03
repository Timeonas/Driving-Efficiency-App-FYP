package com.example.drivingefficiencyapp.trip

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.example.drivingefficiencyapp.databinding.TripsActivityBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TripsActivity : AppCompatActivity() {
    private lateinit var binding: TripsActivityBinding
    private lateinit var tripAdapter: TripAdapter
    private val tripRepository = TripRepository()
    private var isOfflineSnackbarShowing = false
    private lateinit var connectivityManager: ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            runOnUiThread {
                if (isOfflineSnackbarShowing) {
                    isOfflineSnackbarShowing = false
                    showOnlineSnackbar()
                }
                loadTrips(isOnline = true)
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            runOnUiThread {
                if (!isOfflineSnackbarShowing) {
                    isOfflineSnackbarShowing = true
                    showOfflineSnackbar()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = TripsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupNetworkMonitoring()

        loadTrips(isNetworkAvailable())
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun showOfflineSnackbar() {
        Snackbar.make(
            binding.root,
            "You're offline. Showing cached trips.",
            Snackbar.LENGTH_INDEFINITE
        ).show()
    }

    private fun showOnlineSnackbar() {
        Snackbar.make(
            binding.root,
            "Back online!",
            Snackbar.LENGTH_SHORT
        ).show()
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

        if (!show && tripAdapter.itemCount == 0) {
            binding.noTripsText.visibility = View.VISIBLE
        } else {
            binding.noTripsText.visibility = View.GONE
        }
    }

    private fun loadTrips(isOnline: Boolean) {
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

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}