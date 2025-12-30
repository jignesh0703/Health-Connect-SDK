package com.example.health.data.export

import android.util.Log
import com.example.health.data.repository.GenericHealthRepository.FetchedRecords
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Generic exporter for all health record types.
 * Exports data to JSON files organized by category.
 */
object GenericHealthDataExporter {
    private const val TAG = "GenericHealthDataExporter"

    /**
     * Exports fetched records for a single record type to JSON.
     * Uses reflection to extract common fields from records.
     * 
     * @param fetchedRecords The fetched records to export
     * @return JSON string representation
     */
    fun exportRecordTypeToJson(fetchedRecords: FetchedRecords): String {
        val jsonArray = JSONArray()

        fetchedRecords.records.forEach { record ->
            val jsonObject = JSONObject()
            
            // Add common fields using reflection
            try {
                // Try to get startTime (most records have this)
                val startTimeMethod = record.javaClass.getMethod("getStartTime")
                val startTime = startTimeMethod.invoke(record)
                jsonObject.put("startTime", startTime.toString())
            } catch (e: Exception) {
                // Some records might not have startTime
            }

            try {
                // Try to get endTime
                val endTimeMethod = record.javaClass.getMethod("getEndTime")
                val endTime = endTimeMethod.invoke(record)
                jsonObject.put("endTime", endTime.toString())
            } catch (e: Exception) {
                // Some records might not have endTime
            }

            try {
                // Try to get metadata
                val metadataMethod = record.javaClass.getMethod("getMetadata")
                val metadata = metadataMethod.invoke(record)
                if (metadata != null) {
                    // Metadata is complex, just add a placeholder
                    jsonObject.put("hasMetadata", true)
                }
            } catch (e: Exception) {
                // Metadata might not be accessible
            }

            // Add record-specific data using toString for now
            // In a production app, you'd want to properly serialize each record type
            jsonObject.put("recordType", record.javaClass.simpleName)
            jsonObject.put("recordData", record.toString())

            jsonArray.put(jsonObject)
        }

        val result = JSONObject().apply {
            put("recordType", fetchedRecords.recordTypeConfig.displayName)
            put("category", fetchedRecords.recordTypeConfig.category)
            put("totalRecords", fetchedRecords.count)
            put("records", jsonArray)
        }

        return result.toString(2)
    }

    /**
     * Exports all records for a category to a single JSON file.
     * 
     * @param categoryRecords Map of record type display name to fetched records
     * @param category The category name
     * @return JSON string containing all record types for the category
     */
    fun exportCategoryToJson(
        categoryRecords: Map<String, FetchedRecords>,
        category: String
    ): String {
        val categoryJson = JSONObject()
        val recordTypesJson = JSONObject()

        var totalRecordsInCategory = 0

        categoryRecords.forEach { (displayName, fetchedRecords) ->
            try {
                val recordTypeJson = JSONObject().apply {
                    put("displayName", fetchedRecords.recordTypeConfig.displayName)
                    put("category", fetchedRecords.recordTypeConfig.category)
                    put("totalRecords", fetchedRecords.count) // Will be 0 if no data
                    put("hasData", fetchedRecords.count > 0) // Explicit flag for whether data exists
                    put("isPermissionDenied", fetchedRecords.isPermissionDenied) // Track permission status
                    
                    // Create records array (will be empty array if count is 0)
                    val recordsArray = JSONArray()
                    
                    // ALWAYS include records, even if count is 0 (it will just be an empty array)
                    // If fetchedRecords.count > 0, we populate it
                    if (fetchedRecords.count > 0) {
                        fetchedRecords.records.forEach { record ->
                            try {
                                val recordJson = JSONObject().apply {
                                    put("recordType", record.javaClass.simpleName)
                                    
                                    // Try to extract common fields - handle errors gracefully
                                    try {
                                        val startTimeMethod = record.javaClass.getMethod("getStartTime")
                                        val startTime = startTimeMethod.invoke(record)
                                        put("startTime", startTime.toString())
                                    } catch (e: Exception) {
                                        // Method doesn't exist or error - skip this field
                                        Log.d(TAG, "Could not extract startTime for ${record.javaClass.simpleName}")
                                    }
                                    
                                    try {
                                        val endTimeMethod = record.javaClass.getMethod("getEndTime")
                                        val endTime = endTimeMethod.invoke(record)
                                        put("endTime", endTime.toString())
                                    } catch (e: Exception) {
                                        // Method doesn't exist or error - skip this field
                                        Log.d(TAG, "Could not extract endTime for ${record.javaClass.simpleName}")
                                    }
                                    
                                    // Add full record data as string (can be improved with proper serialization)
                                    put("data", record.toString())
                                }
                                recordsArray.put(recordJson)
                            } catch (e: Exception) {
                                // If individual record export fails, log and continue with next record
                                Log.w(TAG, "Error exporting individual record for $displayName: ${e.message} - continuing")
                                // Continue to next record - don't fail entire record type
                            }
                        }
                    }
                    // If count is 0, records array will be empty []
                    put("records", recordsArray)
                }
                
                recordTypesJson.put(displayName, recordTypeJson)
                totalRecordsInCategory += fetchedRecords.count
            } catch (e: Exception) {
                // If exporting a record type fails, log and continue with other types
                Log.e(TAG, "Error exporting record type '$displayName' in category '$category': ${e.message} - continuing", e)
                // Continue to next record type - don't fail entire category export
            }
        }

        categoryJson.apply {
            put("category", category)
            put("totalRecordTypes", categoryRecords.size)
            put("totalRecords", totalRecordsInCategory)
            put("recordTypes", recordTypesJson)
        }

        return categoryJson.toString(2)
    }

