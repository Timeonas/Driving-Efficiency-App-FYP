package com.example.drivingefficiencyapp

/**
 * Data class representing a trip with a date and duration.
 * Includes a companion object to store all trips in memory.
 *
 * @author Tim Samoska
 * @since January 17, 2025
 */

data class Trip(
    val date: String,
    val duration: String
) {
    //Companion object to store all trips in memory, can be accessed statically from Trip class
    companion object {
        /**
         * Static list to store all trips in memory.
         * temporary solution as trips will be lost when the app closes.
         */
        val tripsList = mutableListOf<Trip>()
    }
}