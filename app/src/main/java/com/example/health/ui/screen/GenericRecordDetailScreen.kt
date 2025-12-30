package com.example.health.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.Record
import android.util.Log

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
                    text = "📋 $recordType",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
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

                // Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Records",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${records.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "📊",
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(records) { index, record ->
                        GenericRecordItemCard(
                            record = record,
                            index = index + 1,
                            totalCount = records.size
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dumps ALL fields of a Health Connect record using reflection.
 * No filtering. SDK-safe.
 */
fun dumpRecordFully(record: Any): String {
    val sb = StringBuilder()
    val clazz = record.javaClass

    sb.append("Class: ${clazz.simpleName}\n\n")

    clazz.methods
        .filter {
            it.parameterCount == 0 &&
                    it.name.startsWith("get") &&
                    it.name != "getClass"
        }
        .sortedBy { it.name }
        .forEach { method ->
            try {
                val value = method.invoke(record)
                sb.append("${method.name.removePrefix("get")}: $value\n")
            } catch (e: Throwable) {
                sb.append("${method.name.removePrefix("get")}: <error>\n")
            }
        }

    return sb.toString()
}

@Composable
fun GenericRecordItemCard(
    record: Record,
    index: Int = 0,
    totalCount: Int = 0
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (index > 0 && totalCount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$index",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = "📋 Complete Record Data",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Get all record fields
            val recordFields = remember(record) {
                extractAllRecordFields(record)
            }
            
            // Log for debugging
            Log.d("HealthRecordDump", "Record: ${record.javaClass.simpleName}")
            recordFields.forEach { (fieldName, fieldValue) ->
                Log.d("HealthRecordDump", "$fieldName: $fieldValue")
            }

            // Show summary when collapsed
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                val summaryFields = recordFields.filterKeys { 
                    it.contains("Time", ignoreCase = true) || 
                    it.contains("Date", ignoreCase = true) ||
                    it.contains("Value", ignoreCase = true) ||
                    it.contains("Type", ignoreCase = true)
                }.toList().take(4)
                
                summaryFields.forEach { (fieldName, fieldValue) ->
                    RecordFieldItemCompact(
                        fieldName = fieldName,
                        fieldValue = fieldValue
                    )
                }
                
                if (recordFields.size > summaryFields.size) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to see all ${recordFields.size} fields →",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Display all fields in a structured format when expanded
                Spacer(modifier = Modifier.height(12.dp))
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Group fields by type for better organization
                val timeFields = recordFields.filterKeys { 
                    it.contains("Time", ignoreCase = true) || it.contains("Date", ignoreCase = true) 
                }
                val valueFields = recordFields.filterKeys { 
                    it.contains("Value", ignoreCase = true) || 
                    it.contains("Count", ignoreCase = true) ||
                    it.contains("Amount", ignoreCase = true)
                }
                val metadataFields = recordFields.filterKeys { 
                    it.contains("Metadata", ignoreCase = true) || 
                    it.contains("Source", ignoreCase = true) ||
                    it.contains("Id", ignoreCase = true)
                }
                val otherFields = recordFields.filterKeys { 
                    !it.contains("Time", ignoreCase = true) && 
                    !it.contains("Date", ignoreCase = true) &&
                    !it.contains("Value", ignoreCase = true) && 
                    !it.contains("Count", ignoreCase = true) &&
                    !it.contains("Amount", ignoreCase = true) &&
                    !it.contains("Metadata", ignoreCase = true) && 
                    !it.contains("Source", ignoreCase = true) &&
                    !it.contains("Id", ignoreCase = true) &&
                    !it.contains("Type", ignoreCase = true)
                }
                
                // Display grouped fields
                if (timeFields.isNotEmpty()) {
                    FieldGroup("⏰ Time & Date", timeFields)
                }
                
                if (valueFields.isNotEmpty()) {
                    FieldGroup("📊 Values & Measurements", valueFields)
                }
                
                if (metadataFields.isNotEmpty()) {
                    FieldGroup("🔖 Metadata & IDs", metadataFields)
                }
                
                if (otherFields.isNotEmpty()) {
                    FieldGroup("📝 Other Fields", otherFields)
                }
                
                // Always show record type at the end
                recordFields["Record Type"]?.let { recordType ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    RecordFieldItem(
                        fieldName = "Record Type",
                        fieldValue = recordType
                    )
                }
            }
        }
    }
}

