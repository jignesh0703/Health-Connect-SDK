package com.example.health.data.model

/**
 * Data class representing all historical health data.
 * This contains all fetched steps and heart rate data organized by type.
 * 
 * @property dailySteps List of daily steps, sorted by date (oldest first)
 * @property heartRateSamples List of all heart rate samples, sorted by time (oldest first)
 * @property totalSteps Total number of steps across all days
 * @property totalHeartRateSamples Total number of heart rate samples
 * @property earliestDate The earliest date in the data
 * @property latestDate The latest date in the data
 */
data class HistoricalHealthData(
    val dailySteps: List<DailySteps>,
    val heartRateSamples: List<HeartRateSample>,
    val totalSteps: Long,
    val totalHeartRateSamples: Int,
    val earliestDate: java.time.LocalDate?,
    val latestDate: java.time.LocalDate?
) {
    /**
     * Gets the total number of days with step data.
     */
    fun getDaysWithSteps(): Int = dailySteps.size

    /**
     * Gets the total number of days with heart rate data.
     */
    fun getDaysWithHeartRate(): Int {
        return if (heartRateSamples.isEmpty()) {
            0
        } else {
            heartRateSamples.map { it.getDate() }.distinct().size
        }
    }

    /**
     * Gets the date range as a string.
     */
    fun getDateRangeString(): String {
        return if (earliestDate != null && latestDate != null) {
            "$earliestDate to $latestDate"
        } else {
            "No data"
        }
    }
}

