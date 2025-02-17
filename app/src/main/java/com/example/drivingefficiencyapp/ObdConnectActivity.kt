package com.example.drivingefficiencyapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private var readingJob: Job? = null // Store the coroutine job for cancellation

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // OBD Specific Info
    private val obdMacAddress = "66:1E:32:30:AF:15"
    private val obdName = "OBDII"
    private var isScanning = false
    private var discoveredOBDDevice: BluetoothDevice? = null


    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("ObdConnectActivity", "Bluetooth Connect permission missing")
                return
            }

            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        Log.d("ObdConnectActivity", "Discovered device: ${it.name}, ${it.address}")
                        if (it.address == obdMacAddress && it.name == obdName) {
                            Log.d("ObdConnectActivity", "Target OBD-II device found.")
                            discoveredOBDDevice = it
                            lifecycleScope.launch(Dispatchers.Main) {
                                binding.statusText.text = "Device found. Tap Start."
                                updateButtonStates(deviceFound = true)
                            }
                            isScanning = false
                            bluetoothAdapter?.cancelDiscovery()

                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    isScanning = true
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.statusText.text = getString(R.string.scanning_devices)
                        updateButtonStates(isScanning = true)
                    }
                    Log.d("ObdConnectActivity", "Bluetooth discovery started.")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("ObdConnectActivity", "Bluetooth discovery finished. isScanning: $isScanning")
                    if (isScanning) {
                        isScanning = false
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (discoveredOBDDevice == null) {
                                binding.statusText.text = "Scan complete. Device not found."
                            }
                            updateButtonStates()
                        }
                    }
                }
            }
        }
    }


    private val pairingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("ObdConnectActivity", "Bluetooth connect permissions error")
                return
            }

            when (intent.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device?.address == obdMacAddress) {
                        when (device.bondState) {
                            BluetoothDevice.BOND_BONDING -> {
                                Log.d("ObdConnectActivity", "Pairing in progress...")
                                binding.statusText.text = "Pairing in progress..."
                                updateButtonStates(isPairing = true) // Disable buttons
                            }

                            BluetoothDevice.BOND_BONDED -> {
                                Log.d("ObdConnectActivity", "Successfully paired.")
                                binding.statusText.text =
                                    "Device paired. Tap Start." // Clear instructions
                                discoveredOBDDevice = device //Store the now paired device
                                updateButtonStates(deviceFound = true) // Enable "Start"

                            }

                            BluetoothDevice.BOND_NONE -> {
                                Log.d("ObdConnectActivity", "Pairing failed.")
                                binding.statusText.text = "Pairing failed"
                                updateButtonStates() // Re-enable buttons
                            }
                        }
                    }
                }
            }
        }
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
        } else {
            Toast.makeText(this, "Bluetooth permissions are required.", Toast.LENGTH_LONG).show()
            binding.statusText.text = "Bluetooth permissions required." // Update UI
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ObdConnectActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        registerReceivers()
        checkPermissions() // Only check permissions

        // Set initial UI state:
        binding.statusText.text = "Disconnected"
        binding.rpmText.text = "- RPM"
        binding.speedText.text = "- km/h"
        binding.tempText.text = "- °C"
        binding.connectionStatus.setImageResource(android.R.drawable.presence_offline)
        updateButtonStates() // Initialize button states

        checkPermissions()

        // After permissions are granted, check paired devices
        if (hasRequiredPermissions()) {
            checkPairedDevices()
        }
    }


    private fun setupUI() {
        binding.scanButton.setOnClickListener {
            if (!isScanning) {
                scanForDevices()
            }
        }

        binding.connectButton.setOnClickListener {
            discoveredOBDDevice?.let { device ->
                if (device.bondState == BluetoothDevice.BOND_NONE) {
                    if (ContextCompat.checkSelfPermission(
                            this@ObdConnectActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        device.createBond()
                    }
                } else {
                    startConnection(device) // Now only attempts connection
                }
            } ?: run {
                Toast.makeText(this, "No device found to connect to.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.disconnectButton.setOnClickListener {
            disconnectObd()
        }
    }

    private fun registerReceivers() {
        registerReceiver(discoveryReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(discoveryReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        registerReceiver(
            discoveryReceiver,
            IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        )
        registerReceiver(pairingReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
    }

    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun checkPairedDevices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("ObdConnectActivity", "Missing BLUETOOTH_CONNECT permission")
            return
        }

        try {
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                Log.d("ObdConnectActivity", "Found paired device: ${device.name}, ${device.address}")
                if (device.address == obdMacAddress && device.name == obdName) {
                    Log.d("ObdConnectActivity", "Found paired OBD device!")
                    discoveredOBDDevice = device
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.statusText.text = "Device already paired. Tap Start."
                        updateButtonStates(deviceFound = true)
                        binding.scanButton.isEnabled = false
                        binding.scanButton.alpha = 0.5f
                    }
                    return@forEach
                }
            }
        } catch (e: SecurityException) {
            Log.e("ObdConnectActivity", "Security exception when checking paired devices: ${e.message}")
        }
    }

    private fun scanForDevices() {
        if (!hasRequiredPermissions()) {
            Log.e("ObdConnectActivity", "Missing required permissions")
            return
        }

        checkPairedDevices()

        // If device already found in paired devices, no need to scan
        if (discoveredOBDDevice != null) {
            updateButtonStates(deviceFound = true)  // Update button states
            return
        }

        val adapter = bluetoothAdapter ?: run {
            Log.e("ObdConnectActivity", "Bluetooth adapter is null")
            return
        }

        if (adapter.isDiscovering) {
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED) {
                    adapter.cancelDiscovery()
                    isScanning = false
                } else {
                    Log.e("ObdConnectActivity", "Missing BLUETOOTH_SCAN permission")
                    return
                }
            } catch (e: SecurityException) {
                Log.e("ObdConnectActivity", "Security exception when canceling discovery: ${e.message}")
                return
            }
        }

        isScanning = true
        updateButtonStates(isScanning = true)
        binding.statusText.text = "Scanning for devices..."

        // Start discovery with timeout
        adapter.startDiscovery()

        lifecycleScope.launch {
            delay(10000) // 10-second timeout
            if (isScanning) {
                isScanning = false
                adapter.cancelDiscovery()
                withContext(Dispatchers.Main) {
                    if (discoveredOBDDevice == null) {
                        binding.statusText.text = "Scan timed out. Device not found."
                        updateButtonStates()
                    }
                }
                Log.d("ObdConnectActivity", "Bluetooth discovery timed out.")
            }
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
                        val rpm = ((256 * a) + b) / 4
                        "$rpm RPM"
                    } else {
                        "- (Invalid Data - Short)"
                    }
                }

                "010D" -> { // Speed
                    if (dataBytes.length >= 2) {
                        val speed = Integer.parseInt(dataBytes.substring(0, 2), 16)
                        "$speed km/h"
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
        readingJob?.cancel() // Cancel the reading coroutine
        lifecycleScope.launch {
            try {
                obdSocket?.close() // Close the socket
            } catch (e: IOException) {
                Log.e("ObdConnectActivity", "Error closing socket: ${e.message}")
            } finally {
                obdSocket = null // Ensure socket is null after closing
                withContext(Dispatchers.Main) {
                    // Since the device is still paired, maintain that state
                    if (discoveredOBDDevice != null) {
                        binding.statusText.text = "Device already paired. Tap Start."
                        updateButtonStates(deviceFound = true)
                    } else {
                        binding.statusText.text = "Disconnected"
                        updateButtonStates()
                    }

                    // Reset the readings
                    binding.rpmText.text = "- RPM"
                    binding.speedText.text = "- km/h"
                    binding.tempText.text = "- °C"
                    binding.connectionStatus.setImageResource(android.R.drawable.presence_offline)
                }
            }
        }
    }

    private fun updateButtonStates(
        isScanning: Boolean = false,
        isPairing: Boolean = false,
        isConnecting: Boolean = false,
        deviceFound: Boolean = false
    ) {
        binding.apply {
            if (obdSocket != null) {
                scanButton.isEnabled = false
                connectButton.isEnabled = false
                disconnectButton.isEnabled = true
            }
            else if (deviceFound) {
                scanButton.isEnabled = false
                connectButton.isEnabled = true
                disconnectButton.isEnabled = false
            }
            // No device found/paired and no connection
            else {
                scanButton.isEnabled = !isScanning && !isPairing && !isConnecting
                connectButton.isEnabled = false
                disconnectButton.isEnabled = false
            }

            // Update button appearance
            scanButton.alpha = if (scanButton.isEnabled) 1.0f else 0.5f
            connectButton.alpha = if (connectButton.isEnabled) 1.0f else 0.5f
            disconnectButton.alpha = if (disconnectButton.isEnabled) 1.0f else 0.5f

            //how/hide progress indicator
            connectionProgress.visibility = if (isConnecting) View.VISIBLE else View.GONE
        }
    }
}