package com.example.health.data.repository

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.health.data.model.DailySteps
import com.example.health.data.model.HeartRateSample
import com.example.health.data.model.HistoricalHealthData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Repository class for handling Health Connect data operations.
 * This class encapsulates all data access logic related to Health Connect records.
 * 
 * @property healthClient The HealthConnectClient instance used to read health data
 */
class HealthRepository(
    private val healthClient: HealthConnectClient
) {
    companion object {
        private const val TAG = "HealthRepository"
        
        // Start date for fetching all historical data (January 1, 2020)
        // Health Connect was released around 2022, but using 2020 as a safe early date
        private val HISTORICAL_START_DATE = ZonedDateTime.of(
            2020, 1, 1, 0, 0, 0, 0,
            ZoneId.systemDefault()
        ).toInstant()
    }

    /**
     * Data class representing health data summary.
     * 
     * @property totalSteps Total number of steps in the specified time range
     * @property heartRateSamples Number of heart rate samples found
     * @property heartRateValues List of heart rate values (bpm) for calculation
     */
    data class HealthDataSummary(
        val totalSteps: Long,
        val heartRateSamples: Int,
        val heartRateValues: List<Double>
    )

    /**
     * Fetches steps data from Health Connect for the last N days.
     * 
     * @param days Number of days to look back (default: 7)
     * @return Total number of steps
     * @throws Exception if the operation fails
     */
    suspend fun fetchSteps(days: Int = 7): Long {
        val startTime = ZonedDateTime.now().minusDays(days.toLong()).toInstant()
        val endTime = Instant.now()

        Log.d(TAG, "Fetching steps data from $startTime to $endTime")

        try {
            val stepsResponse = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val totalSteps = stepsResponse.records.sumOf { it.count }
            Log.d(TAG, "Steps records found: ${stepsResponse.records.size}, Total steps: $totalSteps")

            // Log individual step records for debugging
            stepsResponse.records.forEachIndexed { index, record ->
                Log.d(TAG, "Step Record $index: ${record.count} steps at ${record.startTime}")
            }

            return totalSteps
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching steps data", e)
            throw e
        }
    }

    /**
     * Fetches heart rate data from Health Connect for the last N days.
     * 
     * @param days Number of days to look back (default: 7)
     * @return Pair of (number of samples, list of heart rate values in bpm)
     * @throws Exception if the operation fails
     */
    suspend fun fetchHeartRate(days: Int = 7): Pair<Int, List<Double>> {
        val startTime = ZonedDateTime.now().minusDays(days.toLong()).toInstant()
        val endTime = Instant.now()

        Log.d(TAG, "Fetching heart rate data from $startTime to $endTime")

        try {
            val heartRateResponse = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            // Flatten all samples from all records
            val heartRateSamples = heartRateResponse.records.flatMap { it.samples }
            val heartRateValues = heartRateSamples.map { it.beatsPerMinute.toDouble() }

            Log.d(TAG, "Heart rate records found: ${heartRateResponse.records.size}")
            Log.d(TAG, "Total heart rate samples: ${heartRateSamples.size}")

            // Log individual heart rate records for debugging
            heartRateResponse.records.forEachIndexed { index, record ->
                Log.d(TAG, "Heart Rate Record $index: ${record.samples.size} samples")
                record.samples.forEach { sample ->
                    Log.d(TAG, "  - ${sample.beatsPerMinute} bpm at ${sample.time}")
                }
            }

            return Pair(heartRateSamples.size, heartRateValues)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching heart rate data", e)
            throw e
        }
    }

    /**
     * Fetches all health data (steps and heart rate) in a single operation.
     * This method fetches both types of data concurrently for better performance.
     * 
     * @param days Number of days to look back (default: 7)
     * @return HealthDataSummary containing all fetched data
     * @throws Exception if any operation fails
     */
    suspend fun fetchAllHealthData(days: Int = 7): HealthDataSummary {
        Log.d(TAG, "Fetching all health data for last $days days")

        try {
            // Fetch steps and heart rate data
            val totalSteps = fetchSteps(days)
            val (heartRateSamples, heartRateValues) = fetchHeartRate(days)

            // Log summary
            Log.d(TAG, "=".repeat(50))
            Log.d(TAG, "HEALTH DATA SUMMARY")
            Log.d(TAG, "=".repeat(50))
            Log.d(TAG, "Total Steps (Last $days Days): $totalSteps")
            Log.d(TAG, "Total Heart Rate Samples: $heartRateSamples")
            if (heartRateValues.isNotEmpty()) {
                Log.d(TAG, "Average Heart Rate: ${heartRateValues.average().toInt()} bpm")
                Log.d(TAG, "Min Heart Rate: ${heartRateValues.minOrNull()?.toInt()} bpm")
                Log.d(TAG, "Max Heart Rate: ${heartRateValues.maxOrNull()?.toInt()} bpm")
            }
            Log.d(TAG, "=".repeat(50))

            return HealthDataSummary(
                totalSteps = totalSteps,
                heartRateSamples = heartRateSamples,
                heartRateValues = heartRateValues
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all health data", e)
            throw e
        }
    }

    /**
     * Fetches ALL historical steps data from Health Connect from the beginning of records.
     * Steps are grouped by day, with each day's total steps calculated.
     * 
     * @return List of DailySteps, sorted by date (oldest first)
     * @throws Exception if the operation fails
     */
    suspend fun fetchAllHistoricalSteps(): List<DailySteps> {
        val startTime = HISTORICAL_START_DATE
        val endTime = Instant.now()

        Log.d(TAG, "Fetching ALL historical steps data from $startTime to $endTime")

        try {
            // Fetch all steps records
            val stepsResponse = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            Log.d(TAG, "Total steps records found: ${stepsResponse.records.size}")

            // Group steps by day
            val dailyStepsMap = mutableMapOf<LocalDate, MutableList<StepsRecord>>()

            stepsResponse.records.forEach { record ->
                // Get the date from the record's start time
                val date = record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                dailyStepsMap.getOrPut(date) { mutableListOf() }.add(record)
            }

            // Convert map to list of DailySteps
            val dailySteps = dailyStepsMap.map { (date, records) ->
                // Calculate total steps for the day
                val totalSteps = records.sumOf { it.count }
                
                // Find the earliest and latest time for this day
                val startTimes = records.map { it.startTime }
                val endTimes = records.map { it.endTime }
                val dayStartTime = startTimes.minOrNull() ?: Instant.EPOCH
                val dayEndTime = endTimes.maxOrNull() ?: Instant.EPOCH

                DailySteps(
                    date = date,
                    steps = totalSteps,
                    startTime = dayStartTime,
                    endTime = dayEndTime
                )
            }.sortedBy { it.date } // Sort by date (oldest first)

            Log.d(TAG, "Steps grouped into ${dailySteps.size} days")
            Log.d(TAG, "Date range: ${dailySteps.firstOrNull()?.date} to ${dailySteps.lastOrNull()?.date}")

            return dailySteps
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all historical steps data", e)
            throw e
        }
    }

    /**
     * Fetches ALL historical heart rate data from Health Connect from the beginning of records.
     * All heart rate samples are returned with their timestamps.
     * 
     * @return List of HeartRateSample, sorted by time (oldest first)
     * @throws Exception if the operation fails
     */
    suspend fun fetchAllHistoricalHeartRate(): List<HeartRateSample> {
        val startTime = HISTORICAL_START_DATE
        val endTime = Instant.now()

        Log.d(TAG, "Fetching ALL historical heart rate data from $startTime to $endTime")

        try {
            // Fetch all heart rate records
            val heartRateResponse = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            Log.d(TAG, "Total heart rate records found: ${heartRateResponse.records.size}")

            // Flatten all samples from all records into HeartRateSample objects
            val heartRateSamples = heartRateResponse.records.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateSample(
                        time = sample.time,
                        beatsPerMinute = sample.beatsPerMinute.toDouble(),
                        metadata = null // Metadata can be added here if needed in the future
                    )
                }
            }.sortedBy { it.time } // Sort by time (oldest first)

            Log.d(TAG, "Total heart rate samples found: ${heartRateSamples.size}")
            
            if (heartRateSamples.isNotEmpty()) {
                val earliestSample = heartRateSamples.first()
                val latestSample = heartRateSamples.last()
                Log.d(TAG, "Sample range: ${earliestSample.time} to ${latestSample.time}")
            }

            return heartRateSamples
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all historical heart rate data", e)
            throw e
        }
    }

    /**
     * Fetches ALL historical health data (both steps and heart rate) from the beginning.
     * This method fetches all available data and organizes it into daily steps and heart rate samples.
     * 
     * @return HistoricalHealthData containing all fetched data organized by type
     * @throws Exception if any operation fails
     */
    suspend fun fetchAllHistoricalData(): HistoricalHealthData {
        Log.d(TAG, "Fetching ALL historical health data from the beginning")

        try {
            // Fetch all steps and heart rate data
            val dailySteps = fetchAllHistoricalSteps()
            val heartRateSamples = fetchAllHistoricalHeartRate()

            // Calculate totals
            val totalSteps = dailySteps.sumOf { it.steps }
            val totalHeartRateSamples = heartRateSamples.size

            // Determine date range
            val allDates = mutableSetOf<LocalDate>()
            dailySteps.forEach { allDates.add(it.date) }
            heartRateSamples.forEach { allDates.add(it.getDate()) }

            val earliestDate = allDates.minOrNull()
            val latestDate = allDates.maxOrNull()

            // Log summary
            Log.d(TAG, "=".repeat(60))
            Log.d(TAG, "ALL HISTORICAL HEALTH DATA SUMMARY")
            Log.d(TAG, "=".repeat(60))
            Log.d(TAG, "Total Days with Steps: ${dailySteps.size}")
            Log.d(TAG, "Total Steps: $totalSteps")
            Log.d(TAG, "Total Heart Rate Samples: $totalHeartRateSamples")
            Log.d(TAG, "Days with Heart Rate: ${heartRateSamples.map { it.getDate() }.distinct().size}")
            Log.d(TAG, "Date Range: ${earliestDate ?: "N/A"} to ${latestDate ?: "N/A"}")
            if (heartRateSamples.isNotEmpty()) {
                val avgHeartRate = heartRateSamples.map { it.beatsPerMinute }.average()
                Log.d(TAG, "Average Heart Rate: ${avgHeartRate.toInt()} bpm")
            }
            Log.d(TAG, "=".repeat(60))

            return HistoricalHealthData(
                dailySteps = dailySteps,
                heartRateSamples = heartRateSamples,
                totalSteps = totalSteps,
                totalHeartRateSamples = totalHeartRateSamples,
                earliestDate = earliestDate,
                latestDate = latestDate
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all historical health data", e)
            throw e
        }
    }
}

