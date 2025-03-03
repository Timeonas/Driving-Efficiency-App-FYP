package com.example.drivingefficiencyapp.ui
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.drivingefficiencyapp.R

class TripDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.trip_summary_dialog)

        // Extract trip details from intent
        val date = intent.getStringExtra("date") ?: ""
        val duration = intent.getStringExtra("duration") ?: ""
        val averageSpeed = intent.getFloatExtra("averageSpeed", 0f)
        val distance = intent.getFloatExtra("distanceTraveled", 0f)
        val fuelConsumption = intent.getFloatExtra("averageFuelConsumption", 0f)
        val fuelUsed = intent.getFloatExtra("fuelUsed", 0f)
        val maxRPM = intent.getIntExtra("maxRPM", 0)
        val avgRPM = intent.getFloatExtra("avgRPM", 0f)

        // Set up views with data
        setupViews(date, duration, averageSpeed, distance, fuelConsumption, fuelUsed, maxRPM, avgRPM)

        // Set up buttons
        findViewById<Button>(R.id.saveButton).visibility = android.view.View.GONE

        findViewById<Button>(R.id.dismissButton).setOnClickListener {
            finish()
        }

        // Set title
        val summaryTitle = findViewById<TextView>(R.id.summaryTitle)
        summaryTitle.text = "Trip Details: $date"

        // Hide feedback section
        findViewById<TextView>(R.id.feedbackTitle).visibility = android.view.View.VISIBLE
        findViewById<TextView>(R.id.feedbackText).visibility = android.view.View.VISIBLE
    }

    private fun setupViews(
        date: String,
        duration: String,
        averageSpeed: Float,
        distance: Float,
        fuelConsumption: Float,
        fuelUsed: Float,
        maxRPM: Int,
        avgRPM: Float
    ) {
        // Find all TextViews
        findViewById<TextView>(R.id.tripDurationText).text = duration
        findViewById<TextView>(R.id.avgSpeedText).text = "${formatFloat(averageSpeed)} km/h"
        findViewById<TextView>(R.id.distanceText).text = "${formatFloat(distance)} km"
        findViewById<TextView>(R.id.fuelConsumptionText).text = "${formatFloat(fuelConsumption)} L/100km"
        findViewById<TextView>(R.id.fuelUsedText).text = "${formatFloat(fuelUsed)} L"

        // Calculate cost (using a default fuel price)
        val fuelPrice = 1.75 // Default price per liter in Euro
        val estimatedCost = fuelUsed * fuelPrice
        findViewById<TextView>(R.id.estimatedCostText).text = getString(R.string.estimated_cost_format, estimatedCost)

        val rpmInfoText = "Max RPM: $maxRPM | Avg RPM: ${formatFloat(avgRPM)}"
        findViewById<TextView>(R.id.feedbackText).apply {
            visibility = android.view.View.VISIBLE
            text = rpmInfoText
        }
        findViewById<TextView>(R.id.feedbackTitle).apply {
            visibility = android.view.View.VISIBLE
            text = "Engine Performance"
        }

        // Hide efficiency score
        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.scoreContainer).visibility = android.view.View.GONE
    }

    private fun formatFloat(value: Float): String {
        return String.format("%.1f", value)
    }
}