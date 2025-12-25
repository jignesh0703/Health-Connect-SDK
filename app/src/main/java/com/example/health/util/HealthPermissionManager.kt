package com.example.health.util

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import com.example.health.data.config.HealthRecordTypes
import kotlin.reflect.KClass

/**
 * Manages Health Connect permissions for the application.
 */
class HealthPermissionManager(
    private val healthClient: HealthConnectClient,
    private val requestPermissionsLauncher: ActivityResultLauncher<Set<String>>
) {
    companion object {
        private const val TAG = "HealthPermissionManager"

        /**
         * Returns the set of all permissions required by the application.
         * Now includes permissions for ALL supported record types.
         */
        fun getAllRequiredPermissions(): Set<String> {
            val permissions = mutableSetOf<String>()
            
            // Add READ permissions for all configured record types
            HealthRecordTypes.ALL_RECORDS.forEach { config ->
                // Note: Not all record types might have a corresponding permission constant in all SDK versions,
                // but HealthPermission.getReadPermission handles this by accepting KClass
                try {
                    val permission = HealthPermission.getReadPermission(config.recordClass)
                    permissions.add(permission)
                } catch (e: Exception) {
                    // Ignore if permission cannot be generated for a type (e.g. if not supported)
                    Log.w(TAG, "Could not get permission for ${config.recordClass.simpleName}: ${e.message}")
                }
            }
            
            // Explicitly add common permissions to be safe, including those requested by user
            permissions.add(HealthPermission.getReadPermission(StepsRecord::class))
            permissions.add(HealthPermission.getWritePermission(StepsRecord::class))
            permissions.add(HealthPermission.getReadPermission(HeartRateRecord::class))
            permissions.add(HealthPermission.getWritePermission(HeartRateRecord::class))
            permissions.add(HealthPermission.getReadPermission(HeightRecord::class))
            permissions.add(HealthPermission.getReadPermission(WeightRecord::class))
            permissions.add(HealthPermission.getReadPermission(BasalMetabolicRateRecord::class))
            
            return permissions
        }
        
        /**
         * Returns the basic set of permissions (just Steps and Heart Rate)
         * Used for initial testing or basic functionality.
         */
        fun getBasicPermissions(): Set<String> {
            return setOf(
                HealthPermission.getReadPermission(StepsRecord::class),
                HealthPermission.getReadPermission(HeartRateRecord::class)
            )
        }
    }

    /**
     * Checks if all required permissions are granted.
     */
    suspend fun hasAllPermissions(): Boolean {
        val granted = healthClient.permissionController.getGrantedPermissions()
        val required = getAllRequiredPermissions()
        
        val missing = required - granted
        if (missing.isNotEmpty()) {
            Log.d(TAG, "Missing permissions: $missing")
        }
        
        return granted.containsAll(required)
    }

    /**
     * Checks if basic permissions (Steps and Heart Rate) are granted.
     */
    suspend fun hasBasicPermissions(): Boolean {
        val granted = healthClient.permissionController.getGrantedPermissions()
        val required = getBasicPermissions()
        
        return granted.containsAll(required)
    }

    /**
     * Requests all required permissions from the user.
     */
    fun requestAllPermissions() {
        val permissions = getAllRequiredPermissions()
        Log.d(TAG, "Requesting ${permissions.size} permissions")
        requestPermissionsLauncher.launch(permissions)
    }
    
    /**
     * Requests only basic permissions (Steps, Heart Rate).
     */
    fun requestPermissions() {
        requestPermissionsLauncher.launch(getBasicPermissions())
    }
}
