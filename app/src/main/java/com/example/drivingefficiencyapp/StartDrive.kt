package com.example.drivingefficiencyapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class StartDrive : AppCompatActivity() {
    private lateinit var timerTextView: TextView
    private lateinit var dateTextView: TextView
    private var startTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - startTime
            val seconds = (elapsedTime / 1000).toInt()
            val minutes = seconds / 60
            val hours = minutes / 60

            val timeString = String.format("%02d:%02d:%02d",
                hours,
                minutes % 60,
                seconds % 60)

            timerTextView.text = timeString

            if (isRunning) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.start_drive)

        timerTextView = findViewById(R.id.timerTextView)
        dateTextView = findViewById(R.id.dateTextView)
        val endDriveButton = findViewById<Button>(R.id.endDriveButton)

        // Set current date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        dateTextView.text = dateFormat.format(Date())

        // Start timer
        startTime = System.currentTimeMillis()
        isRunning = true
        handler.post(timerRunnable)

        endDriveButton.setOnClickListener {
            isRunning = false
            finish() // Returns to previous activity
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(timerRunnable)
    }
}