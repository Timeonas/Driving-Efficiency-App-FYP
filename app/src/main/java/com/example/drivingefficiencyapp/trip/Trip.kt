package com.example.drivingefficiencyapp.trip

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data class representing a trip with a date and duration.
 * Includes a companion object to store all trips in memory.
 *
 * @author Tim Samoska
 * @since January 17, 2025
 */

data class Trip(
    @DocumentId
    val id: String = "",
    val date: String = "",
    val duration: String = "",
    @ServerTimestamp
    val timestamp: Long = 0L,
    val averageSpeed: Float = 0f,
    val distanceTraveled: Float = 0f,
    val averageFuelConsumption: Float = 0f,
    val fuelUsed: Float = 0f
)