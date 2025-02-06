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
import android.os.Build
import androidx.annotation.RequiresApi

class ObdConnectActivity : AppCompatActivity() {
    private lateinit var binding: ObdConnectActivityBinding
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var selectedDevice: BluetoothDevice? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            scanForDevices()
        } else {
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide() //Hid the action bar which references the app name
        super.onCreate(savedInstanceState)
        binding = ObdConnectActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        binding.scanButton.setOnClickListener {
            scanForDevices()
        }

        binding.connectButton.setOnClickListener {
            selectedDevice?.let { device ->
                testConnection(device)
            } ?: Toast.makeText(this, "Please select a device first", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            scanForDevices()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun scanForDevices() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {
            binding.statusText.text = getString(R.string.bluetooth_permission_required)
            return
        }

        binding.deviceList.removeAllViews()
        binding.statusText.text = getString(R.string.scanning_devices)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pairedDevices = try {
                    bluetoothAdapter?.bondedDevices ?: emptySet()
                } catch (e: SecurityException) {
                    withContext(Dispatchers.Main) {
                        binding.statusText.text = getString(R.string.permission_denied)
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    if (pairedDevices.isEmpty()) {
                        binding.statusText.text = getString(R.string.no_paired_devices)
                        return@withContext
                    }

                    pairedDevices.forEach { device ->
                        addDeviceToList(device)
                    }
                    binding.statusText.text = getString(R.string.select_device)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.statusText.text = getString(R.string.error_scanning, e.message)
                }
            }
        }
    }

    private fun addDeviceToList(device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val deviceName = try {
            device.name ?: "Unknown"
        } catch (e: SecurityException) {
            "Unknown"
        }

        val deviceAddress = try {
            device.address
        } catch (e: SecurityException) {
            "Unknown"
        }

        val button = android.widget.Button(this).apply {
            text = getString(R.string.device_info, deviceName, deviceAddress)
            setOnClickListener {
                selectedDevice = device
                binding.statusText.text = getString(R.string.device_selected, deviceName)
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }
        binding.deviceList.addView(button)
    }

    private fun testConnection(device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {
            binding.statusText.text = getString(R.string.bluetooth_permission_required)
            return
        }

        val deviceName = try {
            device.name ?: "Unknown Device"
        } catch (e: SecurityException) {
            "Unknown Device"
        }

        binding.connectButton.isEnabled = false
        binding.statusText.text = getString(R.string.connecting)
        binding.connectionProgress.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val socket = withContext(Dispatchers.IO) {
                    device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                        .apply { connect() }
                }

                val commands = listOf("ATZ", "ATE0", "ATL0", "ATH0", "ATSP0")
                withContext(Dispatchers.IO) {
                    commands.forEach { command ->
                        socket.outputStream.write((command + "\r").toByteArray())
                        Thread.sleep(100)
                    }
                }

                binding.statusText.text = getString(R.string.connection_success, deviceName)
                binding.connectionProgress.visibility = android.view.View.GONE
                binding.connectionStatus.setImageResource(android.R.drawable.presence_online)

                withContext(Dispatchers.IO) {
                    socket.close()
                }
            } catch (e: Exception) {
                binding.statusText.text = getString(R.string.connection_failed, e.message)
                binding.connectionProgress.visibility = android.view.View.GONE
                binding.connectionStatus.setImageResource(android.R.drawable.presence_busy)
            } finally {
                binding.connectButton.isEnabled = true
            }
        }
    }
}