@Composable
fun FieldGroup(title: String, fields: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        fields.forEach { (fieldName, fieldValue) ->
            RecordFieldItem(
                fieldName = fieldName,
                fieldValue = fieldValue
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun RecordFieldItemCompact(fieldName: String, fieldValue: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = fieldName + ":",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = if (fieldValue.length > 50) fieldValue.take(50) + "..." else fieldValue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

/**
 * Extracts all fields from a Health Connect record using reflection.
 * Returns a map of field names to their string representations.
 * This extracts ALL data including nested objects, lists, maps, etc.
 */
fun extractAllRecordFields(record: Any): Map<String, String> {
    val fields = mutableMapOf<String, String>()
    val clazz = record.javaClass

    // Add class name
    fields["Record Type"] = clazz.simpleName

    // Extract all getter methods - get ALL fields
    clazz.methods
        .filter {
            it.parameterCount == 0 &&
            it.name.startsWith("get") &&
            it.name != "getClass" &&
            !it.name.startsWith("getClass") // Exclude getClass methods
        }
        .sortedBy { it.name }
        .forEach { method ->
            try {
                val value = method.invoke(record)
                val fieldName = formatFieldName(method.name.removePrefix("get"))
                
                // For complex objects, try to extract their properties too
                val fieldValue = if (value != null && value !is String && value !is Number && value !is Boolean && 
                    !value.javaClass.name.startsWith("java.") && 
                    !value.javaClass.name.startsWith("kotlin.") &&
                    !value.javaClass.name.startsWith("androidx.health.connect.client.time")) {
                    // It's a complex object - extract its properties
                    extractComplexObjectValue(value)
                } else {
                    formatFieldValue(value)
                }
                
                fields[fieldName] = fieldValue
            } catch (e: SecurityException) {
                // Permission-related error - skip this field
                val fieldName = formatFieldName(method.name.removePrefix("get"))
                fields[fieldName] = "<permission denied>"
            } catch (e: Throwable) {
                val fieldName = formatFieldName(method.name.removePrefix("get"))
                fields[fieldName] = "<error: ${e.message?.take(50)}>"
            }
        }

    return fields
}

/**
 * Extracts value from complex objects by getting their properties.
 */
fun extractComplexObjectValue(obj: Any): String {
    return try {
        val clazz = obj.javaClass
        val properties = mutableListOf<String>()
        
        // Try to get all getter methods from the object
        clazz.methods
            .filter {
                it.parameterCount == 0 &&
                it.name.startsWith("get") &&
                it.name != "getClass"
            }
            .take(20) // Limit to prevent too much recursion
            .forEach { method ->
                try {
                    val propValue = method.invoke(obj)
                    val propName = method.name.removePrefix("get")
                    properties.add("$propName: ${formatFieldValue(propValue)}")
                } catch (e: Exception) {
                    // Skip if we can't get the property
                }
            }
        
        if (properties.isEmpty()) {
            // Fallback to toString
            obj.toString()
        } else {
            "{${properties.joinToString(", ")}}"
        }
    } catch (e: Exception) {
        // If extraction fails, use toString
        obj.toString()
    }
}

/**
 * Formats field names to be more readable (e.g., "StartTime" -> "Start Time")
 */
fun formatFieldName(name: String): String {
    return name.replace(Regex("([A-Z])"), " $1")
        .trim()
        .replaceFirstChar { it.uppercaseChar() }
}

/**
 * Formats field values to be more readable.
 */
fun formatFieldValue(value: Any?): String {
    if (value == null) return "null"
    
    return when (value) {
        is List<*> -> {
            if (value.isEmpty()) {
                "[]"
            } else {
                // Format lists with better readability
                val items = value.take(10).joinToString(separator = "\n  ") { item ->
                    "• ${formatFieldValue(item)}"
                }
                val more = if (value.size > 10) "\n  ... and ${value.size - 10} more" else ""
                "[${if (value.size <= 3) items.replace("\n  ", ", ") else "\n  $items$more\n"}]"
            }
        }
        is Map<*, *> -> {
            if (value.isEmpty()) {
                "{}"
            } else {
                // Format maps with better readability
                val entries = value.entries.take(10).joinToString(separator = "\n  ") { (k, v) ->
                    "${formatFieldValue(k)}: ${formatFieldValue(v)}"
                }
                val more = if (value.size > 10) "\n  ... and ${value.size - 10} more" else ""
                "{${if (value.size <= 3) entries.replace("\n  ", ", ") else "\n  $entries$more\n"}}"
            }
        }
        is java.time.Instant -> {
            java.time.ZonedDateTime.ofInstant(value, java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }
        is java.time.ZonedDateTime -> {
            value.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }
        is java.time.LocalDate -> {
            value.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
        is java.time.LocalTime -> {
            value.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        }
        is Boolean -> value.toString()
        is Number -> value.toString()
        is String -> value
        else -> {
            // For complex objects, try to extract meaningful info
            try {
                val clazz = value.javaClass
                // If it's a known Health Connect type, try to get toString
                if (clazz.name.startsWith("androidx.health.connect.client")) {
                    value.toString()
                } else {
                    // For other objects, show class name and try to get key properties
                    val simpleName = clazz.simpleName
                    "$simpleName(${value.toString().take(100)})"
                }
            } catch (e: Exception) {
                value.toString()
            }
        }
    }
}

/**
 * Composable for displaying a single record field.
 */
@Composable
fun RecordFieldItem(fieldName: String, fieldValue: String) {
    var isExpanded by remember { mutableStateOf(false) }
    val isLongValue = fieldValue.length > 100
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fieldName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (isLongValue) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Show Less" else "Show More",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = if (isLongValue && !isExpanded) {
                    fieldValue.take(100) + "..."
                } else {
                    fieldValue
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                fontFamily = FontFamily.Monospace,
                lineHeight = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}
