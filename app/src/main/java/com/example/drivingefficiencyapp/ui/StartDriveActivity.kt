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
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.databinding.StartDriveActivityBinding
import com.example.drivingefficiencyapp.location.LocationService
import com.example.drivingefficiencyapp.trip.TripRepository
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
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
    private val tripRepository = TripRepository()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val UPDATE_INTERVAL = 5000L //5 seconds
        private const val FASTEST_INTERVAL = 3000L //3 seconds
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002
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
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = StartDriveActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!checkGooglePlayServices()) {
            Toast.makeText(this, "Google Play Services required", Toast.LENGTH_LONG).show()
            return
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupLocationCallback()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupDateAndTimer()
        if (hasLocationPermission()) {
            startLocationService()
            checkAndRequestPermissions()
        } else {
            requestLocationPermission()
        }

        setupEndDriveButton()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAndRequestPermissions() {
        // Check location permission
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission()
                return
            }
        }

        // If all permissions granted, start location updates and service
        startLocationService()
        startLocationUpdates()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun checkGooglePlayServices(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("LocationDebug", "Google Play Services not available: $resultCode")
            if (availability.isUserResolvableError(resultCode)) {
                availability.getErrorDialog(this, resultCode, 9000)?.show()
            }
            return false
        }

        Log.d("LocationDebug", "Google Play Services available")
        return true
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d("LocationDebug", "Location update received")
                locationResult.lastLocation?.let { location ->
                    Log.d("LocationDebug", "New location: ${location.latitude}, ${location.longitude}")
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    if (googleMap == null) {
                        Log.d("LocationDebug", "Map not ready, storing location")
                        pendingLocation = currentLatLng
                    } else {
                        Log.d("LocationDebug", "Updating camera position")
                        val cameraPosition = CameraPosition.Builder()
                            .target(currentLatLng)
                            .zoom(18f)
                            .tilt(60f)
                            .bearing(currentBearing)
                            .build()

                        googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    }
                } ?: Log.e("LocationDebug", "Location in update was null")
            }
        }
    }

    private fun setupDateAndTimer() {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        binding.dateTextView.text = dateFormat.format(Date())

        startTime = System.currentTimeMillis()
        isRunning = true
        handler.post(timerRunnable)
    }

    private fun setupEndDriveButton() {
        binding.endDriveButton.setOnClickListener {
            val date = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
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

            lifecycleScope.launch {
                try {
                    tripRepository.saveTrip(date, duration)
                        .onSuccess {
                            Toast.makeText(
                                this@StartDriveActivity,
                                "Trip saved successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .onFailure { exception ->
                            Toast.makeText(
                                this@StartDriveActivity,
                                "Failed to save trip: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@StartDriveActivity,
                        "Error saving trip: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            stopLocationService()
            stopLocationUpdates()
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocationService() {
        Log.d("LocationDebug", "Starting location service")
        val serviceIntent = Intent(this, LocationService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun stopLocationService() {
        Log.d("LocationDebug", "Stopping location service")
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }

    private fun startLocationUpdates() {
        Log.d("LocationDebug", "Attempting to start location updates")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
                    .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                    .build()

                Log.d("LocationDebug", "Location request built successfully")

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        Log.d("LocationDebug", "Last location: ${location?.latitude}, ${location?.longitude}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("LocationDebug", "Failed to get last location: ${e.message}")
                    }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                ).addOnSuccessListener {
                    Log.d("LocationDebug", "Location updates successfully requested")
                }.addOnFailureListener { e ->
                    Log.e("LocationDebug", "Failed to request location updates: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("LocationDebug", "Exception starting location updates: ${e.message}", e)
            }
        } else {
            Log.e("LocationDebug", "Location permission not granted")
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
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAndRequestPermissions()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission is required for tracking your drive",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAndRequestPermissions()
                } else {
                    Toast.makeText(
                        this,
                        "Notification permission is required for tracking in background",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
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