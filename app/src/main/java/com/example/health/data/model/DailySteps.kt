package com.example.health.data.model

import java.time.LocalDate

/**
 * Data class representing daily steps data.
 * Each instance represents the total steps for a specific date.
 * 
 * @property date The date for which steps are recorded
 * @property steps Total number of steps for this date
 * @property startTime The earliest time when steps were recorded on this date (in Instant)
 * @property endTime The latest time when steps were recorded on this date (in Instant)
 */
data class DailySteps(
    val date: LocalDate,
    val steps: Long,
    val startTime: java.time.Instant,
    val endTime: java.time.Instant
) {
    /**
     * Formats the date as a string (YYYY-MM-DD).
     */
    fun getDateString(): String {
        return date.toString()
    }

    /**
     * Creates a summary string representation.
     */
    override fun toString(): String {
        return "Date: $date, Steps: $steps"
    }
}

