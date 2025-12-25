package com.example.health.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Data class representing health data summary for UI display.
 */
data class HealthDataUISummary(
    val categoryName: String,
    val recordTypes: Map<String, Int> // Map of record type name to count
)

/**
 * Displays all health data organized by category in a scrollable list.
 * 
 * @param allHealthData Map of category name to map of record type name to count
 * @param modifier Modifier for the composable
 */
@Composable
fun AllHealthDataDisplay(
    allHealthData: Map<String, Map<String, Int>>,
    modifier: Modifier = Modifier
) {
    if (allHealthData.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "No health data to display. Fetch data first.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            // Summary header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "All Health Data Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val totalCategories = allHealthData.size
                    val totalRecordTypes = allHealthData.values.sumOf { it.size }
                    
                    val totalRecords = allHealthData.values.sumOf { innerMap ->
                        innerMap.values.sum()
                    }
                    
                    val categoriesWithData = allHealthData.count { 
                        it.value.values.any { count -> count > 0 } 
                    }
                    
                    Text(
                        text = "$totalCategories categories • $totalRecordTypes types • $totalRecords total records",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$categoriesWithData categories with data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Display each category
        items(allHealthData.toList()) { (categoryName, recordTypes) ->
            val totalRecords = recordTypes.values.sum()
            val typesWithData = recordTypes.values.count { it > 0 }
            
            ExpandableCategory(
                categoryName = categoryName,
                totalTypes = recordTypes.size,
                totalRecords = totalRecords,
                typesWithData = typesWithData,
                recordTypes = recordTypes
            )
        }
    }
}

@Composable
fun ExpandableCategory(
    categoryName: String,
    totalTypes: Int,
    totalRecords: Int,
    typesWithData: Int,
    recordTypes: Map<String, Int>
) {
    var expanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Category Header (Clickable)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$totalTypes types • $totalRecords records",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Expanded Content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recordTypes.forEach { (recordTypeName, count) ->
                    HealthRecordTypeItem(
                        recordTypeName = recordTypeName,
                        recordCount = count,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
