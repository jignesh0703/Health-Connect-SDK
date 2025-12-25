package com.example.health.data.model

import java.time.Instant

/**
 * Data class representing a single heart rate sample.
 * Each instance represents one heart rate measurement at a specific time.
 * 
 * @property time The timestamp when the heart rate was measured
 * @property beatsPerMinute The heart rate value in beats per minute
 * @property metadata Optional metadata associated with the sample
 */
data class HeartRateSample(
    val time: Instant,
    val beatsPerMinute: Double,
    val metadata: Map<String, String>? = null
) {
    /**
     * Gets the date this sample was recorded.
     */
    fun getDate(): java.time.LocalDate {
        return time.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }

    /**
     * Formats the timestamp as a readable string.
     */
    fun getTimeString(): String {
        return time.atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    /**
     * Creates a summary string representation.
     */
    override fun toString(): String {
        return "Time: $time, Heart Rate: ${beatsPerMinute.toInt()} bpm"
    }
}

