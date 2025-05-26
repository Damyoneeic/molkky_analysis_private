package com.example.molkky_analysis.ui.data_display

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.tooling.preview.Preview // コメントアウト済み
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Molkky_analysisTheme is not used in this file directly for preview, but good to keep if other previews are added.
// import com.example.molkky_analysis.ui.theme.Molkky_analysisTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    viewModel: DataViewModel, // ViewModel を受け取る
    onReturnToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Throw Records") }) // Changed to English
            // TODO: Add user filter dropdown here if needed
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.throwRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No throw records available.") // Changed to English
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.throwRecords, key = { it.record.id }) { displayableRecord ->
                        ThrowRecordItem(
                            recordData = displayableRecord,
                            onEdit = {
                                // TODO: Navigate to edit screen or show edit dialog
                                // viewModel.requestEditRecord(displayableRecord.record)
                            },
                            onDelete = {
                                viewModel.deleteThrowRecord(displayableRecord.record)
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onReturnToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Return to Home") // Already in English
            }
        }
    }
}

@Composable
fun ThrowRecordItem(
    recordData: DisplayableThrowRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded } // Make the whole card clickable to expand/collapse
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${recordData.record.distance}m - ${recordData.record.angle}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (recordData.record.isSuccess) "Success" else "Fail", // Changed to English
                    color = if (recordData.record.isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("User: ${recordData.userName}", style = MaterialTheme.typography.bodySmall) // Changed to English
            Text("Date: ${recordData.formattedTimestamp}", style = MaterialTheme.typography.bodySmall) // Changed to English

            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Environmental Details:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) // Added title for section
                Spacer(Modifier.height(4.dp))
                recordData.record.weather?.let { Text("Weather: $it", style = MaterialTheme.typography.bodySmall) }
                recordData.record.temperature?.let { Text("Temperature: $it°C", style = MaterialTheme.typography.bodySmall) }
                recordData.record.humidity?.let { Text("Humidity: $it%", style = MaterialTheme.typography.bodySmall) }
                recordData.record.soil?.let { Text("Soil: $it", style = MaterialTheme.typography.bodySmall) }
                recordData.record.molkkyWeight?.let { Text("Molkky Weight: ${it}g", style = MaterialTheme.typography.bodySmall) }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, // Changed to SpaceBetween for better icon distribution
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row { // Group Edit and Delete buttons
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit") // Changed to English
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) // Changed to English
                    }
                }
                // Expansion Icon
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand", // Changed to English
                )
            }
        }
    }
}

/* // Preview関連はコメントアウト済み
@Preview(showBackground = true)
@Composable
fun DataScreenPreview() {
    Molkky_analysisTheme {
        // DataScreen(pageLabel = "Data Page", onReturnToHome = {}) // ViewModelが必要になる
    }
}
*/