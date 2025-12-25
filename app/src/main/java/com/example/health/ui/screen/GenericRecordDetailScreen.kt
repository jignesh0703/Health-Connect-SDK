package com.example.health.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.Record
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericRecordDetailScreen(
    recordType: String,
    records: List<Record>,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = recordType,
                        style = MaterialTheme.typography.titleMedium 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No records found for $recordType",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text(
                    text = "Total Records: ${records.size}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records) { record ->
                        GenericRecordItemCard(record)
                    }
                }
            }
        }
    }
}

@Composable
fun GenericRecordItemCard(record: Record) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // --- TIME DISPLAY SECTION ---
            // Try to extract time using reflection
            val time = try {
                val method = record.javaClass.getMethod("getTime")
                method.invoke(record) as? java.time.Instant
            } catch (e: Exception) { null }

            val startTime = try {
                val method = record.javaClass.getMethod("getStartTime")
                method.invoke(record) as? java.time.Instant
            } catch (e: Exception) { null }
            
            val endTime = try {
                val method = record.javaClass.getMethod("getEndTime")
                method.invoke(record) as? java.time.Instant
            } catch (e: Exception) { null }
            
            // Prefer "time" for single-sample records, "startTime" for intervals
            val displayTime = time ?: startTime
            
            if (displayTime != null) {
                val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy HH:mm:ss")
                Text(
                    text = displayTime.atZone(ZoneId.systemDefault()).format(dateFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Show duration if available (only for interval records)
                if (startTime != null && endTime != null && startTime != endTime) {
                    val duration = java.time.Duration.between(startTime, endTime)
                    val durationStr = if (duration.toHours() > 0) {
                        "${duration.toHours()}h ${duration.toMinutesPart()}m"
                    } else {
                        "${duration.toMinutes()}m ${duration.toSecondsPart()}s"
                    }
                    
                    Text(
                        text = "Duration: $durationStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // --- DATA DISPLAY SECTION ---
            // Use reflection to extract meaningful data fields (excluding time/metadata)
            val dataFields = remember(record) {
                val ignoredMethods = setOf(
                    "getClass", "wait", "notify", "notifyAll", "toString", "hashCode", "equals",
                    "getMetadata", "getStartTime", "getEndTime", "getTime", 
                    "getZoneOffset", "getStartZoneOffset", "getEndZoneOffset"
                )

                record.javaClass.methods.filter { method ->
                    method.name.startsWith("get") &&
                    method.parameterCount == 0 &&
                    !ignoredMethods.contains(method.name)
                }.map { method ->
                    // Convert "getFieldName" -> "Field Name"
                    val fieldName = method.name.removePrefix("get")
                    // Add spaces before capital letters (CamelCase -> Camel Case)
                    val readableName = fieldName.replace(Regex("([a-z])([A-Z])"), "$1 $2")
                    
                    val value = try {
                        method.invoke(record)
                    } catch (e: Exception) { "Error" }
                    
                    readableName to value
                }.sortedBy { it.first } // Sort alphabetically
            }

            if (dataFields.isNotEmpty()) {
                dataFields.forEach { (name, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$name: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // Fallback to raw string if no fields found (unlikely for valid records)
                 val rawData = record.toString()
                    .replace(record.javaClass.simpleName, "")
                    .trim()
                    .removePrefix("(")
                    .removeSuffix(")")
                
                Text(
                    text = rawData,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}