    /**
     * Exports all categories to separate JSON files.
     * Creates one file per category.
     * 
     * @param allFetchedRecords Map of category to map of record types
     * @param baseDir The base directory for saving files
     * @return Map of category name to file save success status
     */
    fun exportAllCategoriesToFiles(
        allFetchedRecords: Map<String, Map<String, FetchedRecords>>,
        baseDir: File
    ): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        allFetchedRecords.forEach { (category, categoryRecords) ->
            try {
                // Skip empty categories silently (they're expected if no permissions)
                if (categoryRecords.isEmpty()) {
                    Log.d(TAG, "Skipping empty category: $category")
                    results[category] = true // Mark as success since there's nothing to export
                    return@forEach
                }
                
                val jsonString = exportCategoryToJson(categoryRecords, category)
                val fileName = "${category.lowercase().replace(" ", "_")}_data.json"
                val file = File(baseDir, fileName)

                // Create parent directories if they don't exist
                try {
                    file.parentFile?.mkdirs()
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating directories for category: $category", e)
                    results[category] = false
                    return@forEach
                }

                // Write JSON to file
                try {
                    file.writeText(jsonString)
                    results[category] = true
                    Log.d(TAG, "✓ Exported category '$category' to file: ${file.absolutePath}")
                } catch (e: java.io.IOException) {
                    Log.e(TAG, "✗ IO error exporting category: $category - ${e.message}", e)
                    results[category] = false
                } catch (e: SecurityException) {
                    Log.e(TAG, "✗ Security error exporting category: $category - ${e.message}", e)
                    results[category] = false
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Error writing file for category: $category - ${e.message}", e)
                    results[category] = false
                }
            } catch (e: Exception) {
                // Catch any other errors during export
                Log.e(TAG, "✗ Unexpected error exporting category: $category (${e.javaClass.simpleName}) - ${e.message}", e)
                results[category] = false
                // Continue with other categories - don't fail entire export
            }
        }

        return results
    }

    /**
     * Exports all data to a single comprehensive JSON file.
     * 
     * @param allFetchedRecords Map of category to map of record types
     * @param baseDir The base directory for saving files
     * @param fileName The file name (default: "all_health_data.json")
     * @return true if successful, false otherwise
     */
    fun exportAllToSingleFile(
        allFetchedRecords: Map<String, Map<String, FetchedRecords>>,
        baseDir: File,
        fileName: String = "all_health_data.json"
    ): Boolean {
        return try {
            val allDataJson = JSONObject()
            val categoriesJson = JSONObject()

            var totalCategories = 0
            var totalRecordTypes = 0
            var totalRecords = 0

            allFetchedRecords.forEach { (category, categoryRecords) ->
                try {
                    val categoryJson = JSONObject().apply {
                        put("category", category)
                        put("recordTypes", categoryRecords.size)
                        put("totalRecords", categoryRecords.values.sumOf { it.count })
                        put("typesWithData", categoryRecords.values.count { it.count > 0 })
                        put("typesWithPermissionDenied", categoryRecords.values.count { it.isPermissionDenied })
                    }
                    categoriesJson.put(category, categoryJson)
                    
                    totalCategories++
                    totalRecordTypes += categoryRecords.size
                    totalRecords += categoryRecords.values.sumOf { it.count }
                } catch (e: Exception) {
                    // If a category fails to serialize, log and continue
                    Log.w(TAG, "Error serializing category '$category' in summary: ${e.message} - continuing")
                    // Continue with other categories
                }
            }

            allDataJson.apply {
                put("totalCategories", totalCategories)
                put("totalRecordTypes", totalRecordTypes)
                put("totalRecords", totalRecords)
                put("exportDate", System.currentTimeMillis())
                put("categories", categoriesJson)
            }

            val file = File(baseDir, fileName)
            file.parentFile?.mkdirs()
            file.writeText(allDataJson.toString(2))

            Log.d(TAG, "Exported all data to file: ${file.absolutePath}")
            Log.d(TAG, "Summary: $totalCategories categories, $totalRecordTypes types, $totalRecords records")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting all data to single file", e)
            false
        }
    }

    /**
     * Gets file path for a category export file.
     * 
     * @param baseDir The base directory
     * @param category The category name
     * @return File object for the category export
     */
    fun getCategoryFile(baseDir: File, category: String): File {
        val fileName = "${category.lowercase().replace(" ", "_")}_data.json"
        return File(baseDir, fileName)
    }
}
