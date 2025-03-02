package com.example.drivingefficiencyapp.trip

data class TripSummary(
    val averageSpeed: Float,
    val distanceTraveled: Float,
    val averageFuelConsumption: Float,
    val fuelUsed: Float,
    val tripDuration: String,
    val date: String
)
