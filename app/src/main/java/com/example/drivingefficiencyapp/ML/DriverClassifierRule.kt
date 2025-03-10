package com.example.drivingefficiencyapp.ML
import android.util.Log
import com.example.drivingefficiencyapp.trip.Trip

class DriverClassifierRule {
    companion object {
        private const val TAG = "ML"
    }

    fun classifyDriver(trips: List<Trip>): DriverCategory {
        Log.d(TAG, "Rule-based classification starting on ${trips.size} trips")

        if (trips.size < 3) {
            Log.d(TAG, "Insufficient trips for accurate classification, defaulting to BALANCED")
            return DriverCategory.BALANCED
        }

        val avgFuelConsumption = trips.map { it.averageFuelConsumption }.average()
        val avgRPM = trips.map { it.avgRPM }.average()
        val maxRPMAvg = trips.map { it.maxRPM }.average()
        val avgSpeed = trips.map { it.averageSpeed }.average()
        val avgEfficiencyScore = trips.map { it.efficiencyScore }.average()

        Log.d(TAG, "Trip metrics: avgFuel=$avgFuelConsumption, avgRPM=$avgRPM, " +
                "maxRPMAvg=$maxRPMAvg, avgSpeed=$avgSpeed, avgScore=$avgEfficiencyScore")

        val category = when {
            avgEfficiencyScore >= 85 && avgFuelConsumption < 5.5 && avgRPM < 2000 &&
                    avgSpeed > 45 && avgSpeed < 80 -> DriverCategory.ECO_FRIENDLY

            avgEfficiencyScore >= 70 && avgFuelConsumption < 6.5 && avgRPM < 2300 &&
                    avgSpeed > 40 && avgSpeed < 85 -> DriverCategory.ECO_FRIENDLY

            avgEfficiencyScore >= 55 && avgFuelConsumption < 8.0 && avgRPM < 2600 -> DriverCategory.BALANCED

            avgEfficiencyScore < 55 || (avgFuelConsumption > 8.0 && maxRPMAvg > 4500) ||
                    avgSpeed > 90 -> DriverCategory.AGGRESSIVE

            else -> DriverCategory.MODERATE
        }

        Log.d(TAG, "Rule-based classification result: ${category.label}")
        return category
    }

    // Get personalized feedback based on category and trip data
    fun getPersonalizedFeedback(category: DriverCategory, trip: Trip): String {
        Log.d(TAG, "Generating personalized feedback for category: ${category.label}")

        return when(category) {
            DriverCategory.ECO_FRIENDLY -> {
                Log.d(TAG, "Feedback for eco-friendly driver")
                "Excellent driving efficiency! You're maximizing fuel economy with your gentle acceleration and optimal RPM range."
            }

            DriverCategory.BALANCED -> {
                val feedback = if (trip.maxRPM > 4000) {
                    Log.d(TAG, "Feedback for balanced driver with high RPM peaks: ${trip.maxRPM}")
                    "Your driving is reasonably efficient, but try to avoid high RPM peaks to improve fuel economy."
                } else if (trip.averageFuelConsumption > 7.0) {
                    Log.d(TAG, "Feedback for balanced driver with high fuel consumption: ${trip.averageFuelConsumption}")
                    "Consider gentler acceleration and maintaining more consistent speeds to reduce fuel consumption."
                } else {
                    Log.d(TAG, "General feedback for balanced driver")
                    "You have balanced driving habits. Small improvements in acceleration could boost efficiency."
                }
                feedback
            }

            DriverCategory.MODERATE -> {
                Log.d(TAG, "Feedback for moderate/inconsistent driver")
                "Your driving patterns vary considerably. Focus on maintaining steady speeds and consistent acceleration."
            }

            DriverCategory.AGGRESSIVE -> {
                val feedback = if (trip.maxRPM > 4500) {
                    Log.d(TAG, "Feedback for aggressive driver with very high RPM: ${trip.maxRPM}")
                    "High RPM driving is significantly increasing your fuel consumption. Try shifting earlier."
                } else {
                    Log.d(TAG, "General feedback for aggressive driver")
                    "Your driving style tends to be aggressive. Smoother acceleration and deceleration would improve efficiency."
                }
                feedback
            }
        }
    }
}
