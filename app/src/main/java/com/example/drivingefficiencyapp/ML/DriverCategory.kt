package com.example.drivingefficiencyapp.ML

/**
 * Enum representing different driver categories based on driving behavior analysis.
 *
 * @property label Human-readable label for the category
 * @property description Detailed description of the driving behavior category
 */
enum class DriverCategory(val label: String, val description: String) {
    ECO_FRIENDLY(
        "Eco-Friendly Driver",
        "You maintain steady speeds, accelerate gently, and plan ahead to minimize fuel consumption."
    ),

    BALANCED(
        "Balanced Driver",
        "Your driving shows a good balance between efficiency and performance with room for minor improvements."
    ),

    MODERATE(
        "Moderate Driver",
        "Your driving is acceptable but could benefit from smoother acceleration and more consistent speeds."
    ),

    AGGRESSIVE(
        "Aggressive Driver",
        "Your driving style shows frequent rapid acceleration, hard braking, and inconsistent speeds."
    );

    companion object {
        // Factory method to create category from string
        fun fromString(label: String): DriverCategory {
            return values().find { it.label.equals(label, ignoreCase = true) } ?: MODERATE
        }
    }
}