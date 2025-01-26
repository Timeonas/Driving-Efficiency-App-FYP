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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*

class StartDriveActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
    private lateinit var binding: StartDriveActivityBinding //View Binding
    private lateinit var fusedLocationClient: FusedLocationProviderClient //Location services
    private lateinit var locationCallback: LocationCallback //Callback for location updates
    private lateinit var sensorManager: SensorManager //Device sensor manager
    private var googleMap: GoogleMap? = null //Google Maps instance
    private var pendingLocation: LatLng? = null //Stored location when map isn't ready
    private var currentBearing: Float = 0f //Current device orientation
    private var startTime: Long = 0 //Trip start time
    private val handler = Handler(Looper.getMainLooper()) //Handler for timer updates
    private var isRunning = false //Timer state

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val UPDATE_INTERVAL = 5000L //5 seconds
        private const val FASTEST_INTERVAL = 3000L //3 seconds
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

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    if (googleMap == null) {
                        pendingLocation = currentLatLng
                    } else {
                        val cameraPosition = CameraPosition.Builder()
                            .target(currentLatLng)
                            .zoom(18f)
                            .tilt(60f)
                            .bearing(currentBearing)
                            .build()

                        googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    }
                }
            }
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        binding.dateTextView.text = dateFormat.format(Date())

        startTime = System.currentTimeMillis()
        isRunning = true
        handler.post(timerRunnable)

        binding.endDriveButton.setOnClickListener {
            val date = dateFormat.format(Date())

            val elapsedTime = System.currentTimeMillis() - startTime
            val seconds = (elapsedTime / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60

            val duration = when {
                hours > 0 -> {
                    val tempMinutes = minutes % 60
                    if (tempMinutes > 0) "$hours hours $tempMinutes minutes" else "$hours hours"
                }
                minutes > 0 -> "$minutes minutes"
                else -> "$seconds seconds"
            }

            val trip = Trip(date, duration)
            Trip.tripsList.add(trip)

            stopLocationUpdates()
            finish()
        }

        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    /**
     * Callback for when the Google Map is ready.
     * Sets up map UI and initial camera position.
     *
     * @param map The Google Map instance that is ready for use
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isTiltGesturesEnabled = true
        if (hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap?.isMyLocationEnabled = true
                googleMap?.uiSettings?.isMyLocationButtonEnabled = true
                googleMap?.uiSettings?.isTiltGesturesEnabled = true

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        val initialPosition = CameraPosition.Builder()
                            .target(currentLatLng)
                            .zoom(18f)
                            .tilt(60f)
                            .bearing(currentBearing)
                            .build()

                        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(initialPosition))
                    }
                }

                pendingLocation?.let { location ->
                    val cameraPosition = CameraPosition.Builder()
                        .target(location)
                        .zoom(18f)
                        .tilt(60f)
                        .bearing(currentBearing)
                        .build()

                    googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    pendingLocation = null
                }
            }
        }
    }

    /**
     * Registers sensor listeners when activity resumes.
     */
    override fun onResume() {
        super.onResume()
        // Register the sensor listener when the activity resumes
        sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)?.let { orientationSensor ->
            sensorManager.registerListener(
                this,
                orientationSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    /**
     * Unregisters sensor listeners when activity pauses.
     */
    override fun onPause() {
        super.onPause()
        // Unregister the sensor listener when the activity is paused
        sensorManager.unregisterListener(this)
    }

    /**
     * Handles sensor value changes, particularly device orientation.
     * Updates the map camera bearing to match device orientation.
     *
     * @param event The sensor event containing new sensor values
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            currentBearing = event.values[0] // Azimuth value (bearing)

            // Update camera bearing if map and current location are available
            googleMap?.let { map ->
                pendingLocation?.let { location ->
                    val cameraPosition = CameraPosition.Builder()
                        .target(location)
                        .zoom(18f)
                        .tilt(60f)
                        .bearing(currentBearing)
                        .build()

                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
            }
        }
    }

    /**
     * Handles changes in sensor accuracy.
     *
     * @param sensor The sensor whose accuracy changed
     * @param accuracy The new accuracy value
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    /**
     * Checks if the app has location permission.
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
     * Handles the result of permission requests.
     *
     * @param requestCode The code used to request the permission
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
     * Starts location updates if permission is granted.
     * Updates occur according to UPDATE_INTERVAL and FASTEST_INTERVAL constants.
     */
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    /**
     * Stops location updates.
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Cleans up resources when the activity is destroyed.
     * Stops the timer, location updates, and sensor listeners.
     */
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        stopLocationUpdates()
        sensorManager.unregisterListener(this)
    }
}