package com.example.health.data.repository

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.health.data.config.HealthRecordTypes
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.reflect.KClass

/**
 * Generic repository for fetching all types of health records.
 * This class provides a simple way to fetch all health data using loops.
 */
class GenericHealthRepository(
    private val healthClient: HealthConnectClient
) {
    companion object {
        private const val TAG = "GenericHealthRepository"
        
        // Start date for fetching all historical data (January 1, 2020)
        private val HISTORICAL_START_DATE = ZonedDateTime.of(
            2020, 1, 1, 0, 0, 0, 0,
            ZoneId.systemDefault()
        ).toInstant()
    }

    /**
     * Data class representing fetched records for a specific type.
     * 
     * @property recordTypeConfig The configuration for this record type
     * @property records The list of fetched records
     * @property count Total number of records fetched
     */
    data class FetchedRecords(
        val recordTypeConfig: HealthRecordTypes.RecordTypeConfig,
        val records: List<Record>,
        val count: Int
    )

    /**
     * Fetches all historical records for a specific record type.
     * 
     * @param recordClass The record class type
     * @return List of records of the specified type, sorted by start time (oldest first)
     */
    suspend fun <T : Record> fetchRecordsForType(recordClass: KClass<T>): List<T> {
        val startTime = HISTORICAL_START_DATE
        val endTime = Instant.now()

        return try {
            Log.d(TAG, "Fetching records for type: ${recordClass.simpleName}")

            val response = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = recordClass,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val records = response.records
            Log.d(TAG, "Fetched ${records.size} records for type: ${recordClass.simpleName}")

            records
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching records for type: ${recordClass.simpleName}", e)
            emptyList() // Return empty list on error to allow other records to be fetched
        }
    }

    /**
     * Helper function to fetch records using reflection/inline reified generics.
     * This works around the type erasure limitation.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun fetchRecordsForTypeUnsafe(
        recordClass: KClass<out Record>,
        startTime: Instant = HISTORICAL_START_DATE,
        endTime: Instant = Instant.now()
    ): List<Record> {
        return try {
            // Use a type-safe wrapper by calling readRecords with the correct type
            // Since we can't use reified generics in a suspend function parameter,
            // we'll use a workaround with reflection
            val response = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = recordClass as KClass<Record>,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val records = response.records
            val recordCount = records.size
            
            // Log clearly whether data was found or not
            if (recordCount > 0) {
                Log.d(TAG, "  Fetched $recordCount records for ${recordClass.simpleName} ($startTime to $endTime)")
                Log.d(TAG, "    Sample data (first record): ${records.first()}")
            }

            records
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching ${recordClass.simpleName}: ${e.message} (returning 0)")
            emptyList() // Return empty list (0 records) on error
        }
    }

    /**
     * Fetches all historical records for all record types in a category.
     * Ensures ALL record types are included, even if they have 0 records.
     * 
     * @param category The category name (e.g., "Activity", "Vitals")
     * @param startTime Start time for fetching records
     * @param endTime End time for fetching records
     * @return Map of record type display name to fetched records (all types included, 0 if no data)
     */
    suspend fun fetchAllRecordsForCategory(
        category: String,
        startTime: Instant = HISTORICAL_START_DATE,
        endTime: Instant = Instant.now()
    ): Map<String, FetchedRecords> {
        Log.d(TAG, "Fetching all records for category: $category")

        val recordTypes = HealthRecordTypes.getRecordsByCategory(category)
        val fetchedRecords = mutableMapOf<String, FetchedRecords>()

        // Loop through all record types in the category - ensure ALL are included
        for (recordConfig in recordTypes) {
            try {
                val records = fetchRecordsForTypeUnsafe(recordConfig.recordClass, startTime, endTime)
                val recordCount = records.size
                
                // Always include the record type, even if count is 0
                fetchedRecords[recordConfig.displayName] = FetchedRecords(
                    recordTypeConfig = recordConfig,
                    records = records,
                    count = recordCount
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching records for ${recordConfig.displayName}: ${e.message}")
                // Even on error, include the record type with 0 count to show it was attempted
                fetchedRecords[recordConfig.displayName] = FetchedRecords(
                    recordTypeConfig = recordConfig,
                    records = emptyList(),
                    count = 0
                )
            }
        }

        val totalRecords = fetchedRecords.values.sumOf { it.count }
        val typesWithData = fetchedRecords.values.count { it.count > 0 }
        
        Log.d(TAG, "Category '$category' complete: $totalRecords total records across $typesWithData types")
        
        return fetchedRecords
    }

    /**
     * Fetches all historical records for ALL categories and record types.
     * This is the main method to fetch everything.
     * 
     * @param startTime Start time for fetching records (optional, defaults to historical start)
     * @param endTime End time for fetching records (optional, defaults to now)
     * @return Map of category name to map of record type display name to fetched records
     */
    suspend fun fetchAllRecordsForAllCategories(
        startTime: Instant = HISTORICAL_START_DATE,
        endTime: Instant = Instant.now()
    ): Map<String, Map<String, FetchedRecords>> {
        Log.d(TAG, "Starting to fetch ALL health records for ALL categories from $startTime to $endTime")
        val fetchStartTime = System.currentTimeMillis()

        val allFetchedRecords = mutableMapOf<String, Map<String, FetchedRecords>>()

        // Loop through all categories - ensure ALL categories are included
        for ((category, _) in HealthRecordTypes.ALL_RECORDS_BY_CATEGORY) {
            try {
                // Use fetchAllRecordsForCategory which internally fetches correct types
                val categoryRecords = fetchAllRecordsForCategory(category, startTime, endTime)
                allFetchedRecords[category] = categoryRecords
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching category: $category", e)
                // Even on error, create empty map for this category to show it was attempted
                allFetchedRecords[category] = emptyMap()
            }
        }

        val fetchEndTime = System.currentTimeMillis()
        val duration = (fetchEndTime - fetchStartTime) / 1000.0
        
        val totalRecords = allFetchedRecords.values.sumOf { map -> 
             map.values.sumOf { it.count } 
        }

        Log.d(TAG, "=".repeat(60))
        Log.d(TAG, "ALL HEALTH DATA FETCH COMPLETE")
        Log.d(TAG, "Total categories: ${allFetchedRecords.size}")
        Log.d(TAG, "Total record types: ${allFetchedRecords.values.sumOf { it.size }}")
        Log.d(TAG, "Total records fetched: $totalRecords")
        Log.d(TAG, "Time taken: ${duration}s")
        Log.d(TAG, "=".repeat(60))

        return allFetchedRecords
    }

    /**
     * Gets summary statistics for fetched records.
     * 
     * @param fetchedRecords Map of fetched records by category and type
     * @return Map of category to summary statistics
     */
    fun getSummaryStatistics(
        fetchedRecords: Map<String, Map<String, FetchedRecords>>
    ): Map<String, CategorySummary> {
        return fetchedRecords.mapValues { (category, records) ->
            val totalTypes = records.size
            val totalRecords = records.values.sumOf { it.count }
            val typesWithData = records.values.count { it.count > 0 }
            val typesWithoutData = totalTypes - typesWithData

            CategorySummary(
                category = category,
                totalTypes = totalTypes,
                totalRecords = totalRecords,
                typesWithData = typesWithData,
                typesWithoutData = typesWithoutData
            )
        }
    }

    /**
     * Data class representing summary statistics for a category.
     */
    data class CategorySummary(
        val category: String,
        val totalTypes: Int,
        val totalRecords: Int,
        val typesWithData: Int,
        val typesWithoutData: Int
    )
}
