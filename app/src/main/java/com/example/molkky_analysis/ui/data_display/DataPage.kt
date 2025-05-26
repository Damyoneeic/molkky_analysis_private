package com.example.molkky_analysis.ui.data_display

import androidx.compose.foundation.layout.*
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
fun DataScreen(
    pageLabel: String,
    onReturnToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("This is $pageLabel", style = MaterialTheme.typography.headlineMedium)
        // TODO: Add Data display and editing UI content here based on spec [cite: 21]
        Spacer(Modifier.height(16.dp))
        Button(onClick = onReturnToHome) {
            Text("Return to Home")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DataScreenPreview() {
    Molkky_analysisTheme {
        DataScreen(pageLabel = "Data Page", onReturnToHome = {})
    }
}