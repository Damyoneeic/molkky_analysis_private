package com.example.molkky_analysis.ui.data_display

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.tooling.preview.Preview // コメントアウト済み
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.molkky_analysis.ui.theme.Molkky_analysisTheme

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
            TopAppBar(title = { Text("投擲データ一覧") })
            // TODO: ユーザーフィルター用ドロップダウンなどをここに追加可能
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
                    Text("記録された投擲データはありません。")
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
                                // TODO: 編集画面へ遷移 または 編集ダイアログ表示
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
                Text("Return to Home")
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
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
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
                    text = if (recordData.record.isSuccess) "成功" else "失敗",
                    color = if (recordData.record.isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("ユーザー: ${recordData.userName}", style = MaterialTheme.typography.bodySmall)
            Text("日時: ${recordData.formattedTimestamp}", style = MaterialTheme.typography.bodySmall)

            recordData.record.weather?.let { Text("天気: $it", style = MaterialTheme.typography.bodySmall) }
            recordData.record.temperature?.let { Text("気温: $it℃", style = MaterialTheme.typography.bodySmall) }
            // 他の環境情報も同様に表示

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "編集")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                }
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