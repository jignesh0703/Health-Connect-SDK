package com.example.health.data.export

import android.util.Log
import com.example.health.data.model.DailySteps
import com.example.health.data.model.HeartRateSample
import com.example.health.data.model.HistoricalHealthData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.format.DateTimeFormatter

/**
 * Utility class for exporting health data to different formats.
 * Provides methods to convert health data to JSON format for storage.
 */
object HealthDataExporter {
    private const val TAG = "HealthDataExporter"
    private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Exports daily steps data to a JSON string.
     * 
     * @param dailySteps List of daily steps to export
     * @return JSON string representation of the daily steps data
     */
    fun exportDailyStepsToJson(dailySteps: List<DailySteps>): String {
        val jsonArray = JSONArray()
        
        dailySteps.forEach { dailyStep ->
            val jsonObject = JSONObject().apply {
                put("date", dailyStep.date.toString())
                put("steps", dailyStep.steps)
                put("startTime", dailyStep.startTime.toString())
                put("endTime", dailyStep.endTime.toString())
                put("dateString", dailyStep.getDateString())
            }
            jsonArray.put(jsonObject)
        }

        val result = JSONObject().apply {
            put("totalDays", dailySteps.size)
            put("totalSteps", dailySteps.sumOf { it.steps })
            put("earliestDate", dailySteps.firstOrNull()?.date?.toString())
            put("latestDate", dailySteps.lastOrNull()?.date?.toString())
            put("dailySteps", jsonArray)
        }

        return result.toString(2) // Pretty print with 2-space indentation
    }

    /**
     * Exports heart rate samples data to a JSON string.
     * 
     * @param heartRateSamples List of heart rate samples to export
     * @return JSON string representation of the heart rate samples data
     */
    fun exportHeartRateSamplesToJson(heartRateSamples: List<HeartRateSample>): String {
        val jsonArray = JSONArray()
        
        heartRateSamples.forEach { sample ->
            val jsonObject = JSONObject().apply {
                put("time", sample.time.toString())
                put("beatsPerMinute", sample.beatsPerMinute)
                put("date", sample.getDate().toString())
                put("timeString", sample.getTimeString())
                
                // Add metadata if available
                if (sample.metadata != null && sample.metadata.isNotEmpty()) {
                    val metadataJson = JSONObject()
                    sample.metadata.forEach { (key, value) ->
                        metadataJson.put(key, value)
                    }
                    put("metadata", metadataJson)
                }
            }
            jsonArray.put(jsonObject)
        }

        val result = JSONObject().apply {
            put("totalSamples", heartRateSamples.size)
            put("earliestTime", heartRateSamples.firstOrNull()?.time?.toString())
            put("latestTime", heartRateSamples.lastOrNull()?.time?.toString())
            
            // Calculate statistics if data is available
            if (heartRateSamples.isNotEmpty()) {
                val values = heartRateSamples.map { it.beatsPerMinute }
                put("averageBpm", values.average())
                put("minBpm", values.minOrNull())
                put("maxBpm", values.maxOrNull())
            }
            
            put("samples", jsonArray)
        }

        return result.toString(2) // Pretty print with 2-space indentation
    }

    /**
     * Exports all historical health data to JSON strings.
     * 
     * @param historicalData The historical health data to export
     * @return Pair of (dailyStepsJson, heartRateSamplesJson)
     */
    fun exportAllDataToJson(historicalData: HistoricalHealthData): Pair<String, String> {
        val dailyStepsJson = exportDailyStepsToJson(historicalData.dailySteps)
        val heartRateJson = exportHeartRateSamplesToJson(historicalData.heartRateSamples)
        return Pair(dailyStepsJson, heartRateJson)
    }

    /**
     * Saves JSON string to a file.
     * 
     * @param jsonString The JSON string to save
     * @param file The file to save to
     * @return true if successful, false otherwise
     */
    fun saveJsonToFile(jsonString: String, file: File): Boolean {
        return try {
            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()
            
            // Write JSON to file
            file.writeText(jsonString)
            Log.d(TAG, "Successfully saved JSON to file: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving JSON to file: ${file.absolutePath}", e)
            false
        }
    }

    /**
     * Creates a file path for daily steps data in the app's files directory.
     * 
     * @param baseDir The base directory (usually from context.filesDir)
     * @param fileName Optional custom file name (default: "daily_steps.json")
     * @return File object for the daily steps data
     */
    fun getDailyStepsFile(baseDir: File, fileName: String = "daily_steps.json"): File {
        return File(baseDir, fileName)
    }

    /**
     * Creates a file path for heart rate samples data in the app's files directory.
     * 
     * @param baseDir The base directory (usually from context.filesDir)
     * @param fileName Optional custom file name (default: "heart_rate_samples.json")
     * @return File object for the heart rate samples data
     */
    fun getHeartRateSamplesFile(baseDir: File, fileName: String = "heart_rate_samples.json"): File {
        return File(baseDir, fileName)
    }

    /**
     * Exports and saves all historical health data to files.
     * 
     * @param historicalData The historical health data to export
     * @param baseDir The base directory for saving files
     * @param dailyStepsFileName Optional file name for daily steps (default: "daily_steps.json")
     * @param heartRateFileName Optional file name for heart rate (default: "heart_rate_samples.json")
     * @return Pair of (dailyStepsFileSaved, heartRateFileSaved) - true if saved successfully
     */
    fun exportAndSaveAllData(
        historicalData: HistoricalHealthData,
        baseDir: File,
        dailyStepsFileName: String = "daily_steps.json",
        heartRateFileName: String = "heart_rate_samples.json"
    ): Pair<Boolean, Boolean> {
        val (dailyStepsJson, heartRateJson) = exportAllDataToJson(historicalData)
        
        val dailyStepsFile = getDailyStepsFile(baseDir, dailyStepsFileName)
        val heartRateFile = getHeartRateSamplesFile(baseDir, heartRateFileName)
        
        val dailyStepsSaved = saveJsonToFile(dailyStepsJson, dailyStepsFile)
        val heartRateSaved = saveJsonToFile(heartRateJson, heartRateFile)
        
        Log.d(TAG, "Export complete - Daily Steps: ${if (dailyStepsSaved) "Saved" else "Failed"}, " +
                "Heart Rate: ${if (heartRateSaved) "Saved" else "Failed"}")
        
        return Pair(dailyStepsSaved, heartRateSaved)
    }
}

