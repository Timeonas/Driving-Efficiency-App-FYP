package com.example.drivingefficiencyapp.modelLayer.obd

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.drivingefficiencyapp.modelLayer.trip.TripData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.UUID

/**
 * Singleton class to manage OBD connection throughout the app
 */
object ObdConnectionManager {
    private const val TAG = "ObdConnectionManager"
    private const val OBD_MAC_ADDRESS = "66:1E:32:30:AF:15" // Replace with your OBD-II adapter's MAC address
    private const val OBD_NAME = "OBDII" // Replace with your OBD2 device name

    // Connection state
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    // Active socket
    private var obdSocket: BluetoothSocket? = null
    private var connectionJob: Job? = null
    private var dataReaderJob: Job? = null
    private var obdDataReader: ObdDataReader? = null

    /**
     * Connect to OBD device
     */
    fun connect(context: Context, scope: CoroutineScope): Job {
        // If already connected, do nothing
        if (_connectionState.value) {
            return scope.launch { /* No-op */ }
        }

        connectionJob?.cancel()

        return scope.launch {
            try {
                if (!hasRequiredPermissions(context)) {
                    _connectionState.value = false
                    return@launch
                }

                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter

                val pairedDevices = bluetoothAdapter?.bondedDevices
                val obdDevice = pairedDevices?.find { it.address == OBD_MAC_ADDRESS && it.name == OBD_NAME }

                if (obdDevice == null) {
                    _connectionState.value = false
                    return@launch
                }

                try {
                    withTimeoutOrNull(5000) { // 5 second timeout
                        withContext(Dispatchers.IO) {
                            val socket = obdDevice.createRfcommSocketToServiceRecord(
                                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                            )
                            socket.connect()
                            obdSocket = socket
                            _connectionState.value = true
                        }
                    } ?: run {
                        // Timeout occurred
                        _connectionState.value = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Connection failed", e)
                    _connectionState.value = false
                    obdSocket = null
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception", e)
                _connectionState.value = false
                obdSocket = null
            }
        }
    }

    /**
     * Disconnect from OBD device
     */
    fun disconnect(scope: CoroutineScope): Job {
        return scope.launch {
            try {
                obdSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing socket: ${e.message}")
            } finally {
                obdSocket = null
                _connectionState.value = false
                dataReaderJob?.cancel()
                obdDataReader = null
            }
        }
    }

    /**
     * Get the active socket
     */
    fun getSocket(): BluetoothSocket? = obdSocket

    /**
     * Initialize OBD with the given initialization commands
     */
    suspend fun initializeObd(callback: (Boolean) -> Unit) {
        val socket = obdSocket ?: return callback(false)

        try {
            withContext(Dispatchers.IO) {
                val outputStream = socket.outputStream
                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)

                val initCommands = listOf(
                    "ATZ",      // Reset ELM327
                    "ATE0",     // Echo off
                    "ATL0",     // Linefeeds off
                    "ATH0",     // Headers off
                    "ATSP0",    // Set Protocol to Automatic
                    "ATDP",     // Display Protocol
                )

                delay(500) // Initial delay

                // Send initialization commands
                for (command in initCommands) {
                    try {
                        outputStream.write((command + "\r").toByteArray())
                        delay(300)
                        val bytes = inputStream.read(buffer)
                        val response = String(buffer, 0, bytes).trim()

                        if (!response.contains("OK") && !response.uppercase().contains("ELM") && !response.uppercase().contains("AUTO")) {
                            callback(false)
                            return@withContext
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "OBD init error: ${e.message}")
                        callback(false)
                        return@withContext
                    }
                }

                try {
                    outputStream.write(("0100\r").toByteArray()) // Request supported PIDs
                    delay(300)
                    inputStream.read(buffer) // Read and discard response
                } catch (e: IOException) {
                    Log.e(TAG, "Priming read error: ${e.message}")
                }

                callback(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing OBD: ${e.message}")
            callback(false)
        }
    }

    /**
     * Start continuous data reading
     */
    fun startContinuousReading(scope: CoroutineScope): ObdDataReader? {
        val socket = obdSocket ?: return null

        try {
            obdDataReader = ObdDataReader(scope, socket.outputStream, socket.inputStream, GearCalculator())
            dataReaderJob = scope.launch {
                obdDataReader?.startContinuousReading()
            }
            return obdDataReader
        } catch (e: Exception) {
            Log.e(TAG, "Error starting continuous reading: ${e.message}")
            return null
        }
    }

    /**
     * Reset trip data
     */
    fun resetTripData() {
        obdDataReader?.resetTripData()
    }

    fun getTripSummary(): TripData {
        return obdDataReader?.getTripSummary() ?: TripData()
    }

    /**
     * Check if the required permissions are granted
     */
    private fun hasRequiredPermissions(context: Context): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}