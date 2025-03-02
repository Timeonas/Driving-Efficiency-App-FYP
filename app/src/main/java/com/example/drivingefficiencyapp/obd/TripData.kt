package com.example.drivingefficiencyapp.obd

data class TripData(
    val averageSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val distance: Float = 0f,
    val duration: Long = 0,
    val fuelUsed: Float = 0f,
    val averageFuelConsumption: Float = 0f,
    val averageRpm: Float = 0f,
    val maxRpm: Float = 0f
)
