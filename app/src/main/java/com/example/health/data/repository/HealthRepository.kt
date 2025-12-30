package com.example.health.data.repository

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.health.data.config.HealthRecordTypes
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
    
    // We reuse the generic repository for fetching all other types
    private val genericRepository = GenericHealthRepository(healthClient)

    /**
     * Data class representing health data summary.
     */
    data class HealthDataSummary(
        val totalSteps: Long,
        val heartRateSamples: Int,
        val heartRateValues: List<Double>
    )

    /**
     * Fetches steps data from Health Connect for the last N days.
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

            return totalSteps
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching steps data", e)
            // Return 0 on error
            return 0L
        }
    }

    /**
     * Fetches heart rate data from Health Connect for the last N days.
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

            return Pair(heartRateSamples.size, heartRateValues)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching heart rate data", e)
            // Return empty pair on error
            return Pair(0, emptyList())
        }
    }

    /**
     * Fetches all health data (steps and heart rate) in a single operation.
     */
    suspend fun fetchAllHealthData(days: Int = 7): HealthDataSummary {
        Log.d(TAG, "Fetching all health data for last $days days")

        try {
            // Fetch steps and heart rate data
            val totalSteps = fetchSteps(days)
            val (heartRateSamples, heartRateValues) = fetchHeartRate(days)

            return HealthDataSummary(
                totalSteps = totalSteps,
                heartRateSamples = heartRateSamples,
                heartRateValues = heartRateValues
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all health data", e)
            // Return empty summary on error
            return HealthDataSummary(
                totalSteps = 0L,
                heartRateSamples = 0,
                heartRateValues = emptyList()
            )
        }
    }

    /**
     * Fetches ALL historical steps data.
     */
    suspend fun fetchAllHistoricalSteps(): List<DailySteps> {
        val startTime = HISTORICAL_START_DATE
        val endTime = Instant.now()

        try {
            val stepsResponse = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val dailyStepsMap = mutableMapOf<LocalDate, MutableList<StepsRecord>>()

            stepsResponse.records.forEach { record ->
                val date = record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                dailyStepsMap.getOrPut(date) { mutableListOf() }.add(record)
            }

            return dailyStepsMap.map { (date, records) ->
                val totalSteps = records.sumOf { it.count }
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
            }.sortedBy { it.date }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all historical steps data", e)
            // Return empty list on error
            return emptyList()
        }
    }

    /**
     * Fetches ALL historical heart rate data.
     */
    suspend fun fetchAllHistoricalHeartRate(): List<HeartRateSample> {
        val startTime = HISTORICAL_START_DATE
        val endTime = Instant.now()

        try {
            val heartRateResponse = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            return heartRateResponse.records.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateSample(
                        time = sample.time,
                        beatsPerMinute = sample.beatsPerMinute.toDouble(),
                        metadata = null
                    )
                }
            }.sortedBy { it.time }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all historical heart rate data", e)
            // Return empty list on error
            return emptyList()
        }
    }

    /**
     * Fetches ALL historical health data including steps, heart rate, and ALL other record types (Vitals, Sleep, etc.)
     */
    suspend fun fetchAllHistoricalData(): HistoricalHealthData {
        Log.d(TAG, "Fetching ALL historical health data (Steps, Heart Rate, and EVERYTHING else)")

        try {
            // Fetch basic data - these methods now handle their own exceptions and return empty lists
            val dailySteps = fetchAllHistoricalSteps()
            val heartRateSamples = fetchAllHistoricalHeartRate()

            // Fetch ALL other categories using GenericRepository
            val allOtherRecordsMap = mutableMapOf<String, List<Record>>()
            
            // Loop through all defined record types in HealthRecordTypes
            // We skip Steps and Heart Rate as we already fetched them specially, but it's okay to fetch them again or filter them out
            // For completeness, let's fetch everything available via GenericRepository too or just merge
            
            // fetchAllRecordsForAllCategories handles its own exceptions internally
            val allFetchedCategories = genericRepository.fetchAllRecordsForAllCategories(HISTORICAL_START_DATE, Instant.now())
            
            // Flatten the structure: Map<Category, Map<Type, FetchedRecords>> -> Map<Type, List<Record>>
            allFetchedCategories.values.forEach { categoryMap ->
                categoryMap.forEach { (typeName, fetchedRecords) ->
                    if (fetchedRecords.records.isNotEmpty()) {
                        allOtherRecordsMap[typeName] = fetchedRecords.records
                    }
                }
            }

            Log.d("All vitals", "All other records fetched: ${allOtherRecordsMap.keys}")

            // Calculate totals
            val totalSteps = dailySteps.sumOf { it.steps }
            val totalHeartRateSamples = heartRateSamples.size

            // Determine date range
            val allDates = mutableSetOf<LocalDate>()
            dailySteps.forEach { allDates.add(it.date) }
            heartRateSamples.forEach { allDates.add(it.getDate()) }
            // Add dates from other records? (Optional, might be expensive to parse all)

            val earliestDate = allDates.minOrNull()
            val latestDate = allDates.maxOrNull()

            return HistoricalHealthData(
                dailySteps = dailySteps,
                heartRateSamples = heartRateSamples,
                totalSteps = totalSteps,
                totalHeartRateSamples = totalHeartRateSamples,
                earliestDate = earliestDate,
                latestDate = latestDate,
                allOtherRecords = allOtherRecordsMap // Pass the full map of records
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all historical health data", e)
            // Return empty data on error
            return HistoricalHealthData(
                dailySteps = emptyList(),
                heartRateSamples = emptyList(),
                totalSteps = 0,
                totalHeartRateSamples = 0,
                earliestDate = null,
                latestDate = null,
                allOtherRecords = emptyMap()
            )
        }
    }
}
