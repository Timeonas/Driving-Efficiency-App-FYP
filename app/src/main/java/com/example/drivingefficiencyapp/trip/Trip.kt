package com.example.drivingefficiencyapp.trip

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

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
    val timestamp: Date? = null
)