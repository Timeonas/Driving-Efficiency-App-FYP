package com.example.drivingefficiencyapp.ui

/**
 * The start drive activity for when a user starts a new drive (trip). It displays
 * the current date, a timer that starts when the user starts the drive, and a map showing
 * the user's current location. Once the user ends the drive, they will be taken back
 * to the main menu.
 *
 * @author Tim Samoska
 * @since January 17, 2025
 */

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.databinding.StartDriveActivityBinding
import com.example.drivingefficiencyapp.trip.Trip
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*

class StartDriveActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: StartDriveActivityBinding // View Binding
    private lateinit var fusedLocationClient: FusedLocationProviderClient // Location services
    private lateinit var locationCallback: LocationCallback // Callback for location updates
    private lateinit var googleMap: GoogleMap // Google Maps instance

    // Variables to store the start time and manage the timer
    private var startTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    companion object {
        // Constants for location permissions and update intervals
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val UPDATE_INTERVAL = 5000L // 5 seconds
        private const val FASTEST_INTERVAL = 3000L // 3 seconds
    }

    /**
     * Runnable object to update the timer text view every second with the current
     * elapsed time of the drive.
     */
    private val timerRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - startTime

            val seconds = (elapsedTime / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60

            val timeString = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                hours,
                minutes % 60,
                seconds % 60
            )

            binding.timerTextView.text = timeString

            if (isRunning) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    /**
     * Creates the start drive screen, sets up the timer to track drive duration,
     * initializes the map, and starts location tracking.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = StartDriveActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set current date in date text view
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        binding.dateTextView.text = dateFormat.format(Date())

        // Start timer for tracking drive duration
        startTime = System.currentTimeMillis()
        isRunning = true
        handler.post(timerRunnable)

        // Set up callback for receiving location updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                }
            }
        }

        // Handle end drive button click
        binding.endDriveButton.setOnClickListener {
            val date = dateFormat.format(Date())

            val elapsedTime = System.currentTimeMillis() - startTime
            val seconds = (elapsedTime / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60

            val duration = when {
                hours > 0 -> {
                    val mins = minutes % 60
                    if (mins > 0) "$hours hours $mins minutes" else "$hours hours"
                }
                minutes > 0 -> "$minutes minutes"
                else -> "$seconds seconds"
            }

            val trip = Trip(date, duration)
            Trip.tripsList.add(trip)

            stopLocationUpdates()
            finish()
        }

        // Check and request location permissions if needed
        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    /**
     * Callback for when the Google Map is ready to be used.
     * Enables the user's location on the map if permission is granted.
     *
     * @param map The Google Map instance that is ready for use
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings.isMyLocationButtonEnabled = true
            }
        }
    }

    /**
     * Checks if the app has been granted location permission.
     *
     * @return Boolean indicating if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests location permission from the user.
     */
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Handles the result of the permission request.
     *
     * @param requestCode The code that was used to request the permission
     * @param permissions The requested permissions
     * @param grantResults The grant results for the corresponding permissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission is required for tracking your drive",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Starts receiving location updates if permission is granted.
     * Updates occur every UPDATE_INTERVAL milliseconds.
     */
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = UPDATE_INTERVAL
                fastestInterval = FASTEST_INTERVAL
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    /**
     * Stops receiving location updates.
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Cleans up resources when the activity is destroyed.
     * Stops the timer and location updates.
     */
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        stopLocationUpdates()
    }
}
