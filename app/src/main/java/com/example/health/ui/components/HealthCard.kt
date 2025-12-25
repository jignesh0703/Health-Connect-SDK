package com.example.health.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Reusable card component for displaying health data metrics.
 * 
 * @param title The title of the card (e.g., "Steps", "Heart Rate")
 * @param value The main value to display
 * @param subtitle Optional subtitle or additional information to display below the value
 * @param modifier Modifier for the card
 * @param containerColor The background color of the card
 * @param contentColor The text color for the content
 */
@Composable
fun HealthCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Title text
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            // Main value text
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            
            // Optional subtitle
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Card component specifically for displaying steps data.
 * 
 * @param steps Total number of steps
 * @param modifier Modifier for the card
 */
@Composable
fun StepsCard(
    steps: Long,
    modifier: Modifier = Modifier
) {
    HealthCard(
        title = "Steps (Last 7 Days)",
        value = "$steps steps",
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

/**
 * Card component specifically for displaying heart rate data.
 * 
 * @param samples Number of heart rate samples
 * @param heartRateValues List of heart rate values for calculating statistics
 * @param modifier Modifier for the card
 */
@Composable
fun HeartRateCard(
    samples: Int,
    heartRateValues: List<Double>,
    modifier: Modifier = Modifier
) {
    // Calculate statistics if data is available
    val subtitle = if (heartRateValues.isNotEmpty()) {
        val avgHeartRate = heartRateValues.average()
        val minHeartRate = heartRateValues.minOrNull() ?: 0.0
        val maxHeartRate = heartRateValues.maxOrNull() ?: 0.0
        "Avg: ${avgHeartRate.toInt()} bpm | Min: ${minHeartRate.toInt()} | Max: ${maxHeartRate.toInt()}"
    } else {
        null
    }

    HealthCard(
        title = "Heart Rate (Last 7 Days)",
        value = "$samples samples",
        subtitle = subtitle,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

/**
 * Card component for displaying status information.
 * 
 * @param status Status message to display
 * @param errorMessage Optional error message to display
 * @param lastUpdateTime Optional timestamp of last update
 * @param modifier Modifier for the card
 */
@Composable
fun StatusCard(
    status: String,
    errorMessage: String? = null,
    lastUpdateTime: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status text
            Text(
                text = "Status: $status",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            // Error message if present
            if (errorMessage != null) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Last update time if available
            if (lastUpdateTime != null) {
                Text(
                    text = "Last Updated: $lastUpdateTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

