package com.example.drivingefficiencyapp.modelLayer.trip

data class TripSummary(
    val averageSpeed: Float,
    val distanceTraveled: Float,
    val averageFuelConsumption: Float,
    val fuelUsed: Float,
    val tripDuration: String,
    val date: String,
    val maxRPM: Int = 0,
    val avgRPM: Float = 0f,
    var efficiencyScore: Int = 0
)
