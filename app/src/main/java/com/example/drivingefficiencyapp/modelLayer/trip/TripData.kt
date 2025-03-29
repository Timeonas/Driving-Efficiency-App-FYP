package com.example.drivingefficiencyapp.modelLayer.trip

data class TripData(
    val averageSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val distance: Float = 0f,
    val duration: Long = 0,
    val fuelUsed: Float = 0f,
    val averageFuelConsumption: Float = 0f,
    val avgRPM: Float = 0f,
    val maxRPM: Int = 0
)
