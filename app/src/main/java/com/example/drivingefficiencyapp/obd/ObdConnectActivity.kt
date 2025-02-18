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
    private var currentRpm = 0
    private var currentSpeed: Double = 0.0
    private val gearCalculator = GearCalculator()

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

    private val OBD_MAC_ADDRESS = "66:1E:32:30:AF:15"
    private val OBD_NAME = "OBDII"

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

        // Set initial UI state
        binding.statusText.text = "Ensure OBD2 is paired and press connect"
        binding.rpmText.text = "- RPM"
        binding.speedText.text = "- km/h"
        binding.tempText.text = "- °C"
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
                val obdDevice = pairedDevices?.find { it.address == OBD_MAC_ADDRESS && it.name == OBD_NAME }

                if (obdDevice == null) {
                    binding.statusText.text = "Failed to connect. Please ensure the OBD2 device is paired to the phone"
                    return@launch
                }

                startConnection(obdDevice)
            } catch (e: SecurityException) {
                binding.statusText.text = "Failed to access Bluetooth devices"
            }
        }
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

    private fun startConnection(device: BluetoothDevice) { // Renamed function
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ObdConnectActivity", "Bluetooth Connect permission missing in connectToDevice")
            return
        }

        // Cancel any ongoing connection attempts
        readingJob?.cancel()
        try {
            obdSocket?.close()
        } catch (e: IOException) {
            Log.e("ObdConnectActivity", "Error closing existing socket: ${e.message}")
        }
        obdSocket = null

        readingJob = lifecycleScope.launch {

            withContext(Dispatchers.Main) {
                updateButtonStates(isConnecting = true)
                binding.statusText.text = "Connecting..."
            }

            try {
                // Use withTimeoutOrNull for connection timeout, including socket creation.
                val tempSocket = withTimeoutOrNull(10000) {
                    withContext(Dispatchers.IO){ //Socket operations to IO thread
                        device.createRfcommSocketToServiceRecord(
                            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                        ).apply {
                            connect()
                        }
                    }
                }
                obdSocket = tempSocket

                if(obdSocket == null){
                    throw IOException("Connection timed out") // Throw if null
                }


                // OBD initialization and data stream
                obdSocket?.let { socket ->
                    Log.d("ObdConnectActivity", "Socket connected, calling initializeObd")
                    initializeObd(socket)
                    Log.d("ObdConnectActivity", "initializeObd complete, calling startContinuousReading")
                    startContinuousReading(socket.outputStream, socket.inputStream)
                    withContext(Dispatchers.Main) {
                        binding.connectionProgress.visibility = View.GONE
                        binding.connectionStatus.setImageResource(android.R.drawable.presence_online)
                        binding.statusText.text = "Connected"
                        updateButtonStates() // isScanning is already false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.statusText.text = "Connection failed: ${e.message}"
                    binding.connectionProgress.visibility = View.GONE
                    binding.connectionStatus.setImageResource(android.R.drawable.presence_busy)
                    updateButtonStates()
                }
                Log.e("ObdConnectActivity", "Connection failed: ${e.message}", e)
            }
        }
    }

    private suspend fun initializeObd(socket: BluetoothSocket) {
        withContext(Dispatchers.IO) {
            val outputStream = socket.outputStream
            val inputStream = socket.inputStream
            val buffer = ByteArray(1024)

            val initCommands = listOf(
                "ATZ",      // Reset
                "ATE0",     // Echo off
                "ATL0",     // Linefeeds off
                "ATH0",     // Headers off
                "ATSP0",    // Set Protocol to Automatic
                "ATDP",     // Display Protocol
            )
            delay(500)

            for (command in initCommands) {
                try {
                    outputStream.write((command + "\r").toByteArray())
                    delay(300)
                    val bytes = inputStream.read(buffer)
                    val response = String(buffer, 0, bytes).trim()
                    Log.d("ObdConnectActivity", "OBD Init: $command -> $response")

                    if (!response.contains("OK") && !response.uppercase()
                            .contains("ELM") && !response.uppercase().contains("AUTO")
                    ) {
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
                    throw e
                }
            }

            try {
                outputStream.write(("0100\r").toByteArray())
                delay(300)
                inputStream.read(buffer) // Read and *discard* the response
                Log.d("ObdConnectActivity", "Priming read complete.")
            } catch (e: IOException) {
                Log.e("ObdConnectActivity", "Priming read error: ${e.message}")
            }
        }
    }

    private suspend fun sendCommandAndReadResponse(
        outputStream: OutputStream,
        inputStream: InputStream,
        command: String
    ): String {
        return withContext(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            try {
                Log.d("SendCommand", "Sending command: $command")
                val startTime = System.currentTimeMillis()
                val response = withTimeoutOrNull(2000) {
                    outputStream.write((command + "\r").toByteArray())
                    delay(50)
                    val bytesRead = withTimeoutOrNull(1000) { inputStream.read(buffer) } ?: 0
                    if (bytesRead > 0) {
                        String(buffer, 0, bytesRead)
                    } else {
                        "" // Return empty string on inner timeout/no data
                    }
                } ?: ""  // Return empty string on outer timeout

                val endTime = System.currentTimeMillis()
                Log.d("SendCommand", "Command $command took ${endTime - startTime} ms")
                response // Return the response string
            } catch (e: IOException) {
                Log.e("ObdConnectActivity", "Error sending/reading command $command: ${e.message}")
                "" // Return empty string on error
            }
        }
    }

    private suspend fun startContinuousReading(
        outputStream: OutputStream,
        inputStream: InputStream
    ) {
        Log.d("ObdConnectActivity", "startContinuousReading called")
        readingJob = lifecycleScope.launch(Dispatchers.IO) {
            val commands = listOf("010C", "010D", "0105")

            while (isActive) {
                try {
                    for (command in commands) {
                        val response = sendCommandAndReadResponse(
                            outputStream,
                            inputStream,
                            command
                        )
                        if (response.isNotEmpty()) {
                            val parsedValue = parseObdResponse(command, response)
                            withContext(Dispatchers.Main) {
                                when (command) {
                                    "010C" -> binding.rpmText.text = parsedValue
                                    "010D" -> binding.speedText.text = parsedValue
                                    "0105" -> binding.tempText.text = parsedValue
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                when (command) {
                                    "010C" -> binding.rpmText.text = "- RPM (No Data)"
                                    "010D" -> binding.speedText.text = "- km/h (No Data)"
                                    "0105" -> binding.tempText.text = "- °C (No Data)"
                                }
                            }
                        }
                        delay(100) // Delay between commands
                    }
                } catch (e: CancellationException) {
                    Log.d("ObdConnectActivity", "Reading loop cancelled")
                    break
                } catch (e: Exception) {
                    Log.e("ObdConnectActivity", "Error in reading loop: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        binding.statusText.text = "Reading error: ${e.message}"
                    }
                    delay(500)
                }
            }
        }
    }

    private fun parseObdResponse(command: String, response: String): String {
        try {
            val cleanResponse = response.replace(">", "").trim() // Remove prompt and trim
            Log.d("OBD_PARSER", "Raw response for $command: $cleanResponse")

            if (cleanResponse.isEmpty() || cleanResponse.uppercase() == "NO DATA") {
                return "- (No Data)"
            }
            if (cleanResponse.contains("ERROR") || cleanResponse.contains("CAN ERROR") || cleanResponse.startsWith(
                    "?"
                )
            ) {
                return "- (Error)"
            }
            if (cleanResponse.contains("SEARCHING")) {
                return "- (Searching...)"
            }
            if (cleanResponse.contains("BUS INIT")) {
                return "- (BUS INIT Error)"
            }

            val pid = command.substring(2) // Extract PID (e.g., "0C", "0D", "05")
            val pidIndex = cleanResponse.indexOf(pid, ignoreCase = true)

            if (pidIndex == -1) {
                return "- (Unexpected Response)" // PID not found
            }

            var dataBytes = cleanResponse.substring(pidIndex + pid.length).trim()

            dataBytes =
                dataBytes.filter { it.isDigit() || (it >= 'A' && it <= 'F') || (it >= 'a' && it <= 'f') }

            Log.d("OBD_PARSER", "Cleaned data bytes for $command: $dataBytes")


            return when (command) {
                "010C" -> { // RPM
                    if (dataBytes.isNotEmpty() && dataBytes.length >= 4) { // Added isNotEmpty check
                        Log.d("OBD_PARSER", "Parsing RPM.  dataBytes: $dataBytes")
                        val a = Integer.parseInt(dataBytes.substring(0, 2), 16)
                        val b = Integer.parseInt(dataBytes.substring(2, 4), 16)
                        currentRpm =  ((256 * a) + b) / 4
                        "$currentRpm RPM"
                    } else {
                        "- (Invalid Data - Short)"
                    }
                }

                "010D" -> { // Speed
                    if (dataBytes.length >= 2) {
                        currentSpeed = Integer.parseInt(dataBytes.substring(0, 2), 16).toDouble()
                        val currentGear = gearCalculator.calculateGear(currentRpm, currentSpeed)
                        "$currentSpeed km/h (Gear: $currentGear)"
                    } else {
                        "- (Invalid Data - Short)"
                    }
                }

                "0105" -> { // Coolant Temp
                    if (dataBytes.length >= 2) {
                        val temp = Integer.parseInt(dataBytes.substring(0, 2), 16) - 40
                        "$temp°C"
                    } else {
                        "- (Invalid Data - Short)"
                    }
                }

                else -> "Unknown command: $command"
            }

        } catch (e: NumberFormatException) {
            Log.e("OBD_PARSER", "Number format error: ${e.message} for response: $response")
            return "- (Parse Error: Invalid Number)"
        } catch (e: Exception) {
            Log.e("OBD_PARSER", "Parse error: ${e.message} for response: $response")
            return "- (Parse Error)"
        }
    }

    private fun disconnectObd() {
        readingJob?.cancel()
        lifecycleScope.launch {
            try {
                obdSocket?.close()
            } catch (e: IOException) {
                Log.e("ObdConnectActivity", "Error closing socket: ${e.message}")
            } finally {
                obdSocket = null
                withContext(Dispatchers.Main) {
                    binding.statusText.text = "Ensure OBD2 is paired and press connect"
                    binding.rpmText.text = "- RPM"
                    binding.speedText.text = "- km/h"
                    binding.tempText.text = "- °C"
                    binding.connectionStatus.setImageResource(android.R.drawable.presence_offline)
                    updateButtonStates()
                }
            }
        }
    }

    private fun updateButtonStates(isConnecting: Boolean = false) {
        binding.apply {
            if (obdSocket != null) {
                connectButton.isEnabled = false
                disconnectButton.isEnabled = true
            } else {
                connectButton.isEnabled = !isConnecting
                disconnectButton.isEnabled = false
            }

            connectButton.alpha = if (connectButton.isEnabled) 1.0f else 0.5f
            disconnectButton.alpha = if (disconnectButton.isEnabled) 1.0f else 0.5f
            connectionProgress.visibility = if (isConnecting) View.VISIBLE else View.GONE
        }
    }
}