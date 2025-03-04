package com.example.drivingefficiencyapp

import com.example.drivingefficiencyapp.trip.Trip
import kotlin.math.max
import kotlin.math.min

/**
 * Utility class to calculate driving efficiency score based on various parameters
 */
class EfficiencyCalculator {

    companion object {
        // Constants for scoring weights
        private const val SPEED_WEIGHT = 0.30
        private const val RPM_WEIGHT = 0.35
        private const val FUEL_CONSUMPTION_WEIGHT = 0.35

        // Thresholds for ideal driving
        private const val OPTIMAL_AVG_SPEED_MIN = 55f // km/h - city/highway mix
        private const val OPTIMAL_AVG_SPEED_MAX = 75f // km/h
        private const val OPTIMAL_MAX_RPM = 2500 // RPM
        private const val OPTIMAL_AVG_RPM = 1600f // RPM
        private const val EXCELLENT_FUEL_CONSUMPTION = 4.5f // L/100km
        private const val POOR_FUEL_CONSUMPTION = 10.0f // L/100km

        // Maximum thresholds for worst scores
        private const val WORST_AVG_SPEED_LOW = 25f // km/h
        private const val WORST_AVG_SPEED_HIGH = 120f // km/h
        private const val WORST_MAX_RPM = 4500 // RPM
        private const val WORST_AVG_RPM = 3000f // RPM
        private const val WORST_FUEL_CONSUMPTION = 16.0f // L/100km

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
         */
        private fun calculateSpeedScore(avgSpeed: Float): Float {
            return when {
                avgSpeed in OPTIMAL_AVG_SPEED_MIN..OPTIMAL_AVG_SPEED_MAX -> 100f
                avgSpeed < OPTIMAL_AVG_SPEED_MIN -> {
                    if (avgSpeed <= WORST_AVG_SPEED_LOW) {
                        // Very inefficient low speed
                        10f
                    } else {
                        // Linear scaling between worst and optimal
                        val range = OPTIMAL_AVG_SPEED_MIN - WORST_AVG_SPEED_LOW
                        val position = avgSpeed - WORST_AVG_SPEED_LOW
                        10f + (position / range) * 90f
                    }
                }
                else -> {
                    if (avgSpeed >= WORST_AVG_SPEED_HIGH) {
                        // Very inefficient high speed
                        10f
                    } else {
                        // Linear scaling between optimal and worst
                        val range = WORST_AVG_SPEED_HIGH - OPTIMAL_AVG_SPEED_MAX
                        val position = avgSpeed - OPTIMAL_AVG_SPEED_MAX
                        max(10f, 100f - (position / range) * 90f)
                    }
                }
            }
        }

        /**
         * Calculate score based on engine RPM
         */
        private fun calculateRpmScore(maxRpm: Int, avgRpm: Float): Float {
            val maxRpmScore = when {
                maxRpm <= OPTIMAL_MAX_RPM -> 100f
                maxRpm >= WORST_MAX_RPM -> 10f
                else -> {
                    // Linear scaling between optimal and worst
                    val range = WORST_MAX_RPM - OPTIMAL_MAX_RPM
                    val position = maxRpm - OPTIMAL_MAX_RPM
                    max(10f, 100f - (position.toFloat() / range) * 90f)
                }
            }

            val avgRpmScore = when {
                avgRpm <= OPTIMAL_AVG_RPM -> 100f
                avgRpm >= WORST_AVG_RPM -> 10f
                else -> {
                    // Linear scaling between optimal and worst
                    val range = WORST_AVG_RPM - OPTIMAL_AVG_RPM
                    val position = avgRpm - OPTIMAL_AVG_RPM
                    max(10f, 100f - (position / range) * 90f)
                }
            }

            // Combine the scores with more weight on average RPM
            return (maxRpmScore * 0.4f + avgRpmScore * 0.6f)
        }

        /**
         * Calculate score based on fuel consumption
         */
        private fun calculateFuelScore(fuelConsumption: Float): Float {
            return when {
                fuelConsumption <= EXCELLENT_FUEL_CONSUMPTION -> 100f
                fuelConsumption >= WORST_FUEL_CONSUMPTION -> 10f
                fuelConsumption >= POOR_FUEL_CONSUMPTION -> {
                    // Linear scaling between poor and worst
                    val range = WORST_FUEL_CONSUMPTION - POOR_FUEL_CONSUMPTION
                    val position = fuelConsumption - POOR_FUEL_CONSUMPTION
                    val ratio = position / range
                    max(10f, 30f - (ratio * 20f))
                }
                else -> {
                    // Linear scaling between excellent and poor
                    val range = POOR_FUEL_CONSUMPTION - EXCELLENT_FUEL_CONSUMPTION
                    val position = fuelConsumption - EXCELLENT_FUEL_CONSUMPTION
                    100f - (position / range) * 70f
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
            val overallScore = (speedScore * SPEED_WEIGHT + rpmScore * RPM_WEIGHT + fuelScore * FUEL_CONSUMPTION_WEIGHT).toInt()

            val feedback = StringBuilder()

            // Add appropriate feedback based on severity
            if (overallScore < 30) {
                feedback.append("Your driving efficiency is critically low. ")
            } else if (overallScore < 50) {
                feedback.append("Your driving shows significant inefficiencies. ")
            }

            // Add positive feedback first (if any)
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
            if (speedScore < 60) {
                if (trip.averageSpeed < OPTIMAL_AVG_SPEED_MIN) {
                    feedback.append("Your average speed of ${trip.averageSpeed} km/h is too low, indicating frequent stops or traffic congestion. Try planning routes to avoid heavy traffic. ")
                } else {
                    feedback.append("Your average speed of ${trip.averageSpeed} km/h is inefficiently high. Reduce highway speed to around 70-80 km/h for better efficiency. ")
                }
            } else if (speedScore < 85) {
                if (trip.averageSpeed < OPTIMAL_AVG_SPEED_MIN) {
                    feedback.append("Try to maintain a steadier speed and avoid stop-and-go traffic when possible. ")
                } else {
                    feedback.append("Consider reducing your highway speed slightly for better efficiency. ")
                }
            }

            if (rpmScore < 60) {
                feedback.append("Your engine RPM is far too high (max: ${trip.maxRPM}, avg: ${trip.avgRPM}). Shift up earlier and accelerate more gently. ")
            } else if (rpmScore < 85) {
                feedback.append("Try shifting earlier to keep RPM lower. Your engine is working harder than optimal. ")
            }

            if (fuelScore < 60) {
                feedback.append("Your fuel consumption of ${trip.averageFuelConsumption} L/100km is extremely high. Focus on smoother acceleration, consistent speeds, and gentler braking. ")
            } else if (fuelScore < 85) {
                feedback.append("Your fuel consumption could be improved. Maintain steady speeds and anticipate traffic flow to reduce fuel usage. ")
            }

            // Add severity indicator for very poor scores
            if (overallScore < 30) {
                feedback.append("\n\nYour current driving style is significantly increasing fuel costs and emissions. Consider following eco-driving principles for substantial improvements.")
            }

            return feedback.toString().trim()
        }
    }
}