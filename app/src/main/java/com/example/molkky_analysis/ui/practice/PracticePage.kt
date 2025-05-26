package com.example.molkky_analysis.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.example.molkky_analysis.data.local.FakeThrowDao // 削除
import com.example.molkky_analysis.data.local.AppDatabase // PreviewでViewModelを生成する場合に必要になる可能性
import com.example.molkky_analysis.data.repository.ThrowRepository // PreviewでViewModelを生成する場合に必要になる可能性
import com.example.molkky_analysis.ui.theme.Molkky_analysisTheme

// Iconの代わりに表示するシンプルな長方形のComposable
@Composable
fun SimpleRectanglePlaceholder(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(color, shape = RectangleShape)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticePage(
    viewModel: PracticeViewModel,
    onReturnToHome: () -> Unit
) {
    val uiState by viewModel.practiceSessionState.collectAsState()

    if (uiState.showExitConfirmDialog) {
        ConfirmExitDialog(
            onSave = {
                viewModel.confirmSaveAndExit()
                onReturnToHome()
            },
            onDiscard = {
                viewModel.confirmDiscardAndExit()
                onReturnToHome()
            },
            onCancel = viewModel::cancelExit
        )
    }

    BackHandler(enabled = uiState.isDirty) {
        viewModel.requestExitConfirmation()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Name Button & Env Config Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { /* TODO: viewModel.onNameClicked() */ }) {
                    SimpleRectanglePlaceholder()
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.currentUserName)
                }
                Button(onClick = { /* TODO: viewModel.onEnvConfigClicked() */ }) {
                    SimpleRectanglePlaceholder()
                    Spacer(Modifier.width(8.dp))
                    Text("Env Config")
                }
            }
            Spacer(Modifier.height(16.dp))

            // Session Tabs
            // TODO: Implement Session Tabs UI (SimpleRectanglePlaceholder を使用)

            Spacer(Modifier.height(16.dp))

            // Angle Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AngleButton(label = "LEFT", isSelected = uiState.selectedAngle == "LEFT", onClick = { viewModel.selectAngle("LEFT") })
                AngleButton(label = "CENTER", isSelected = uiState.selectedAngle == "CENTER", onClick = { viewModel.selectAngle("CENTER") })
                AngleButton(label = "RIGHT", isSelected = uiState.selectedAngle == "RIGHT", onClick = { viewModel.selectAngle("RIGHT") })
            }
            Spacer(Modifier.height(16.dp))

            // OK / FAIL Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = { viewModel.addThrow(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADDDCE)),
                    enabled = uiState.activeDistance != null
                ) { Text("OK", fontSize = 18.sp) }
                Button(
                    onClick = { viewModel.addThrow(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC0CB)),
                    enabled = uiState.activeDistance != null
                ) { Text("FAIL", fontSize = 18.sp) }
            }
            Spacer(Modifier.height(16.dp))

            // Distance Rows / List
            if (uiState.throwsGroupedByDistance.isEmpty()) {
                Text("距離を追加して練習を開始してください。", modifier = Modifier.padding(vertical = 20.dp))
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.throwsGroupedByDistance.entries.toList(), key = { it.key }) { entry ->
                        val distance = entry.key
                        val throwsAtDistance = entry.value
                        val successes = throwsAtDistance.count { it.isSuccess }
                        DistanceRowItem(
                            distance = distance,
                            successCount = successes,
                            attemptCount = throwsAtDistance.size,
                            isActive = uiState.activeDistance == distance,
                            onTap = { viewModel.selectDistance(distance) },
                            onLongPress = { /* TODO: viewModel.editDistance(distance) */ }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Add Distance Row Button & Undo Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { /* TODO: viewModel.onAddDistanceClicked() */ },
                    modifier = Modifier.weight(1f)
                ) {
                    SimpleRectanglePlaceholder()
                    Spacer(Modifier.width(4.dp))
                    Text("Add Distance")
                }
                Button(
                    onClick = viewModel::undo,
                    enabled = uiState.canUndo,
                    modifier = Modifier.weight(1f)
                ) {
                    SimpleRectanglePlaceholder()
                    Spacer(Modifier.width(4.dp))
                    Text("Undo")
                }
            }
            Spacer(Modifier.height(16.dp))

            // Save Button & Return Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = viewModel::save,
                    enabled = uiState.isDirty
                ) { Text("Save") }
                Button(onClick = {
                    if (uiState.isDirty) {
                        viewModel.requestExitConfirmation()
                    } else {
                        onReturnToHome()
                    }
                }) { Text("Return") }
            }
        }
    }
}

@Composable
fun AngleButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.size(width = 100.dp, height = 60.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SimpleRectanglePlaceholder(
                modifier = Modifier.size(width = 40.dp, height = 20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Text(label, fontSize = 10.sp) // 必要ならラベルも表示
        }
    }
}

@Composable
fun DistanceRowItem(
    distance: Float,
    successCount: Int,
    attemptCount: Int,
    isActive: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent, MaterialTheme.shapes.medium)
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${distance}m", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("$successCount / $attemptCount", fontSize = 18.sp)
    }
}

@Composable
fun ConfirmExitDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Unsaved Changes") },
        text = { Text("There are unsaved changes. Do you want to save them?") },
        confirmButton = {
            Button(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            Button(onClick = onDiscard) { Text("Discard") }
        },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

// Preview用のダミーViewModelとFakeThrowDaoを削除しました
// class DummyPracticeViewModel : PracticeViewModel(...) // 削除
// class FakeThrowDao : ThrowDao { ... } // 削除

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PracticePagePreview() {
    Molkky_analysisTheme {
        // このプレビューは、PracticeViewModel の適切なインスタンス化方法がないと
        // そのままでは動作しないか、エラーになる可能性があります。
        // DIコンテナを使用している場合は、プレビュー用の設定が必要になることがあります。
        // 簡単なプレビューのためには、ViewModelのモックを手動で作成し、
        // 必要なダミーデータを返すようにする必要があります。
        // 例:
        // val dummyViewModel = PracticeViewModel(
        //     throwRepository = ThrowRepository(AppDatabase.getDatabase(LocalContext.current).throwDao()), // これは実際のDBアクセスを試みる
        //     userId = 1
        // )
        // PracticePage(viewModel = dummyViewModel, onReturnToHome = {})

        // 現時点では、プレビューはコメントアウトするか、適切なモックViewModelを用意してください。
        Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
            Text("PracticePage Preview Area: ViewModel instance needed for actual preview.",
                modifier = Modifier.align(Alignment.Center).padding(16.dp))
        }
    }
}