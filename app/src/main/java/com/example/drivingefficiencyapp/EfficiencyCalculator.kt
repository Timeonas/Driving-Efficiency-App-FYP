package com.example.drivingefficiencyapp

import com.example.drivingefficiencyapp.trip.Trip
import kotlin.math.max

/**
 * Utility class to calculate driving efficiency score based on various parameters
 */
class EfficiencyCalculator {

    companion object {
        // Constants for scoring weights - adjusted to be stricter
        private const val SPEED_WEIGHT = 0.30
        private const val RPM_WEIGHT = 0.35
        private const val FUEL_CONSUMPTION_WEIGHT = 0.35

        // Thresholds for ideal driving - stricter parameters
        private const val OPTIMAL_AVG_SPEED_MIN = 55f // km/h - city/highway mix
        private const val OPTIMAL_AVG_SPEED_MAX = 75f // km/h
        private const val OPTIMAL_MAX_RPM = 2200 // RPM - lower optimal RPM
        private const val OPTIMAL_AVG_RPM = 1500f // RPM - lower optimal RPM
        private const val EXCELLENT_FUEL_CONSUMPTION = 4.5f // L/100km - more demanding
        private const val POOR_FUEL_CONSUMPTION = 10.0f // L/100km - stricter threshold

        /**
         * Calculate the overall efficiency score based on trip data
         * @param trip The trip data
         * @return An efficiency score from 0-100
         */
        fun calculateEfficiencyScore(trip: Trip): Int {
            // Calculate individual component scores
            val speedScore = calculateSpeedScore(trip.averageSpeed)
            val rpmScore = calculateRpmScore(trip.maxRPM, trip.avgRPM)
            val fuelScore = calculateFuelScore(trip.averageFuelConsumption)

            // Weighted average for final score
            val overallScore = (speedScore * SPEED_WEIGHT +
                    rpmScore * RPM_WEIGHT +
                    fuelScore * FUEL_CONSUMPTION_WEIGHT)

            return overallScore.toInt()
        }

        /**
         * Calculate score based on average speed
         * More strictly penalizes inefficient speeds
         */
        private fun calculateSpeedScore(avgSpeed: Float): Float {
            return when {
                avgSpeed in OPTIMAL_AVG_SPEED_MIN..OPTIMAL_AVG_SPEED_MAX -> 100f
                avgSpeed < OPTIMAL_AVG_SPEED_MIN -> {
                    // More severe penalty for low speeds
                    max(40f, 60f + (avgSpeed / OPTIMAL_AVG_SPEED_MIN) * 40f)
                }
                else -> {
                    // More severe penalty for high speeds
                    max(40f, 100f - ((avgSpeed - OPTIMAL_AVG_SPEED_MAX) / 15f) * 60f)
                }
            }
        }

        /**
         * Calculate score based on engine RPM
         * More severely penalizes high RPM
         */
        private fun calculateRpmScore(maxRpm: Int, avgRpm: Float): Float {
            val maxRpmScore = when {
                maxRpm <= OPTIMAL_MAX_RPM -> 100f
                else -> {
                    // Stricter penalty for high max RPM
                    max(30f, 100f - ((maxRpm - OPTIMAL_MAX_RPM).toFloat() / 800f) * 70f)
                }
            }

            val avgRpmScore = when {
                avgRpm <= OPTIMAL_AVG_RPM -> 100f
                else -> {
                    // Stricter penalty for high average RPM
                    max(30f, 100f - ((avgRpm - OPTIMAL_AVG_RPM) / 600f) * 70f)
                }
            }

            // Combine the scores with more weight on average RPM
            return (maxRpmScore * 0.4f + avgRpmScore * 0.6f)
        }

        /**
         * Calculate score based on fuel consumption
         * More severely penalizes high consumption
         */
        private fun calculateFuelScore(fuelConsumption: Float): Float {
            return when {
                fuelConsumption <= EXCELLENT_FUEL_CONSUMPTION -> 100f
                fuelConsumption >= POOR_FUEL_CONSUMPTION -> 30f // Lower minimum score
                else -> {
                    // Steeper drop in score for higher consumption
                    val ratio = (fuelConsumption - EXCELLENT_FUEL_CONSUMPTION) /
                            (POOR_FUEL_CONSUMPTION - EXCELLENT_FUEL_CONSUMPTION)
                    100f - (ratio * 70f)
                }
            }
        }

        /**
         * Generate driving feedback based on the trip data and scores
         */
        fun generateFeedback(trip: Trip): String {
            val speedScore = calculateSpeedScore(trip.averageSpeed)
            val rpmScore = calculateRpmScore(trip.maxRPM, trip.avgRPM)
            val fuelScore = calculateFuelScore(trip.averageFuelConsumption)

            val feedback = StringBuilder()

            // Add positive feedback first
            if (speedScore >= 85) {
                feedback.append("Good job maintaining an efficient speed. ")
            }

            if (rpmScore >= 85) {
                feedback.append("You're keeping engine RPM in an efficient range. ")
            }

            if (fuelScore >= 85) {
                feedback.append("Excellent fuel consumption! ")
            }

            // Add improvement suggestions
            if (speedScore < 85) {
                if (trip.averageSpeed < OPTIMAL_AVG_SPEED_MIN) {
                    feedback.append("Try to maintain a steadier speed and avoid stop-and-go traffic when possible. ")
                } else {
                    feedback.append("Consider reducing your highway speed slightly for better efficiency. ")
                }
            }

            if (rpmScore < 85) {
                feedback.append("Try shifting earlier to keep RPM lower. Your engine is working too hard. ")
            }

            if (fuelScore < 85) {
                feedback.append("Your fuel consumption is very high. Focus on smoother acceleration and consistent speeds. ")
            }

            // Add severity indicator for very poor scores
            if ((speedScore + rpmScore + fuelScore) / 3 < 50) {
                feedback.append("\n\nYour driving shows significant inefficiencies that are costing you money and increasing emissions.")
            }

            return feedback.toString().trim()
        }
    }
}