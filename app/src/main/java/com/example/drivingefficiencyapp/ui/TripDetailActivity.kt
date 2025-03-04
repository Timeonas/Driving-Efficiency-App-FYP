package com.example.drivingefficiencyapp.ui
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.drivingefficiencyapp.EfficiencyCalculator
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.trip.Trip

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

        // Create a Trip object for efficiency calculation
        val trip = Trip(
            date = date,
            duration = duration,
            averageSpeed = averageSpeed,
            distanceTraveled = distance,
            averageFuelConsumption = fuelConsumption,
            fuelUsed = fuelUsed,
            maxRPM = maxRPM,
            avgRPM = avgRPM
        )

        // Calculate efficiency score and generate feedback using EfficiencyCalculator
        val efficiencyScore = EfficiencyCalculator.calculateEfficiencyScore(trip)
        val feedback = EfficiencyCalculator.generateFeedback(trip)

        // Set up views with data
        setupViews(trip, efficiencyScore, feedback)

        // Set up buttons
        findViewById<Button>(R.id.saveButton).visibility = View.GONE

        findViewById<Button>(R.id.dismissButton).setOnClickListener {
            finish()
        }

        // Set title
        val summaryTitle = findViewById<TextView>(R.id.summaryTitle)
        summaryTitle.text = "Trip Details: $date"
    }

    private fun setupViews(
        trip: Trip,
        efficiencyScore: Int,
        feedback: String
    ) {
        // Set efficiency score
        val scoreContainer = findViewById<ConstraintLayout>(R.id.scoreContainer)
        scoreContainer.visibility = View.VISIBLE

        val scoreTextView = findViewById<TextView>(R.id.efficiencyScoreText)
        scoreTextView.text = efficiencyScore.toString()
        scoreTextView.setTextColor(getScoreColor(efficiencyScore))

        // Find all TextViews and populate with trip data
        findViewById<TextView>(R.id.tripDurationText).text = trip.duration
        findViewById<TextView>(R.id.avgSpeedText).text = "${formatFloat(trip.averageSpeed)} km/h"
        findViewById<TextView>(R.id.distanceText).text = "${formatFloat(trip.distanceTraveled)} km"
        findViewById<TextView>(R.id.fuelConsumptionText).text = "${formatFloat(trip.averageFuelConsumption)} L/100km"
        findViewById<TextView>(R.id.fuelUsedText).text = "${formatFloat(trip.fuelUsed)} L"
        findViewById<TextView>(R.id.maxRpmText).text = "${trip.maxRPM}"
        findViewById<TextView>(R.id.avgRpmText).text = formatFloat(trip.avgRPM)

        // Calculate cost (using a default fuel price)
        val fuelPrice = 1.75 // Default price per liter in Euro
        val estimatedCost = trip.fuelUsed * fuelPrice
        findViewById<TextView>(R.id.estimatedCostText).text = getString(R.string.estimated_cost_format, estimatedCost)

        // Set feedback
        findViewById<TextView>(R.id.feedbackText).apply {
            visibility = View.VISIBLE
            text = feedback
        }
        findViewById<TextView>(R.id.feedbackTitle).apply {
            visibility = View.VISIBLE
            text = "Driving Feedback"
        }
    }

    /**
     * Returns a color based on the efficiency score
     */
    private fun getScoreColor(score: Int): Int {
        return when {
            score >= 85 -> getColor(android.R.color.holo_green_dark)
            score >= 70 -> getColor(android.R.color.holo_blue_dark)
            score >= 50 -> getColor(android.R.color.holo_orange_dark)
            else -> getColor(android.R.color.holo_red_dark)
        }
    }

    private fun formatFloat(value: Float): String {
        return String.format("%.1f", value)
    }
}