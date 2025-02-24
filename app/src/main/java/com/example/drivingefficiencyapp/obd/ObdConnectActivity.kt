package com.example.drivingefficiencyapp.obd

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.drivingefficiencyapp.databinding.ObdConnectActivityBinding
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ObdConnectActivity : AppCompatActivity() {

    private lateinit var binding: ObdConnectActivityBinding
    private var obdSocket: BluetoothSocket? = null
    private var readingJob: Job? = null
    private var obdDataReader: ObdDataReader? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

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

    private val OBD_MAC_ADDRESS = "66:1E:32:30:AF:15" // Replace with YOUR OBD-II adapter's MAC address.
    private val OBD_NAME = "OBDII" // Replace with your OBD2 device name.

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) {
            Toast.makeText(this, "Bluetooth permissions are required.", Toast.LENGTH_LONG).show()
            binding.statusText.text = "Bluetooth permissions required."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ObdConnectActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()

        binding.statusText.text = "Ensure OBD2 is paired and press connect"
        binding.connectionStatus.setImageResource(android.R.drawable.presence_offline)
        updateButtonStates()
    }

    private fun setupUI() {
        binding.connectButton.setOnClickListener {
            findAndConnectToObd()
        }

        binding.disconnectButton.setOnClickListener {
            disconnectObd()
        }
    }

    private fun findAndConnectToObd() {
        if (!hasRequiredPermissions()) {
            checkPermissions()
            return
        }

        lifecycleScope.launch {
            try {
                val pairedDevices = bluetoothAdapter?.bondedDevices
                val obdDevice = pairedDevices?.find { it.address == OBD_MAC_ADDRESS && it.name == OBD_NAME}

                if (obdDevice == null) {
                    withContext(Dispatchers.Main) {
                        binding.statusText.text = "Failed to connect.  Ensure the OBD2 device is paired."
                    }
                    return@launch
                }

                startConnection(obdDevice)
            } catch (e: SecurityException) {
                // Handle the case where you don't have permission to access bonded devices.
                withContext(Dispatchers.Main) {
                    binding.statusText.text = "Failed to access Bluetooth devices"
                }
            }
        }
    }


    private fun startConnection(device: BluetoothDevice) {
        if (!hasRequiredPermissions()) {
            Log.e("ObdConnectActivity", "Bluetooth Connect permission missing in connectToDevice")
            return
        }

        readingJob?.cancel() // Cancel any existing reading job.
        try {
            obdSocket?.close() // Close any existing socket.
        } catch (e: IOException) {
            Log.e("ObdConnectActivity", "Error closing existing socket: ${e.message}")
        }
        obdSocket = null // Reset the socket.

        readingJob = lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                updateButtonStates(isConnecting = true)
                binding.statusText.text = "Connecting..."
            }

            try {
                // Use a timeout to prevent indefinite blocking.
                val tempSocket = withTimeoutOrNull(10000) {
                    withContext(Dispatchers.IO) {
                        if (ContextCompat.checkSelfPermission(
                                this@ObdConnectActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // Handle missing permission (shouldn't happen if we checked earlier).
                            throw SecurityException("Bluetooth connect permission not granted")
                        }
                        // Attempt to create and connect the socket.
                        try {
                            device.createRfcommSocketToServiceRecord(
                                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard OBD-II UUID.
                            ).apply {
                                connect() // This is a blocking call.
                            }
                        } catch (e: SecurityException) {
                            Log.e("ObdConnectActivity", "Security exception creating socket: ${e.message}")
                            throw e // Re-throw the exception to be caught by the outer try-catch.
                        }
                    }
                }
                obdSocket = tempSocket  // Assign the socket if successful.

                obdSocket?.let { socket ->
                    initializeObd(socket)
                    startContinuousReading(socket.outputStream, socket.inputStream)
                    withContext(Dispatchers.Main) {
                        binding.connectionProgress.visibility = View.GONE
                        binding.connectionStatus.setImageResource(android.R.drawable.presence_online)
                        binding.statusText.text = "Connected"
                        updateButtonStates() // Update button states after successful connection.
                    }
                }
            } catch (e: Exception) {
                // Handle connection failures (timeout, IOException, etc.).
                withContext(Dispatchers.Main) {
                    binding.statusText.text = "Connection failed: ${e.message}"
                    binding.connectionProgress.visibility = View.GONE
                    binding.connectionStatus.setImageResource(android.R.drawable.presence_busy)
                    updateButtonStates() // Update button states after connection failure.
                    //Automatically Reset on Connection Loss
                    obdDataReader?.resetTripData()
                }
                Log.e("ObdConnectActivity", "Connection failed: ${e.message}", e)
            }
        }
    }


    private suspend fun initializeObd(socket: BluetoothSocket) {
        withContext(Dispatchers.IO) {
            val outputStream = socket.outputStream
            val inputStream = socket.inputStream
            val buffer = ByteArray(1024) // Buffer for reading responses.

            // Initialization commands (as strings).
            val initCommands = listOf(
                "ATZ",      // Reset ELM327.
                "ATE0",     // Echo off.
                "ATL0",     // Linefeeds off.
                "ATH0",     // Headers off.
                "ATSP0",    // Set Protocol to Automatic.
                "ATDP",     // Display Protocol (optional, for debugging).
            )

            delay(500) // Initial delay before sending commands.

            // Send initialization commands and check responses.
            for (command in initCommands) {
                try {
                    outputStream.write((command + "\r").toByteArray())
                    delay(300) // Wait for the command to be processed.
                    val bytes = inputStream.read(buffer) // Read the response.
                    val response = String(buffer, 0, bytes).trim()
                    Log.d("ObdConnectActivity", "OBD Init: $command -> $response")

                    // Basic response validation (adjust based on your OBD-II adapter's responses).
                    if (!response.contains("OK") && !response.uppercase().contains("ELM") && !response.uppercase().contains("AUTO")) {
                        withContext(Dispatchers.Main) {
                            binding.statusText.text = "Init failed: $command -> $response"
                        }
                        throw IOException("OBD init failed for command: $command")
                    }
                } catch (e: IOException) {
                    Log.e("ObdConnectActivity", "OBD init error: ${e.message}")
                    withContext(Dispatchers.Main) {
                        binding.statusText.text = "Initialization error: ${e.message}"
                    }
                    throw e // Re-throw to be caught in the calling coroutine.
                }
            }

            // "Priming" read (send a command and discard the response).  This is often
            // necessary to get the ELM327 adapter ready to receive further commands.
            try {
                outputStream.write(("0100\r").toByteArray()) // Request supported PIDs (Mode 01, PID 00).
                delay(300)
                inputStream.read(buffer) // Read and discard the response.
                Log.d("ObdConnectActivity", "Priming read complete.")
            } catch (e: IOException) {
                Log.e("ObdConnectActivity", "Priming read error: ${e.message}")
                // It's not *strictly* critical if this fails, so we won't throw an exception here.
                //  But log the error.
            }
        }
    }


    private suspend fun startContinuousReading(outputStream: OutputStream, inputStream: InputStream) {
        obdDataReader = ObdDataReader(lifecycleScope, outputStream, inputStream, GearCalculator())

        lifecycleScope.launch {
            obdDataReader?.obdData?.collect { data ->
                // Update UI elements with OBD data.
                binding.rpmText.text = data.rpm
                binding.speedText.text = data.speed
                binding.tempText.text = data.temperature
                binding.fuelRateText.text = data.instantFuelRate // Correct ID
                binding.avgFuelConsText.text = data.averageFuelConsumption // Correct ID
                binding.avgSpeedText.text = data.averageSpeed // Correct ID
                binding.distanceText.text = data.distanceTraveled // Correct ID
                binding.fuelUsedText.text = data.fuelUsed // Correct ID
                binding.instantFuelConsumptionText.text = data.instantFuelConsumption
            }
        }

        obdDataReader?.startContinuousReading()
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

    private fun disconnectObd() {
        obdDataReader?.stopReading() // Stop the data reader.
        lifecycleScope.launch {
            try {
                obdSocket?.close() // Close the Bluetooth socket.
            } catch (e: IOException) {
                Log.e("ObdConnectActivity", "Error closing socket: ${e.message}")
            } finally {
                obdSocket = null // Reset the socket variable.
                withContext(Dispatchers.Main) {
                    // Update UI elements.
                    binding.statusText.text = "Ensure OBD2 is paired and press connect"
                    binding.rpmText.text = "- RPM"
                    binding.speedText.text = "- km/h"
                    binding.tempText.text = "- °C"
                    binding.fuelRateText.text = "- L/h"          // Correct ID
                    binding.avgFuelConsText.text = "- L/100km"  // Correct ID
                    binding.avgSpeedText.text = "- km/h"        // Correct ID
                    binding.distanceText.text = "- km"          // Correct ID
                    binding.fuelUsedText.text = "- L"          // Correct ID
                    binding.instantFuelConsumptionText.text = "- L/100km" //Added
                    binding.connectionStatus.setImageResource(android.R.drawable.presence_offline)
                    updateButtonStates() // Update button states after disconnection.
                }
                //Automatically Reset on Disconnect
                obdDataReader?.resetTripData()
            }
        }
    }
    private fun updateButtonStates(isConnecting: Boolean = false) {
        binding.apply {
            if (obdSocket != null) {
                // Connected state:
                connectButton.isEnabled = false
                disconnectButton.isEnabled = true
            } else {
                // Disconnected state:
                connectButton.isEnabled = !isConnecting
                disconnectButton.isEnabled = false
            }

            // Set button transparency based on enabled state.
            connectButton.alpha = if (connectButton.isEnabled) 1.0f else 0.5f
            disconnectButton.alpha = if (disconnectButton.isEnabled) 1.0f else 0.5f

            // Show/hide the progress bar.
            connectionProgress.visibility = if (isConnecting) View.VISIBLE else View.GONE
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        disconnectObd() // Ensure resources are released when the activity is destroyed.
    }
}