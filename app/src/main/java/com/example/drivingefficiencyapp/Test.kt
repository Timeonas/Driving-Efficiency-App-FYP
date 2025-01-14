package com.example.drivingefficiencyapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Test : AppCompatActivity() {
    private var counter = 0
    private lateinit var counterText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_main)

        // Initialize views
        counterText = findViewById(R.id.counterText)
        val incrementButton = findViewById<Button>(R.id.incrementButton)
        val resetButton = findViewById<Button>(R.id.resetButton)

        // Set initial counter value
        updateCounterDisplay()

        // Set click listeners
        incrementButton.setOnClickListener {
            counter++
            updateCounterDisplay()
        }

        resetButton.setOnClickListener {
            counter = 0
            updateCounterDisplay()
        }
    }

    private fun updateCounterDisplay() {
        counterText.text = "Count: $counter"
    }
}