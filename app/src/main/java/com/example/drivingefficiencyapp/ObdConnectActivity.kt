package com.example.drivingefficiencyapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.drivingefficiencyapp.databinding.ObdConnectActivityBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.View

class ObdConnectActivity : AppCompatActivity() {
    private lateinit var binding: ObdConnectActivityBinding
    private val discoveredDevices = mutableMapOf<String, BluetoothDevice>()
    private var selectedDevice: BluetoothDevice? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }

            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        try {
                            if (it.address == "66:1E:32:30:AF:15" && it.name == "OBDII"
                                && !discoveredDevices.containsKey(it.address)) {
                                discoveredDevices[it.address] = it
                                onDeviceFound(it)
                            }
                        } catch (e: SecurityException) {
                            binding.statusText.text = getString(R.string.permission_denied_device)
                        }
                    }
                }
            }
        }
    }

    private fun onDeviceFound(device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            selectedDevice = device
            binding.statusText.text = getString(R.string.device_found)
            binding.connectButton.isEnabled = true
        } catch (e: SecurityException) {
            binding.statusText.text = getString(R.string.permission_denied_device)
        }
    }

    private val pairingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }

            when (intent.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    try {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        when (device?.bondState) {
                            BluetoothDevice.BOND_BONDED -> {
                                binding.statusText.text = getString(R.string.paired_successfully)
                                testConnection(device)
                            }
                            BluetoothDevice.BOND_NONE -> {
                                binding.statusText.text = getString(R.string.pairing_failed)
                            }
                        }
                    } catch (e: SecurityException) {
                        binding.statusText.text = getString(R.string.permission_denied)
                    }
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            scanForDevices()
        } else {
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ObdConnectActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        registerReceivers()
        checkPermissions()
    }

    private fun setupUI() {
        binding.scanButton.setOnClickListener {
            scanForDevices()
        }

        binding.connectButton.setOnClickListener {
            selectedDevice?.let { device ->
                connectToDevice(device)
            } ?: Toast.makeText(this, "Please select a device first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerReceivers() {
        registerReceiver(discoveryReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(pairingReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            if (permissions.all {
                    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                }) {
                scanForDevices()
            } else {
                requestPermissionLauncher.launch(permissions)
            }
        }
    }

    private fun scanForDevices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            binding.statusText.text = getString(R.string.bluetooth_permission_required)
            return
        }

        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }

            discoveredDevices.clear()
            binding.statusText.text = getString(R.string.scanning_devices)

            // Find the specific OBD device
            bluetoothAdapter?.bondedDevices?.find { device ->
                device.address == "66:1E:32:30:AF:15" && device.name == "OBDII"
            }?.let { device ->
                discoveredDevices[device.address] = device
                onDeviceFound(device)
            }

            bluetoothAdapter?.startDiscovery()
        } catch (e: SecurityException) {
            binding.statusText.text = getString(R.string.permission_denied_scan)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                device.createBond()
            } else {
                testConnection(device)
            }
        } catch (e: SecurityException) {
            binding.statusText.text = getString(R.string.permission_denied_connection)
        }
    }

    private fun testConnection(device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {
            binding.statusText.text = getString(R.string.bluetooth_permission_required)
            return
        }

        binding.connectButton.isEnabled = false
        binding.statusText.text = getString(R.string.connecting)
        binding.connectionProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val socket = withContext(Dispatchers.IO) {
                    device.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    ).apply { connect() }
                }

                val commands = listOf("ATZ", "ATE0", "ATL0", "ATH0", "ATSP0")
                withContext(Dispatchers.IO) {
                    commands.forEach { command ->
                        socket.outputStream.write((command + "\r").toByteArray())
                        Thread.sleep(100)
                    }
                }

                binding.statusText.text = getString(R.string.connection_success, device.name)
                binding.connectionProgress.visibility = View.GONE
                binding.connectionStatus.setImageResource(android.R.drawable.presence_online)

                withContext(Dispatchers.IO) {
                    socket.close()
                }
            } catch (e: Exception) {
                binding.statusText.text = getString(R.string.connection_failed, e.message)
                binding.connectionProgress.visibility = View.GONE
                binding.connectionStatus.setImageResource(android.R.drawable.presence_busy)
            } finally {
                binding.connectButton.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
                unregisterReceiver(discoveryReceiver)
                unregisterReceiver(pairingReceiver)
                bluetoothAdapter?.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            // Silent fail on cleanup
        }
    }
}