package com.example.health.data.model

import androidx.health.connect.client.records.Record

/**
 * Data class representing all historical health data.
 * This contains all fetched steps and heart rate data organized by type, plus all other record types.
 * 
 * @property dailySteps List of daily steps, sorted by date (oldest first)
 * @property heartRateSamples List of all heart rate samples, sorted by time (oldest first)
 * @property totalSteps Total number of steps across all days
 * @property totalHeartRateSamples Total number of heart rate samples
 * @property earliestDate The earliest date in the data
 * @property latestDate The latest date in the data
 * @property allOtherRecords Map of record type name to list of records (e.g. "Blood Glucose" -> [Record1, Record2])
 */
data class HistoricalHealthData(
    val dailySteps: List<DailySteps>,
    val heartRateSamples: List<HeartRateSample>,
    val totalSteps: Long,
    val totalHeartRateSamples: Int,
    val earliestDate: java.time.LocalDate?,
    val latestDate: java.time.LocalDate?,
    // New field to hold all other fetched records (Vitals, Sleep, etc.)
    val allOtherRecords: Map<String, List<Record>> = emptyMap()
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
