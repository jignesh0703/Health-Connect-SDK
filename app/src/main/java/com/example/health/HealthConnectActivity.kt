package com.example.health

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Alternative Activity for Health Connect operations.
 * 
 * This activity provides a different approach to handling Health Connect operations.
 * It can be used as a standalone activity or called from other activities.
 * 
 * Features:
 * - Checks if Health Connect app is enabled
 * - Handles permission requests
 * - Reads health data and returns results via Intent
 * 
 * Result codes:
 * - RESULT_OK: Data fetched successfully
 * - RESULT_PERMISSIONS_DENIED: User denied permissions
 * - RESULT_NO_DATA: No health data found
 * - RESULT_ERROR: An error occurred
 * - RESULT_APP_DISABLED: Health Connect app is disabled
 */
class HealthConnectActivity : ComponentActivity() {

    companion object {
        private const val TAG = "HealthConnectActivity"

        /**
         * Result code when permissions are denied by the user.
         */
        const val RESULT_PERMISSIONS_DENIED = 1001

        /**
         * Result code when no health data is found.
         */
        const val RESULT_NO_DATA = 1002

        /**
         * Result code when an error occurs during data fetching.
         */
        const val RESULT_ERROR = 1003

        /**
         * Result code when Health Connect app is disabled on the device.
         */
        const val RESULT_APP_DISABLED = 1006

        /**
         * Intent extra key for steps count.
         */
        const val EXTRA_STEPS = "steps"

        /**
         * Intent extra key for heart rate samples count.
         */
        const val EXTRA_HEART_RATE_SAMPLES = "heart_rate_samples"

        /**
         * Intent extra key for error message.
         */
        const val EXTRA_ERROR_MESSAGE = "error_message"
    }

    // Lazy initialization of Health Connect client
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(this) }

    // Launcher for permission request activity
    private val requestPermissionsLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // After permission request, check permissions again
            checkPermissionsAndProceed()
        }

    /**
     * Called when the activity is first created.
     * Checks if Health Connect app is enabled and proceeds with permission check.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        // Check if Health Connect app is enabled
        if (!isHealthConnectAppEnabled()) {
            Log.w(TAG, "Health Connect app is not enabled")
            setResult(RESULT_APP_DISABLED)
            finish()
            return
        }

        // Check permissions and proceed with data fetching
        checkPermissionsAndProceed()
    }

    /**
     * Checks if the Health Connect app is enabled on the device.
     * The app might be installed but disabled by the user in device settings.
     * 
     * @return true if the app is enabled, false otherwise
     */
    private fun isHealthConnectAppEnabled(): Boolean {
        val packageName = "com.google.android.apps.healthdata"
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isEnabled = appInfo.enabled
            Log.d(TAG, "Health Connect app enabled: $isEnabled")
            isEnabled
        } catch (e: PackageManager.NameNotFoundException) {
            // The app is not installed at all
            Log.w(TAG, "Health Connect app not found")
            false
        }
    }

    /**
     * Checks if required permissions are granted and proceeds accordingly.
     * If permissions are granted, fetches health data.
     * If not, requests permissions from the user.
     */
    private fun checkPermissionsAndProceed() {
        val permissions = getRequiredPermissions()

        lifecycleScope.launch {
            try {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                
                if (granted.containsAll(permissions)) {
                    Log.d(TAG, "All permissions are already granted. Reading data.")
                    readHealthData()
                } else {
                    Log.d(TAG, "Permissions not granted. Launching permission request.")
                    requestPermissions(permissions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking permissions", e)
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_ERROR_MESSAGE, e.toString())
                }
                setResult(RESULT_ERROR, resultIntent)
                finish()
            }
        }
    }

    /**
     * Gets the set of required permissions for reading health data.
     * 
     * @return Set of permission strings required by the app
     */
    private fun getRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class)
        )
    }

    /**
     * Requests Health Connect permissions by launching the permission request activity.
     * 
     * @param permissions Set of permissions to request
     */
    private fun requestPermissions(permissions: Set<String>) {
        try {
            val intent = Intent("androidx.health.connect.client.permission.REQUEST_PERMISSIONS")
            intent.setPackage("com.google.android.apps.healthdata")
            intent.putExtra("HEALTH_CONNECT_CLIENT_PERMISSION_KEYS", permissions.toTypedArray())
            
            Log.d(TAG, "Launching permission request for: $permissions")
            requestPermissionsLauncher.launch(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Error launching permission request", t)
            // If we can't launch the permission request, the app is likely disabled
            setResult(RESULT_APP_DISABLED)
            finish()
        }
    }

    /**
     * Reads health data from Health Connect for the last 24 hours.
     * Returns results via Intent extras and finishes the activity.
     */
    private fun readHealthData() {
        lifecycleScope.launch {
            val startTime = ZonedDateTime.now().minusDays(1).toInstant()
            val endTime = Instant.now()

            try {
                Log.d(TAG, "Reading health data from $startTime to $endTime")

                // Fetch steps data
                val stepsResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )

                // Fetch heart rate data
                val heartRateResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )

                // Check if any data was found
                if (stepsResponse.records.isEmpty() && heartRateResponse.records.isEmpty()) {
                    Log.d(TAG, "No health data found")
                    setResult(RESULT_NO_DATA)
                    finish()
                    return@launch
                }

                // Calculate summary data
                val stepsCount = stepsResponse.records.sumOf { it.count }
                val heartRateSamples = heartRateResponse.records.size

                Log.d(TAG, "Data fetched: Steps=$stepsCount, HeartRateSamples=$heartRateSamples")

                // Return results via Intent
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_STEPS, stepsCount)
                    putExtra(EXTRA_HEART_RATE_SAMPLES, heartRateSamples)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()

            } catch (e: Exception) {
                if (e is SecurityException) {
                    Log.e(TAG, "SecurityException reading health data", e)
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_ERROR_MESSAGE, e.toString())
                    }
                    setResult(RESULT_PERMISSIONS_DENIED, resultIntent)
                    finish()
                    return@launch
                }

                Log.e(TAG, "Error reading health data", e)
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_ERROR_MESSAGE, e.toString())
                }
                setResult(RESULT_ERROR, resultIntent)
                finish()
            }
        }
    }
}
