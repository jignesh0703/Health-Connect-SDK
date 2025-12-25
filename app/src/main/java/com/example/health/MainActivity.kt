package com.example.health

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.health.data.export.GenericHealthDataExporter
import com.example.health.data.export.HealthDataExporter
import com.example.health.data.model.DailySteps
import com.example.health.data.model.HeartRateSample
import com.example.health.data.repository.GenericHealthRepository
import com.example.health.data.repository.HealthRepository
import com.example.health.ui.screen.HealthScreen
import com.example.health.util.HealthConnectUtils
import com.example.health.util.HealthPermissionManager
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Main Activity for the Health Connect application.
 * 
 * This activity is responsible for:
 * - Checking Health Connect availability
 * - Initializing Health Connect client
 * - Managing permissions
 * - Coordinating between UI and data layer
 * 
 * The activity follows the single responsibility principle by delegating:
 * - Data operations to HealthRepository
 * - Permission management to HealthPermissionManager
 * - UI rendering to HealthScreen composable
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Health Connect client instance
    private lateinit var healthClient: HealthConnectClient
    
    // Permission launcher for requesting Health Connect permissions
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Set<String>>
    
    // Repository for health data operations
    private lateinit var healthRepository: HealthRepository
    
    // Generic repository for fetching all health data types
    private lateinit var genericHealthRepository: GenericHealthRepository
    
    // Permission manager for handling permissions
    private lateinit var permissionManager: HealthPermissionManager
    
    // Flag indicating if Health Connect is available on the device
    private var isHealthConnectAvailable = false

    /**
     * Called when the activity is first created.
     * Initializes Health Connect client and sets up the UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        // Check if Health Connect is supported on this Android version
        val errorMessage = HealthConnectUtils.getAvailabilityErrorMessage(this)
        if (errorMessage != null) {
            Log.e(TAG, "Health Connect not available: $errorMessage")
            setContent {
                MaterialTheme {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            return
        }

        // Check SDK availability and initialize client
        if (HealthConnectUtils.isSdkAvailable(this)) {
            healthClient = HealthConnectClient.getOrCreate(this)
            isHealthConnectAvailable = true
            Log.d(TAG, "Health Connect client initialized successfully")
        } else {
            Log.e(TAG, "Health Connect SDK not available")
            isHealthConnectAvailable = false
        }

        // Initialize repository and permission manager
        if (isHealthConnectAvailable) {
            healthRepository = HealthRepository(healthClient)
            genericHealthRepository = GenericHealthRepository(healthClient)
            
            // Register permission launcher using the official contract
            val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
            
            requestPermissionsLauncher = registerForActivityResult(requestPermissionActivityContract) { granted ->
                val requiredPermissions = HealthPermissionManager.getAllRequiredPermissions()
                val grantedCount = granted.size
                val totalCount = requiredPermissions.size
                if (granted.containsAll(requiredPermissions)) {
                    Log.d(TAG, "All permissions granted by user ($grantedCount/$totalCount)")
                } else {
                    Log.w(TAG, "Some permissions denied. Granted: $grantedCount/$totalCount")
                }
            }
            
            permissionManager = HealthPermissionManager(healthClient, requestPermissionsLauncher)
        }

        // Set up the UI
        setContent {
            MaterialTheme {
                HealthScreenContent()
            }
        }

        Log.d(TAG, "MainActivity setup complete")
    }

    /**
     * Composable content for the health screen.
     * Manages UI state and coordinates data fetching operations.
     */
    @Composable
    private fun HealthScreenContent() {
        // UI state variables
        var steps by remember { mutableLongStateOf(0L) }
        var heartRateSamples by remember { mutableIntStateOf(0) }
        var heartRateValues by remember { mutableStateOf<List<Double>>(emptyList()) }
        var status by remember {
            mutableStateOf(
                if (isHealthConnectAvailable) "Preparing to fetch data..." 
                else "Health Connect SDK Unavailable"
            )
        }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var lastUpdateTime by remember { mutableStateOf<String?>(null) }
        var allHealthData by remember { mutableStateOf<Map<String, Map<String, Int>>>(emptyMap()) }
        // NEW: Store full records to pass to detail screen
        var allHealthRecords by remember { mutableStateOf<Map<String, Map<String, GenericHealthRepository.FetchedRecords>>>(emptyMap()) }
        var dailyStepsList by remember { mutableStateOf<List<DailySteps>>(emptyList()) }
        var heartRateSampleList by remember { mutableStateOf<List<HeartRateSample>>(emptyList()) }
        var hasAutoFetched by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()

        /**
         * Automatically fetches all health data when the screen loads (only once).
         * This eliminates the need to manually press the "Fetch ALL Health Data Types" button.
         */
        LaunchedEffect(isHealthConnectAvailable) {
            if (isHealthConnectAvailable && !hasAutoFetched) {
                // Wait a bit to ensure repository is initialized (initialized in onCreate)
                kotlinx.coroutines.delay(1000)
                try {
                    hasAutoFetched = true
//                    fetchAllHealthDataTypes(null, null)
                } catch (e: UninitializedPropertyAccessException) {
                    Log.e(TAG, "Repository not initialized yet: ${e.message}")
                    errorMessage = "Initializing repository..."
                    status = "Initializing..."
                    hasAutoFetched = false // Allow retry on next recomposition
                } catch (e: Exception) {
                    Log.e(TAG, "Error in auto-fetch: ${e.message}")
                    errorMessage = "Auto-fetch failed: ${e.message}"
                    status = "Error during auto-fetch"
                    hasAutoFetched = false // Allow retry
                }
            }
        }

        /**
         * Fetches health data from Health Connect.
         * This function:
         * 1. Checks if permissions are granted
         * 2. Fetches data using the repository
         * 3. Updates UI state with the results
         */
        suspend fun fetchHealthData() {
            if (!isHealthConnectAvailable || !::permissionManager.isInitialized) {
                errorMessage = "Health Connect SDK not available on this device"
                status = "Error"
                return
            }

            try {
                // Check permissions first
                status = "Checking permissions..."
                errorMessage = null

                // For basic health data (Steps/Heart Rate), we only need basic permissions
                if (!permissionManager.hasBasicPermissions()) {
                    status = "Requesting permissions..."
                    permissionManager.requestPermissions() // Requests basic permissions
                    errorMessage = "Please grant permissions first"
                    status = "Permissions required"
                    return
                }

                // Fetch data using repository
                status = "Fetching data from Health Connect..."
                val summary = healthRepository.fetchAllHealthData(days = 7)

                // Update UI state with fetched data
                steps = summary.totalSteps
                heartRateSamples = summary.heartRateSamples
                heartRateValues = summary.heartRateValues
                lastUpdateTime = ZonedDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                
                status = "Data fetched successfully!"
                errorMessage = null

                Log.d(TAG, "Health data updated: Steps=$steps, HeartRateSamples=$heartRateSamples")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching health data", e)
                errorMessage = "Failed to fetch data: ${e.message}"
                status = "Error occurred"
            }
        }

        /**
         * Fetches ALL historical health data from Health Connect and exports it to files.
         * This function:
         * 1. Checks if permissions are granted
         * 2. Fetches ALL historical data using the repository
         * 3. Exports daily steps and heart rate samples to separate JSON files
         * 4. Updates UI state with the results
         */
        suspend fun fetchAllHistoricalData() {
            if (!isHealthConnectAvailable || !::permissionManager.isInitialized) {
                errorMessage = "Health Connect SDK not available on this device"
                status = "Error"
                return
            }

            try {
                // Check permissions first
                status = "Checking permissions..."
                errorMessage = null

                // For basic health data history (Steps/Heart Rate), we only need basic permissions
                if (!permissionManager.hasBasicPermissions()) {
                    status = "Requesting permissions..."
                    permissionManager.requestPermissions() // Requests basic permissions
                    errorMessage = "Please grant permissions first"
                    status = "Permissions required"
                    return
                }

                // Fetch ALL historical data using repository
                status = "Fetching ALL historical data from Health Connect..."
                Log.d(TAG, "Starting to fetch all historical health data")
                
                val historicalData = healthRepository.fetchAllHistoricalData()

                // Export data to files
                status = "Exporting data to files..."
                val baseDir = filesDir
                val (dailyStepsSaved, heartRateSaved) = HealthDataExporter.exportAndSaveAllData(
                    historicalData,
                    baseDir
                )

                if (dailyStepsSaved && heartRateSaved) {
                    status = "All historical data exported successfully!"
                    errorMessage = null
                    
                    // Update UI with summary (showing totals)
                    steps = historicalData.totalSteps
                    heartRateSamples = historicalData.totalHeartRateSamples
                    heartRateValues = historicalData.heartRateSamples.map { it.beatsPerMinute }
                    dailyStepsList = historicalData.dailySteps
                    heartRateSampleList = historicalData.heartRateSamples
                    
                    lastUpdateTime = ZonedDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    
                    Log.d(TAG, "Historical data export complete:")
                    Log.d(TAG, "  - Daily Steps file: ${File(baseDir, "daily_steps.json").absolutePath}")
                    Log.d(TAG, "  - Heart Rate file: ${File(baseDir, "heart_rate_samples.json").absolutePath}")
                    Log.d(TAG, "  - Total days with steps: ${historicalData.getDaysWithSteps()}")
                    Log.d(TAG, "  - Total HR samples: ${historicalData.totalHeartRateSamples}")
                    Log.d(TAG, "  - Date range: ${historicalData.getDateRangeString()}")
                } else {
                    status = "Data fetched but export had errors"
                    errorMessage = "Daily Steps: ${if (dailyStepsSaved) "OK" else "Failed"}, " +
                            "Heart Rate: ${if (heartRateSaved) "OK" else "Failed"}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching all historical health data", e)
                errorMessage = "Failed to fetch data: ${e.message}"
                status = "Error occurred"
            }
        }

        /**
         * Fetches ALL health data types from ALL categories and exports them to files.
         * This is the comprehensive method that fetches everything available in Health Connect.
         * This function is called automatically when the app starts.
         *
         * @param startDate Optional start date to filter records. If null, fetches from historical start.
         * @param endDate Optional end date to filter records. If null, fetches until now.
         */
        suspend fun fetchAllHealthDataTypes(startDate: LocalDate? = null, endDate: LocalDate? = null) {
            if (!isHealthConnectAvailable) {
                errorMessage = "Health Connect SDK not available on this device"
                status = "Health Connect SDK Unavailable"
                return
            }

            // Check if repository is initialized
            val isRepositoryInitialized = try {
                ::genericHealthRepository.isInitialized
            } catch (e: Exception) {
                false
            }

            if (!isRepositoryInitialized) {
                errorMessage = "Health Connect repository not initialized"
                status = "Initializing..."
                return
            }

            try {
                // Check permissions first
                status = "Checking permissions for all data types..."
                errorMessage = null

                val startTime = if (startDate != null) {
                    startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                } else {
                    // Default historical start date
                    ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant()
                }

                val endTime = if (endDate != null) {
                    endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
                } else {
                    java.time.Instant.now()
                }

                // For now, we'll try to fetch even if not all permissions are granted
                // Health Connect will simply return empty results for types without permission
                status = if (startDate != null && endDate != null) {
                    "Fetching data from ${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} to ${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}..."
                } else {
                    "Fetching ALL historical health data..."
                }
                
                Log.d(TAG, "Starting to fetch health data types. Range: $startTime to $endTime")

                // Fetch all records for all categories with the specified time range
                val allFetchedRecords = genericHealthRepository.fetchAllRecordsForAllCategories(
                    startTime = startTime,
                    endTime = endTime
                )

                // Export data to files (one file per category)
                status = "Exporting data to files..."
                val baseDir = filesDir
                val exportResults = GenericHealthDataExporter.exportAllCategoriesToFiles(
                    allFetchedRecords,
                    baseDir
                )

                // Also export a summary file
                val summaryExported = GenericHealthDataExporter.exportAllToSingleFile(
                    allFetchedRecords,
                    baseDir,
                    "all_health_data_summary.json"
                )

                // Get summary statistics
                val summaryStats = genericHealthRepository.getSummaryStatistics(allFetchedRecords)

                // Log summary
                Log.d(TAG, "=".repeat(60))
                Log.d(TAG, "ALL HEALTH DATA FETCH AND EXPORT COMPLETE")
                Log.d(TAG, "=".repeat(60))
                summaryStats.forEach { (category, stats) ->
//                    Log.d(TAG, "$category: ${stats.totalRecordTypes} types, ${stats.totalRecords} records")
                    Log.d(TAG, "  - Types with data: ${stats.typesWithData}, Types without data: ${stats.typesWithoutData}")
                }
                Log.d(TAG, "=".repeat(60))

                // Convert fetched records to UI format: Map<Category, Map<RecordType, Count>>
                val uiHealthData = allFetchedRecords.mapValues { (_, categoryRecords) ->
                    categoryRecords.mapValues { (_, fetchedRecords) ->
                        fetchedRecords.count
                    }
                }
                
                // Update UI state with all health data
                allHealthData = uiHealthData
                
                // Store full records map for detail view
                allHealthRecords = allFetchedRecords

                // Check export results
                val successCount = exportResults.values.count { it }
                val totalCount = exportResults.size
                
                if (successCount == totalCount && summaryExported) {
                    status = if (startDate != null && endDate != null) {
                        "Data for ${startDate.format(DateTimeFormatter.ofPattern("MMM dd"))} - ${endDate.format(DateTimeFormatter.ofPattern("MMM dd"))} fetched!"
                    } else {
                        "All historical data fetched!"
                    }
                    errorMessage = null
                    
                    // Update UI with summary
                    // Calculate total records across all categories and types
                    val totalRecords = allFetchedRecords.values.sumOf { categoryMap ->
                        categoryMap.values.sumOf { it.count }
                    }

                    steps = totalRecords.toLong() // Display total records count in the steps field for summary
                    heartRateSamples = totalCount // Use category count
                    
                    lastUpdateTime = ZonedDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    
                    // Log file locations
                    Log.d(TAG, "Files saved to: ${baseDir.absolutePath}")
                    exportResults.forEach { (category, success) ->
                        if (success) {
                            val fileName = GenericHealthDataExporter.getCategoryFile(baseDir, category).name
                            Log.d(TAG, "  - $category: $fileName")
                        }
                    }
                } else {
                    status = "Data fetched but some exports had errors"
                    errorMessage = "Categories exported: $successCount/$totalCount"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching all health data types", e)
                errorMessage = "Failed to fetch data: ${e.message}"
                status = "Error occurred"
            }
        }

        /**
         * Requests Health Connect permissions from the user (all permissions).
         */
        fun requestPermissions() {
            if (!isHealthConnectAvailable || !::permissionManager.isInitialized) {
                errorMessage = "Health Connect SDK not available on this device"
                status = "Error"
                return
            }

            try {
                permissionManager.requestAllPermissions() // Request all permissions now
                status = "Requesting all Health Connect permissions..."
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting permissions", e)
                errorMessage = "Failed to request permissions: ${e.message}"
            }
        }

        // Render the health screen
        HealthScreen(
            isHealthConnectAvailable = isHealthConnectAvailable,
            onFetchData = { fetchHealthData() },
            onFetchAllHistoricalData = { fetchAllHistoricalData() },
            onFetchAllHealthDataTypes = { startDate, endDate -> fetchAllHealthDataTypes(startDate, endDate) },
            onRequestPermissions = { requestPermissions() },
            steps = steps,
            heartRateSamples = heartRateSamples,
            heartRateValues = heartRateValues,
            status = status,
            errorMessage = errorMessage,
            lastUpdateTime = lastUpdateTime,
            allHealthData = allHealthData,
            allHealthRecords = allHealthRecords, // Pass full records map
            dailyStepsList = dailyStepsList,
            heartRateSampleList = heartRateSampleList
        )
    }
}
