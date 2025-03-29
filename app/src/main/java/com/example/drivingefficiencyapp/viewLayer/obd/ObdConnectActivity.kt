package com.example.drivingefficiencyapp.viewLayer.obd

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.drivingefficiencyapp.databinding.ObdConnectActivityBinding
import com.example.drivingefficiencyapp.modelLayer.obd.ObdConnectionManager
import com.example.drivingefficiencyapp.modelLayer.obd.ObdDataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ObdConnectActivity : AppCompatActivity() {
    private lateinit var binding: ObdConnectActivityBinding
    private var dataCollectionJob: Job? = null
    private var obdDataReader: ObdDataReader? = null

    private val TAG = "ObdConnectActivity"

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) {
            Toast.makeText(this, "Bluetooth permissions are required.", Toast.LENGTH_LONG).show()
            binding.statusText.text = "Bluetooth permissions required."
        } else {
            // Permissions granted, check connection
            updateConnectionStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ObdConnectActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        binding.connectButton.setOnClickListener {
            connectToObd()
        }

        binding.disconnectButton.setOnClickListener {
            disconnectObd()
        }
    }

    private fun updateConnectionStatus() {
        val isConnected = ObdConnectionManager.connectionState.value
        binding.connectionStatus.setImageResource(
            if (isConnected) android.R.drawable.presence_online
            else android.R.drawable.presence_offline
        )

        if (isConnected) {
            binding.statusText.text = "Connected to OBD"
            initializeAndStartDataCollection()
        } else {
            binding.statusText.text = "Not connected to OBD"
            resetDataDisplays()
        }

        updateButtonStates()
    }

    private fun initializeAndStartDataCollection() {
        // If already reading data, don't restart
        if (dataCollectionJob?.isActive == true) return

        lifecycleScope.launch {
            binding.connectionProgress.visibility = View.VISIBLE

            ObdConnectionManager.initializeObd { success ->
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.connectionProgress.visibility = View.GONE

                    if (success) {
                        startDataCollection()
                    } else {
                        binding.statusText.text = "Failed to initialize OBD"
                    }
                }
            }
        }
    }

    private fun startDataCollection() {
        // First get a data reader from the connection manager
        obdDataReader = ObdConnectionManager.startContinuousReading(lifecycleScope)

        // Then collect data from it
        dataCollectionJob = lifecycleScope.launch {
            obdDataReader?.obdData?.collect { data ->
                withContext(Dispatchers.Main) {
                    binding.rpmText.text = data.rpm
                    binding.speedText.text = data.speed
                    binding.tempText.text = data.temperature
                    binding.gearText.text = data.gear
                    binding.fuelRateText.text = data.instantFuelRate
                    binding.avgFuelConsText.text = data.averageFuelConsumption
                    binding.avgSpeedText.text = data.averageSpeed
                    binding.distanceText.text = data.distanceTraveled
                    binding.fuelUsedText.text = data.fuelUsed
                }
            }
        }
    }

    private fun resetDataDisplays() {
        binding.rpmText.text = "- RPM"
        binding.speedText.text = "- km/h"
        binding.tempText.text = "- °C"
        binding.gearText.text = "- Gear"
        binding.fuelRateText.text = "- L/h"
        binding.avgFuelConsText.text = "- L/100km"
        binding.avgSpeedText.text = "- km/h"
        binding.distanceText.text = "- km"
        binding.fuelUsedText.text = "- L"
    }

    private fun connectToObd() {
        if (!hasRequiredPermissions()) {
            checkPermissions()
            return
        }

        binding.connectionProgress.visibility = View.VISIBLE
        binding.statusText.text = "Connecting..."
        updateButtonStates(isConnecting = true)

        lifecycleScope.launch {
            ObdConnectionManager.connect(this@ObdConnectActivity, lifecycleScope)
                .invokeOnCompletion {
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.connectionProgress.visibility = View.GONE
                        updateConnectionStatus()
                    }
                }
        }
    }

    private fun disconnectObd() {
        binding.statusText.text = "Disconnecting..."

        lifecycleScope.launch {
            dataCollectionJob?.cancel()
            dataCollectionJob = null

            ObdConnectionManager.disconnect(lifecycleScope)
                .invokeOnCompletion {
                    lifecycleScope.launch(Dispatchers.Main) {
                        updateConnectionStatus()
                    }
                }
        }
    }

    private fun updateButtonStates(isConnecting: Boolean = false) {
        val isConnected = ObdConnectionManager.connectionState.value

        binding.connectButton.isEnabled = !isConnected && !isConnecting
        binding.disconnectButton.isEnabled = isConnected

        binding.connectButton.alpha = if (binding.connectButton.isEnabled) 1.0f else 0.5f
        binding.disconnectButton.alpha = if (binding.disconnectButton.isEnabled) 1.0f else 0.5f
    }

    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onResume() {
        super.onResume()
        updateConnectionStatus()
    }

    override fun onPause() {
        super.onPause()
        dataCollectionJob?.cancel()
        dataCollectionJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        dataCollectionJob?.cancel()
        //don't disconnect when the activity is destroyed to maintain connection
    }
}