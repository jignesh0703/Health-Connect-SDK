package com.example.health.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient

/**
 * Utility class for Health Connect SDK availability and compatibility checks.
 * Provides helper methods to check if Health Connect is available on the device.
 */
object HealthConnectUtils {
    private const val TAG = "HealthConnectUtils"
    private const val HEALTH_CONNECT_PACKAGE_NAME = "com.google.android.apps.healthdata"

    /**
     * Checks if Health Connect is supported on the current Android version.
     * Health Connect requires Android 11 (API 30) or higher.
     * 
     * @return true if Android version is 11 or higher, false otherwise
     */
    fun isHealthConnectSupported(): Boolean {
        val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        Log.d(TAG, "Health Connect supported: $isSupported (SDK: ${Build.VERSION.SDK_INT})")
        return isSupported
    }

    /**
     * Checks if Health Connect SDK is available on the device.
     * This checks if the Health Connect app is installed and available.
     * 
     * @param context The application context
     * @return true if SDK is available, false otherwise
     */
    fun isSdkAvailable(context: Context): Boolean {
        return try {
            val sdkStatus = HealthConnectClient.getSdkStatus(context)
            val isAvailable = sdkStatus == HealthConnectClient.SDK_AVAILABLE
            Log.d(TAG, "Health Connect SDK available: $isAvailable (Status: $sdkStatus)")
            isAvailable
        } catch (e: Exception) {
            Log.e(TAG, "Error checking SDK status", e)
            false
        }
    }

    /**
     * Checks if the Health Connect app is enabled on the device.
     * The app might be installed but disabled by the user.
     * 
     * @param context The application context
     * @return true if the app is enabled, false otherwise
     */
    fun isHealthConnectAppEnabled(context: Context): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                HEALTH_CONNECT_PACKAGE_NAME,
                0
            )
            val isEnabled = appInfo.enabled
            Log.d(TAG, "Health Connect app enabled: $isEnabled")
            isEnabled
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Health Connect app not installed")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if Health Connect app is enabled", e)
            false
        }
    }

    /**
     * Gets a user-friendly error message based on Health Connect availability.
     * 
     * @param context The application context
     * @return Error message string, or null if Health Connect is available
     */
    fun getAvailabilityErrorMessage(context: Context): String? {
        if (!isHealthConnectSupported()) {
            return "Health Connect is not supported on Android 10 or lower.\n" +
                    "Please use a device with Android 11 or higher."
        }

        if (!isSdkAvailable(context)) {
            return "Health Connect SDK is not available on this device.\n" +
                    "Please install Health Connect from the Play Store."
        }

        if (!isHealthConnectAppEnabled(context)) {
            return "Health Connect app is disabled.\n" +
                    "Please enable it in your device settings."
        }

        return null // Health Connect is available
    }
}

