package com.example.molkky_analysis.ui.settings // Assuming same package

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.molkky_analysis.ui.theme.Molkky_analysisTheme

@Composable
fun SettingsScreenLayout(
    pageLabel: String, // e.g., "Page 5"
    onReturnToPage1: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "welcome to $pageLabel",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        // Add Page 5 specific content here
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onReturnToPage1) {
            Text("return to page1")
        }
    }
}

@Preview(showBackground = true, name = "Settings Page Preview")
@Composable
fun SettingsPagePreview() {
    Molkky_analysisTheme {
        SettingsScreenLayout(
            pageLabel = "Page 5 (Settings)",
            onReturnToPage1 = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}