package com.example.drivingefficiencyapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.drivingefficiencyapp.databinding.MainMenuActivityBinding
import com.example.drivingefficiencyapp.obd.ObdConnectActivity
import com.example.drivingefficiencyapp.obd.ObdConnectionManager
import com.example.drivingefficiencyapp.profile.ProfileActivity
import com.example.drivingefficiencyapp.trip.TripsActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainMenuActivity : AppCompatActivity() {
    private lateinit var binding: MainMenuActivityBinding

    companion object {
        private const val TAG = "MainMenuActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = MainMenuActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        observeObdConnection()
    }

    private fun setupButtons() {
        // Connect to OBD button
        binding.obdConnectButton.setOnClickListener {
            if (ObdConnectionManager.connectionState.value) {
                // If already connected, show OBD screen
                val intent = Intent(this, ObdConnectActivity::class.java)
                startActivity(intent)
            } else {
                // If not connected, try to connect in background
                binding.connectionStatusText.text = "Connecting to OBD..."
                val connectionJob = ObdConnectionManager.connect(this, lifecycleScope)

                // Set a timeout to reset the status if connection is taking too long
                lifecycleScope.launch {
                    delay(6000) // Wait slightly longer than connection timeout
                    if (binding.connectionStatusText.text == "Connecting to OBD...") {
                        binding.connectionStatusText.text = "Connection timed out"
                        delay(2000) // Show timeout message briefly
                        binding.connectionStatusText.text = "OBD Not Connected"
                    }
                }
            }
        }

        // Start drive button (initially disabled)
        binding.startDriveButton.setOnClickListener {
            val intent = Intent(this, StartDriveActivity::class.java)
            startActivity(intent)
        }

        // View trips button (initially disabled)
        binding.viewTripsButton.setOnClickListener {
            val intent = Intent(this, TripsActivity::class.java)
            startActivity(intent)
        }

        // Profile button (always enabled)
        binding.profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // OBD Test button (for testing purposes)
        binding.obdTestButton.setOnClickListener {
            val intent = Intent(this, ObdConnectActivity::class.java)
            startActivity(intent)
        }

        // Initially disable trip-related buttons
        updateButtonState(false)
    }

    private fun observeObdConnection() {
        lifecycleScope.launchWhenStarted {
            ObdConnectionManager.connectionState.collect { isConnected ->
                updateButtonState(isConnected)
                updateConnectionStatus(isConnected)

                // Update connect button text based on connection state
                binding.obdConnectButton.text = if (isConnected) {
                    "OBD Settings"
                } else {
                    "Connect to OBD"
                }
            }
        }
    }

    private fun updateButtonState(isConnected: Boolean) {
        // Only the Start Drive button should depend on OBD connection
        binding.startDriveButton.isEnabled = isConnected
        binding.startDriveButton.alpha = if (isConnected) 1.0f else 0.5f

        // View Trips should always be enabled
        binding.viewTripsButton.isEnabled = true
        binding.viewTripsButton.alpha = 1.0f

        // Update connection indicator
        binding.connectionStatusView.visibility = View.VISIBLE
        binding.connectionStatusView.setImageResource(
            if (isConnected) android.R.drawable.presence_online
            else android.R.drawable.presence_offline
        )
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        binding.connectionStatusText.text = if (isConnected) {
            "OBD Connected"
        } else {
            "OBD Not Connected"
        }

        // Change prompt text based on connection status
        binding.promptText.text = if (isConnected) {
            "You're all set! Start driving or view past trips."
        } else {
            "Connect to your OBD adapter to start using the app."
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if OBD connection state has changed
        updateButtonState(ObdConnectionManager.connectionState.value)
        updateConnectionStatus(ObdConnectionManager.connectionState.value)
    }
}