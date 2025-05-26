package com.example.molkky_analysis.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // items(list, key=...) を使うために必要
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
// import androidx.compose.ui.platform.LocalContext // ViewModelのプレビュー用インスタンス化がなければ不要
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
// import androidx.compose.ui.tooling.preview.Preview // コメントアウト済み
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.molkky_analysis.ui.theme.Molkky_analysisTheme
import com.example.molkky_analysis.data.model.User
import com.example.molkky_analysis.ui.practice.PracticeViewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

// Iconの代わりに表示するシンプルな長方形のComposable (変更なし)
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

    // --- Dialogs ---
    if (uiState.showExitConfirmDialog) {
        ConfirmExitDialog(
            onSave = {
                viewModel.confirmSaveAndExit()
                onReturnToHome() // 画面遷移はViewModelの処理が終わった後
            },
            onDiscard = {
                viewModel.confirmDiscardAndExit()
                onReturnToHome() // 画面遷移はViewModelの処理が終わった後
            },
            onCancel = viewModel::cancelExit
        )
    }

    if (uiState.showAddDistanceDialog) {
        AddDistanceDialog(
            onDismiss = viewModel::cancelAddDistance,
            onConfirm = { distanceStr ->
                viewModel.confirmAddDistance(distanceStr)
            }
        )
    }

    if (uiState.showUserDialog) { // [cite: 2]
        // Placeholder User Dialog
        AlertDialog(
            onDismissRequest = viewModel::dismissUserDialog,
            title = { Text("User Management") },
            text = { Text("User selection/creation UI will be here.") },
            confirmButton = {
                Button(onClick = viewModel::dismissUserDialog) { Text("OK") }
            }
        )
    }

    if (uiState.showUserDialog) {
        UserManagementDialog(
            availableUsers = uiState.availableUsers,
            currentUser = uiState.availableUsers.find { it.id == uiState.currentUserId },
            onDismiss = viewModel::dismissUserDialog,
            onUserSelected = { user ->
                viewModel.switchUser(user.id)
                viewModel.dismissUserDialog() // 選択後ダイアログを閉じる
            },
            onAddNewUser = { userName ->
                viewModel.addNewUser(userName)
                // addNewUser内で成功すればダイアログは閉じるので、ここでは何もしないか、
                // エラー時のみダイアログを閉じない等の制御をViewModelで行う
            },
            errorMessage = uiState.userDialogErrorMessage
        )
    }

    if (uiState.showUserSwitchConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::cancelUserSwitch,
            title = { Text("ユーザー切り替え") },
            text = { Text("現在の練習データが保存されていません。保存しますか？") },
            confirmButton = {
                Button(onClick = { viewModel.confirmAndSwitchUser(saveCurrent = true) }) {
                    Text("保存して切り替え")
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.confirmAndSwitchUser(saveCurrent = false) }) {
                    Text("破棄して切り替え")
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        )
    }

    if (uiState.showEnvConfigDialog) { // [cite: 2]
        // Placeholder Env Config Dialog
        // ここで実際の環境設定入力UIを作成します。
        // 簡単な例として、いくつかの TextField を持つ AlertDialog を表示します。
        var weatherText by remember { mutableStateOf(uiState.sessionWeather ?: "") }
        var tempText by remember { mutableStateOf(uiState.sessionTemperature?.toString() ?: "") }
        // 他の環境変数も同様に remember で状態を持つ

        AlertDialog(
            onDismissRequest = viewModel::dismissEnvConfigDialog,
            title = { Text("Environment Configuration") },
            text = {
                Column {
                    TextField(
                        value = weatherText,
                        onValueChange = { weatherText = it },
                        label = { Text("Weather") }
                    )
                    TextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        label = { Text("Temperature (°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    // TODO: Add fields for humidity, soil, molkkyWeight
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateSessionWeather(weatherText.ifEmpty { null })
                    viewModel.updateSessionTemperature(tempText.toFloatOrNull())
                    // TODO: Update other env settings
                    viewModel.dismissEnvConfigDialog()
                }) { Text("Apply") }
            },
            dismissButton = {
                Button(onClick = viewModel::dismissEnvConfigDialog) { Text("Cancel") }
            }
        )
    }


    // --- Back Handler for Unsaved Data ---
    BackHandler(enabled = uiState.isDirty) { // [cite: 2]
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
            // Name Button & Env Config Button [cite: 2]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = viewModel::onNameButtonClicked) { // Connect to ViewModel
                    SimpleRectanglePlaceholder() // Placeholder for icon
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.currentUserName)
                }
                Button(onClick = viewModel::onEnvConfigButtonClicked) { // Connect to ViewModel
                    SimpleRectanglePlaceholder() // Placeholder for icon
                    Spacer(Modifier.width(8.dp))
                    Text("Env Config")
                }
            }
            Spacer(Modifier.height(16.dp))

            // Session Tabs (Placeholder UI) [cite: 2]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Session:", style = MaterialTheme.typography.titleSmall)
                // Example: static tabs for now
                listOf("1", "2", "+").forEach { tabLabel ->
                    Button(onClick = { /* TODO: viewModel.selectSession(tabLabel) */ }) {
                        Text(tabLabel)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Angle Selector (Existing, no changes needed here) [cite: 2]
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

            // OK / FAIL Buttons (Existing, no changes needed here) [cite: 2]

//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
//            ) {
//                Button(
//                    onClick = { viewModel.addThrow(true) },
//                    modifier = Modifier.weight(1f),
//                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADDDCE)),
//                    enabled = uiState.activeDistance != null // Enable only if a distance is active
//                ) { Text("OK", fontSize = 18.sp) }
//                Button(
//                    onClick = { viewModel.addThrow(false) },
//                    modifier = Modifier.weight(1f),
//                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC0CB)),
//                    enabled = uiState.activeDistance != null // Enable only if a distance is active
//                ) { Text("FAIL", fontSize = 18.sp) }
//            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                val isButtonActuallyEnabled = uiState.activeDistance != null // ★ 条件を変数に格納

                Button(
                    onClick = {
                        android.util.Log.d("PracticePage", "OK Button clicked. activeDistance: ${uiState.activeDistance}")
                        if (isButtonActuallyEnabled) { // ★ 再度ここでチェック
                            viewModel.addThrow(true)
                        } else {
                            android.util.Log.w("PracticePage", "OK Button was clicked but it should be disabled!")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isButtonActuallyEnabled) Color(0xFFADDDCE) else MaterialTheme.colorScheme.surfaceVariant, // ★ 色も明示的に変えてみる
                        contentColor = if (isButtonActuallyEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = isButtonActuallyEnabled // ★ ここは変更なし
                ) { Text("OK", fontSize = 18.sp) }

                Button(
                    onClick = {
                        android.util.Log.d("PracticePage", "FAIL Button clicked. activeDistance: ${uiState.activeDistance}")
                        if (isButtonActuallyEnabled) { // ★ 再度ここでチェック
                            viewModel.addThrow(false)
                        } else {
                            android.util.Log.w("PracticePage", "FAIL Button was clicked but it should be disabled!")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isButtonActuallyEnabled) Color(0xFFFFC0CB) else MaterialTheme.colorScheme.surfaceVariant, // ★ 色も明示的に変えてみる
                        contentColor = if (isButtonActuallyEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = isButtonActuallyEnabled // ★ ここは変更なし
                ) { Text("FAIL", fontSize = 18.sp) }
            }
            Spacer(Modifier.height(16.dp))

            // Distance Rows / List [cite: 3]
            // Iterates over configuredDistances and gets counts from throwsGroupedByDistance
            if (uiState.configuredDistances.isEmpty()) {
                Text("「+ 距離追加」ボタンで練習する距離を追加してください。", modifier = Modifier.padding(vertical = 20.dp))
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.configuredDistances, key = { it }) { distance -> // Use configuredDistances
                        val throwsAtThisDistance = uiState.throwsGroupedByDistance[distance] ?: emptyList()
                        val successes = throwsAtThisDistance.count { it.isSuccess }
                        val attempts = throwsAtThisDistance.size
                        DistanceRowItem(
                            distance = distance,
                            successCount = successes,
                            attemptCount = attempts,
                            isActive = uiState.activeDistance == distance,
                            onTap = { viewModel.selectDistance(distance) },
                            onLongPress = { /* TODO: viewModel.editDistance(distance) */ }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))


            // Add Distance Row Button & Undo Button [cite: 3]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = viewModel::requestAddDistance, // Connect to ViewModel
                    modifier = Modifier.weight(1f)
                ) {
                    SimpleRectanglePlaceholder() // Placeholder for icon
                    Spacer(Modifier.width(4.dp))
                    Text("Add Distance")
                }
                Button(
                    onClick = viewModel::undo,
                    enabled = uiState.canUndo,
                    modifier = Modifier.weight(1f)
                ) {
                    SimpleRectanglePlaceholder() // Placeholder for icon
                    Spacer(Modifier.width(4.dp))
                    Text("Undo")
                }
            }
            Spacer(Modifier.height(16.dp))

            // Save Button & Return Button [cite: 3]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = viewModel::save,
                    enabled = uiState.isDirty
                ) { Text("Save") }
                Button(onClick = {
                    if (uiState.isDirty) { // [cite: 2]
                        viewModel.requestExitConfirmation()
                    } else {
                        onReturnToHome()
                    }
                }) { Text("Return") }
            }
        }
    }
}

