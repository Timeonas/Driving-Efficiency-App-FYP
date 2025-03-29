package com.example.drivingefficiencyapp.modelLayer.obd

class GearCalculator(
    wheelDiameter: Double = 0.6096,
    private val finalDriveRatioI: Double = 3.68,  // For gears 1-4
    private val finalDriveRatioII: Double = 2.92, // For gears 5-6
    private val idleRpmUpperThreshold: Int = 1000,
    private val speedThreshold: Int = 3,
    private val rpmChangeThreshold: Int = 200,
    private val gearRatios: Map<Int, Double> = mapOf(
        1 to 3.77,
        2 to 1.96,
        3 to 1.26,
        4 to 0.87,
        5 to 0.86,
        6 to 0.72
    )
) {
    private val wheelCircumference = Math.PI * wheelDiameter
    private val secondsPerMinute = 60.0
    private val kmhConversionFactor = 3.6

    private var lastRpm = 0
    private var lastSpeed = 0.0
    private var neutralCounter = 0

    fun calculateGear(rpm: Int, speedKmh: Double): String {
        // First check if engine is off
        if (rpm <= 0) return "-"

        // Then check for neutral
        if (isNeutral(rpm, speedKmh)) {
            return "N"
        } else {
            neutralCounter = 0 // Reset counter when not in neutral
        }

        // If speed is 0 but engine is running, we're in neutral
        if (speedKmh <= 0.0) return "N"

        // Calculate theoretical speed for each gear
        val gearSpeeds = gearRatios.mapValues { (gear, ratio) ->
            val finalDrive = if (gear <= 4) finalDriveRatioI else finalDriveRatioII
            (rpm * wheelCircumference * kmhConversionFactor) / (ratio * finalDrive * secondsPerMinute)
        }

        // Find the gear with the closest calculated speed to actual speed
        val gear = gearSpeeds.entries.minByOrNull { (_, calculatedSpeed) ->
            Math.abs(calculatedSpeed - speedKmh)
        }?.key

        lastRpm = rpm
        lastSpeed = speedKmh

        return gear?.toString() ?: "N" // Return "N" if no gear is found
    }
    private fun isNeutral(rpm: Int, speedKmh: Double): Boolean {
        if (rpm in 600..idleRpmUpperThreshold && speedKmh < speedThreshold) {
            neutralCounter++
            return neutralCounter >= 2
        }

        val rpmDelta = Math.abs(rpm - lastRpm)
        val speedDelta = Math.abs(speedKmh - lastSpeed)
        // Clutch-in scenario. Keep returning neutral
        if (rpmDelta > rpmChangeThreshold && speedDelta < speedThreshold && speedKmh < speedThreshold) {
            neutralCounter++ // Keep incrementing to avoid flapping
            return true
        }

        return false
    }
}