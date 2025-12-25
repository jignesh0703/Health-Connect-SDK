package com.example.health.ui.screen

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.Record
import com.example.health.data.model.DailySteps
import com.example.health.data.model.HeartRateSample
import com.example.health.data.repository.GenericHealthRepository
import com.example.health.ui.components.AllHealthDataDisplay
import com.example.health.ui.components.HeartRateCard
import com.example.health.ui.components.StatusCard
import com.example.health.ui.components.StepsCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Main screen composable for displaying health data.
 * This screen shows steps, heart rate data, and provides buttons to fetch data and request permissions.
 */
@Composable
fun HealthScreen(
    isHealthConnectAvailable: Boolean,
    onFetchData: suspend () -> Unit,
    onFetchAllHistoricalData: suspend () -> Unit,
    onFetchAllHealthDataTypes: suspend (LocalDate?, LocalDate?) -> Unit,
    onRequestPermissions: () -> Unit,
    steps: Long = 0L,
    heartRateSamples: Int = 0,
    heartRateValues: List<Double> = emptyList(),
    status: String = "Ready to fetch data",
    errorMessage: String? = null,
    lastUpdateTime: String? = null,
    // Updated to receive full records map
    allHealthRecords: Map<String, Map<String, GenericHealthRepository.FetchedRecords>> = emptyMap(),
    // Keep this for backwards compatibility if needed, or derived from allHealthRecords
    allHealthData: Map<String, Map<String, Int>> = emptyMap(),
    dailyStepsList: List<DailySteps> = emptyList(),
    heartRateSampleList: List<HeartRateSample> = emptyList()
) {
    val scope = rememberCoroutineScope()
    var showAllDataScreen by remember { mutableStateOf(false) }
    var showStepsDetailScreen by remember { mutableStateOf(false) }
    var showHeartRateDetailScreen by remember { mutableStateOf(false) }
    
    // State for generic record detail view
    var selectedRecordType by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    // Date filter state
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    val context = LocalContext.current

    // Navigation logic
    if (selectedRecordType != null && selectedCategory != null) {
        // Find the records for the selected type
        val records = allHealthRecords[selectedCategory]?.get(selectedRecordType)?.records ?: emptyList()
        
        GenericRecordDetailScreen(
            recordType = selectedRecordType!!,
            records = records,
            onBackClick = { 
                selectedRecordType = null
                selectedCategory = null
            }
        )
    } else if (showStepsDetailScreen) {
        StepsDetailScreen(
            dailyStepsList = dailyStepsList,
            onBackClick = { showStepsDetailScreen = false }
        )
    } else if (showHeartRateDetailScreen) {
        HeartRateDetailScreen(
            heartRateSamples = heartRateSampleList,
            onBackClick = { showHeartRateDetailScreen = false }
        )
    } else if (showAllDataScreen) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { showAllDataScreen = false }) {
                    Text("Back")
                }
                
                Text(
                    text = "All Health Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Date Range Picker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start Date Picker
                Button(
                    onClick = {
                        showDatePicker(context, startDate) { newDate ->
                            startDate = newDate
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Text("Start: " + startDate.format(DateTimeFormatter.ofPattern("MMM dd")))
                }

                // End Date Picker
                Button(
                    onClick = {
                        showDatePicker(context, endDate) { newDate ->
                            endDate = newDate
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text("End: " + endDate.format(DateTimeFormatter.ofPattern("MMM dd")))
                }
            }

            // Fetch Button
            Button(
                onClick = {
                    scope.launch {
                        onFetchAllHealthDataTypes(startDate, endDate)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fetch Data for Range")
            }

            if (allHealthData.isNotEmpty()) {
                AllHealthDataDisplay(
                    allHealthData = allHealthData,
                    modifier = Modifier.weight(1f),
                    onRecordTypeClick = { recordType, category ->
                        selectedRecordType = recordType
                        selectedCategory = category
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available. Select dates and fetch data.")
                }
            }
        }
    } else {
        // Main Dashboard View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen title
            Text(
                text = "Health Connect Data",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status card
            StatusCard(
                status = status,
                errorMessage = errorMessage,
                lastUpdateTime = lastUpdateTime,
                modifier = Modifier.fillMaxWidth()
            )

            // Steps card - Clickable to open details
            StepsCard(
                steps = steps,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        // Only navigate if we have data or if the user wants to see history
                        if (steps > 0 || dailyStepsList.isNotEmpty()) {
                            showStepsDetailScreen = true 
                        }
                    }
            )

            // Heart rate card
            HeartRateCard(
                samples = heartRateSamples,
                heartRateValues = heartRateValues,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Only navigate if we have data or if the user wants to see history
                        if (heartRateSamples > 0 || heartRateSampleList.isNotEmpty()) {
                            showHeartRateDetailScreen = true
                        }
                    }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // First row: Fetch buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                onFetchData()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isHealthConnectAvailable
                    ) {
                        Text("Fetch Last 7 Days")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                onFetchAllHistoricalData()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        enabled = isHealthConnectAvailable
                    ) {
                        Text("Fetch All Steps/HR")
                    }
                }

                // Second row: Fetch ALL health data types button
                Button(
                    onClick = {
                        scope.launch {
                            onFetchAllHealthDataTypes(null, null) // null means fetch all history
                            showAllDataScreen = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = isHealthConnectAvailable
                ) {
                    Text("Fetch ALL Health Data Types")
                }
                
                // Show All Data Screen Button (if data exists)
                if (allHealthData.isNotEmpty()) {
                    Button(
                        onClick = { showAllDataScreen = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("View All Data Records")
                    }
                }

                // Third row: Request permissions button
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = isHealthConnectAvailable
                ) {
                    Text("Request All Permissions")
                }
            }
        }
    }
}

fun showDatePicker(context: Context, currentDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.set(currentDate.year, currentDate.monthValue - 1, currentDate.dayOfMonth)
    
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