// AddDistanceDialog Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDistanceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var distanceText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新しい距離を追加") },
        text = {
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text("距離 (m)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(distanceText) }) {
                Text("追加")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}


// AngleButton Composable (変更なし)
@Composable
fun AngleButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    // ... (previous implementation)
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

// DistanceRowItem Composable (変更なし)
@Composable
fun DistanceRowItem(
    distance: Float,
    successCount: Int,
    attemptCount: Int,
    isActive: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    // ... (previous implementation)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent, MaterialTheme.shapes.medium)
            .clickable(onClick = onTap)
            //.pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) } // 必要であれば長押し対応
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${distance}m", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("$successCount / $attemptCount", fontSize = 18.sp)
    }
}

// ConfirmExitDialog Composable (変更なし)
@Composable
fun ConfirmExitDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    // ... (previous implementation)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementDialog(
    availableUsers: List<User>,
    currentUser: User?,
    onDismiss: () -> Unit,
    onUserSelected: (User) -> Unit,
    onAddNewUser: (String) -> Unit,
    errorMessage: String?
) {
    var newUserName by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ユーザー管理") },
        text = {
            Column {
                if (availableUsers.isNotEmpty()) {
                    Text("既存のユーザーを選択:", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) { // 高さに制限を設ける
                        items(availableUsers, key = { it.id }) { user ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserSelected(user) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (user.id == currentUser?.id),
                                    onClick = { onUserSelected(user) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(user.name)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                Text("新しいユーザーを作成:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newUserName,
                    onValueChange = { newUserName = it },
                    label = { Text("新しいユーザー名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newUserName.isNotBlank()) {
                            onAddNewUser(newUserName)
                            newUserName = "" // 成功したらクリア
                        }
                        keyboardController?.hide()
                    }),
                    isError = errorMessage != null // エラーがあればTextFieldもエラー表示にするなど
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newUserName.isNotBlank()) {
                            onAddNewUser(newUserName)
                            // newUserName = "" // ViewModel側で成功時にダイアログを閉じるならここでのクリアは不要かも
                        }
                    },
                    enabled = newUserName.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("作成")
                }
                // ダイアログ初回表示時にTextFieldにフォーカスを当てる (オプション)
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        },
        confirmButton = {
            // 通常、このダイアログでは選択または作成が主なので、
            // OKボタンは必須ではないかもしれない。ここでは閉じるボタンのみ。
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("閉じる")
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

/* // Preview関連はコメントアウト済み
@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PracticePagePreview() {
    // ...
}
*/