package com.example.drivingefficiencyapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.drivingefficiencyapp.LocationService
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.databinding.StartDriveActivityBinding
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class StartDriveActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
    private lateinit var binding: StartDriveActivityBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sensorManager: SensorManager
    private var googleMap: GoogleMap? = null
    private var pendingLocation: LatLng? = null
    private var currentBearing: Float = 0f
    private var startTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private val tripRepository = TripRepository()
    private var accelerometerValues = FloatArray(3)
    private var magnetometerValues = FloatArray(3)
    private var hasAccelerometerData = false
    private var hasMagnetometerData = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002
        private const val UPDATE_INTERVAL = 5000L
        private const val FASTEST_INTERVAL = 3000L
        private const val TAG = "StartDriveActivity"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBasicUI()

        if (!checkGooglePlayServices()) return

        initializeComponents()
        checkAndRequestPermissions()
    }

    private fun setupBasicUI() {
        supportActionBar?.hide()
        binding = StartDriveActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initializeComponents() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        setupMapFragment()
        setupDateAndTimer()
        setupEndDriveButton()
    }

    private fun setupMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateMapLocation(LatLng(location.latitude, location.longitude))
                }
            }
        }
    }

    private fun updateMapLocation(location: LatLng) {
        if (googleMap == null) {
            pendingLocation = location
            return
        }

        val cameraPosition = CameraPosition.Builder()
            .target(location)
            .zoom(18f)
            .tilt(60f)
            .bearing(currentBearing)
            .build()

        googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun setupDateAndTimer() {
        binding.dateTextView.text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            .format(Date())
        startTime = System.currentTimeMillis()
        isRunning = true
        startTimer()
    }

    private fun startTimer() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isRunning) return

                val elapsedTime = System.currentTimeMillis() - startTime
                val seconds = (elapsedTime / 1000).toInt()
                val minutes = seconds / 60
                val hours = minutes / 60

                binding.timerTextView.text = String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d",
                    hours,
                    minutes % 60,
                    seconds % 60
                )

                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun setupEndDriveButton() {
        binding.endDriveButton.setOnClickListener {
            saveTrip()
            cleanup()
            finish()
        }
    }

    private fun saveTrip() {
        val date = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        val duration = calculateDuration()

        // Use runBlocking to ensure save completes before activity destruction
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    tripRepository.saveTrip(date, duration)
                        .onSuccess {
                            withContext(Dispatchers.Main) {
                                showToast("Trip saved successfully")
                                cleanup()
                                finish()
                            }
                        }
                        .onFailure { e ->
                            withContext(Dispatchers.Main) {
                                showToast("Failed to save trip: ${e.message}")
                            }
                        }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error saving trip: ${e.message}")
                }
            }
        }
    }

    private fun calculateDuration(): String {
        val elapsedTime = System.currentTimeMillis() - startTime
        val seconds = (elapsedTime / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> {
                val remainingMinutes = minutes % 60
                if (remainingMinutes > 0) "$hours hours $remainingMinutes minutes"
                else "$hours hours"
            }
            minutes > 0 -> "$minutes minutes"
            else -> "$seconds seconds"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAndRequestPermissions() {
        when {
            !hasLocationPermission() -> requestLocationPermission()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !hasNotificationPermission() -> requestNotificationPermission()
            else -> {
                startLocationService()
                startLocationUpdates()
            }
        }
    }

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasNotificationPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
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

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Error starting location updates", e)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setupMapSettings()
        if (hasLocationPermission()) {
            enableLocationFeatures()
        }
    }

    private fun setupMapSettings() {
        googleMap?.apply {
            uiSettings.apply {
                isTiltGesturesEnabled = true
                isMyLocationButtonEnabled = true
            }
        }
    }

    private fun enableLocationFeatures() {
        try {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    updateMapLocation(LatLng(it.latitude, it.longitude))
                }
            }
            pendingLocation?.let { location ->
                updateMapLocation(location)
                pendingLocation = null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error enabling location features", e)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerValues, 0, 3)
                hasAccelerometerData = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerValues, 0, 3)
                hasMagnetometerData = true
            }
        }

        if (hasAccelerometerData && hasMagnetometerData) {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magnetometerValues)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                currentBearing = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                if (currentBearing < 0) {
                    currentBearing += 360f
                }
                pendingLocation?.let { updateMapLocation(it) }
            }
        }
    }

    private fun cleanup() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        stopLocationUpdates()
        stopLocationService()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkAndRequestPermissions()
        } else {
            val message = when (requestCode) {
                LOCATION_PERMISSION_REQUEST_CODE ->
                    "Location permission is required for tracking your drive"
                NOTIFICATION_PERMISSION_REQUEST_CODE ->
                    "Notification permission is required for tracking in background"
                else -> "Required permission was denied"
            }
            showToast(message)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun checkGooglePlayServices(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(resultCode)) {
                availability.getErrorDialog(this, resultCode, 9000)?.show()
            }
            showToast("Google Play Services required")
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocationService() {
        startForegroundService(Intent(this, LocationService::class.java))
    }

    private fun stopLocationService() {
        stopService(Intent(this, LocationService::class.java))
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}