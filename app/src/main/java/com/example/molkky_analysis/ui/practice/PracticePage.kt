package com.example.molkky_analysis.ui.practice

import androidx.compose.material.icons.filled.Delete // UserManagementDialogで使用
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape // SimpleRectanglePlaceholderで使用 (今回は未使用)
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.compose.ui.window.Dialog // AlertDialogを使用するため直接は不要
import com.example.molkky_analysis.data.model.User
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown // EnvConfigDialogで使用
// import androidx.compose.material.icons.filled.Close // SessionTabsUIのコメントアウト部分で使用案あり
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.util.Log
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticePage(
    viewModel: PracticeViewModel,
    onReturnToHome: () -> Unit
) {
    Log.d("PracticePage_Entry", "PracticePage Composable INVOKED")
    val uiState by viewModel.practiceSessionState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Log.d("PracticePage", "Showing loading indicator")
        }
        return
    }

    val currentSession = uiState.currentSessionState
    Log.d("PracticePage", "isLoading is false. currentSession: $currentSession, currentActiveSessionId: ${uiState.currentActiveSessionId}, all sessions count: ${uiState.sessions.size}")

    // --- ダイアログ表示ロジック ---
    // currentSessionがnullでも表示される可能性のあるダイアログ (確認ダイアログなど)
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

    if (uiState.showDeleteSessionConfirmDialog) {
        val sessionNameToDelete = uiState.sessionToDeleteId?.let { id ->
            uiState.sessions[id]?.currentUserName ?: "Session $id"
        } ?: "Selected Session"
        DeleteSessionConfirmDialog(
            sessionName = sessionNameToDelete,
            onConfirm = viewModel::confirmDeleteSession,
            onCancel = viewModel::cancelDeleteSession
        )
    }

    if (uiState.showDeleteDistanceConfirmDialog) { // これは currentSession がないと distanceToDelete が設定されないはず
        DeleteDistanceConfirmationDialog(
            distance = uiState.distanceToDelete ?: 0f,
            onConfirm = viewModel::confirmDeleteDistance,
            onCancel = viewModel::cancelDeleteDistance
        )
    }
    if (uiState.showDeleteUserConfirmDialog) { // グローバルなユーザー削除
        DeleteUserConfirmationDialog(
            user = uiState.userToDelete,
            onConfirm = viewModel::confirmDeleteUser,
            onCancel = viewModel::cancelDeleteUser
        )
    }
    if (uiState.showUserSwitchConfirmDialog) { // 特定セッションのユーザー切り替え確認
        AlertDialog(
            onDismissRequest = viewModel::cancelUserSwitch,
            title = { Text("Switch User") },
            text = { Text("Current practice data is unsaved. Save changes before switching user?") },
            confirmButton = {
                Button(onClick = { viewModel.confirmAndSwitchUser(saveCurrent = true) }) {
                    Text("Save and Switch")
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.confirmAndSwitchUser(saveCurrent = false) }) {
                    Text("Discard and Switch")
                }
            }
        )
    }
    if (uiState.showAddDistanceDialog) { // currentSessionがnullでもダイアログ自体は表示できるが、confirmはcurrentSessionに依存
        AddDistanceDialog(
            onDismiss = viewModel::cancelAddDistanceDialog,
            onConfirm = { distanceStr ->
                viewModel.confirmAddDistance(distanceStr)
            }
        )
    }


    // currentSession が null の場合は、セッションタブとフォールバックメッセージのみ表示
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SessionTabsUI(uiState = uiState, viewModel = viewModel)
        }
    ) { paddingValues ->
        if (currentSession == null) {
            Column(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No active session. Please select or add a session.")
                Log.e("PracticePage", "currentSession is null after loading. Cannot render main content area.")
            }
            return@Scaffold
        }

        // currentSession が null でない場合のメインコンテンツ
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Name Button と Env Config Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = viewModel::onNameButtonClicked) {
                    Text(currentSession.currentUserName)
                }
                Button(onClick = viewModel::onEnvConfigButtonClicked) {
                    Text("Env Config")
                }
            }
            Spacer(Modifier.height(16.dp))

            // AngleButton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AngleButton(label = "LEFT", isSelected = currentSession.selectedAngle == "LEFT", onClick = { viewModel.selectAngle("LEFT") })
                AngleButton(label = "CENTER", isSelected = currentSession.selectedAngle == "CENTER", onClick = { viewModel.selectAngle("CENTER") })
                AngleButton(label = "RIGHT", isSelected = currentSession.selectedAngle == "RIGHT", onClick = { viewModel.selectAngle("RIGHT") })
            }
            Spacer(Modifier.height(16.dp))

            // 「OK」「FAIL」ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                val isButtonActuallyEnabled = currentSession.activeDistance != null
                Log.d("PracticePage", "OK/Fail Buttons: isButtonActuallyEnabled = $isButtonActuallyEnabled (activeDistance: ${currentSession.activeDistance})")

                Button(
                    onClick = { if (isButtonActuallyEnabled) viewModel.addThrow(true) },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isButtonActuallyEnabled) Color(0xFFADDDCE) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isButtonActuallyEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = isButtonActuallyEnabled
                ) { Text("OK", fontSize = 18.sp) }

                Button(
                    onClick = { if (isButtonActuallyEnabled) viewModel.addThrow(false) },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isButtonActuallyEnabled) Color(0xFFFFC0CB) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isButtonActuallyEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = isButtonActuallyEnabled
                ) { Text("FAIL", fontSize = 18.sp) }
            }
            Spacer(Modifier.height(16.dp))

            // 距離リスト
            if (currentSession.configuredDistances.isEmpty()) {
                Text("Add practice distances using the '+ Add Distance' button.", modifier = Modifier.padding(vertical = 20.dp))
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(currentSession.configuredDistances, key = { it }) { distance ->
                        val throwsAtThisDistance = currentSession.throwsGroupedByDistance[distance] ?: emptyList()
                        val successes = throwsAtThisDistance.count { it.isSuccess }
                        val attempts = throwsAtThisDistance.size
                        val isActive = currentSession.activeDistance == distance
                        DistanceRowItem(
                            distance = distance,
                            successCount = successes,
                            attemptCount = attempts,
                            isActive = isActive,
                            onTap = { viewModel.selectDistance(distance) },
                            onLongPress = { viewModel.requestDeleteDistance(distance) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Add Distance と Undo ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp) // 水平方向のスペース
            ) {
                Button(
                    onClick = viewModel::requestAddDistanceDialog,
                    modifier = Modifier.weight(1f) // ★ weight を適用
                ) {
                    Text("Add Distance")
                }
                Button(
                    onClick = viewModel::undo,
                    enabled = currentSession.canUndo,
                    modifier = Modifier.weight(1f) // ★ weight を適用
                ) {
                    Text("Undo")
                }
            }
            Spacer(Modifier.height(16.dp))

            // Save と Return ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = viewModel::saveCurrentSession, // 現在のセッションのみ保存
                    enabled = currentSession.isDirty && currentSession.drafts.isNotEmpty()
                ) { Text("Save Session") }
                Button(onClick = {
                    if (uiState.sessions.values.any { it.isDirty && it.drafts.isNotEmpty() }) { // 全セッションのダーティ状態を確認
                        viewModel.requestExitConfirmation()
                    } else {
                        onReturnToHome()
                    }
                }) { Text("Return") }
            }
        } // End of Main Content Column (if currentSession != null)
    } // End of Scaffold

    // --- currentSession に依存するダイアログ ---
    // これらは currentSession が null でない場合にのみ意味を持つか、表示されるべき
    if (currentSession != null) {
        if (uiState.showUserDialog) {
            UserManagementDialog(
                availableUsers = uiState.availableUsers,
                currentUser = uiState.availableUsers.find { it.id == currentSession.currentUserId },
                onDismiss = viewModel::dismissUserDialog,
                onUserSelected = { user -> viewModel.switchUserForCurrentSession(user.id) },
                onAddNewUser = { userName -> viewModel.addNewUserAndSwitch(userName) },
                onDeleteUserClicked = { user -> viewModel.requestDeleteUser(user) }, // これはグローバルだが、ダイアログのトリガーはここ
                errorMessage = uiState.userDialogErrorMessage,
                currentUserId = currentSession.currentUserId // 削除ボタンの有効化判定に使用
            )
        }
        if (uiState.showEnvConfigDialog) {
            EnvConfigDialog(
                initialWeather = currentSession.sessionWeather,
                initialTemperature = currentSession.sessionTemperature,
                initialHumidity = currentSession.sessionHumidity,
                initialSoil = currentSession.sessionSoil,
                onDismiss = viewModel::dismissEnvConfigDialog,
                onReset = viewModel::resetEnvironmentConfiguration,
                onApply = { weather, temperature, humidity, soil ->
                    viewModel.updateSessionWeather(weather)
                    viewModel.updateSessionTemperature(temperature)
                    viewModel.updateSessionHumidity(humidity)
                    viewModel.updateSessionSoil(soil)
                    viewModel.dismissEnvConfigDialog()
                }
            )
        }
        // BackHandler は currentSession が null でない場合にのみ意味がある
        BackHandler(enabled = currentSession.isDirty && currentSession.drafts.isNotEmpty()) {
            viewModel.requestExitConfirmation()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) // Add ExperimentalFoundationApi
@Composable
fun SessionTabsUI(uiState: PracticeUiState, viewModel: PracticeViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    ScrollableTabRow(
        selectedTabIndex = uiState.activeSessionTabs.indexOf(uiState.currentActiveSessionId)
            .coerceAtLeast(0).let { if (uiState.activeSessionTabs.isEmpty() && it == -1) 0 else it }
            .coerceAtMost(if (uiState.activeSessionTabs.isEmpty()) 0 else uiState.activeSessionTabs.size - 1)
    ) {
        uiState.activeSessionTabs.forEachIndexed { index, sessionId ->
            val sessionTabInfo = uiState.sessions[sessionId]
            val interactionSource = remember { MutableInteractionSource() }
            var pressJob by remember { mutableStateOf<Job?>(null) }
            var isPressed by remember { mutableStateOf(false) } // 押下状態を追跡

            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> {
                            isPressed = true
                            // 以前の長押しジョブがあればキャンセル
                            pressJob?.cancel()
                            // 新しい長押しジョブを開始
                            pressJob = coroutineScope.launch {
                                Log.d("SessionTabsUI_Interaction", "Press detected for session ID: $sessionId. Starting long press timer.")
                                delay(500L) // 長押しとみなす時間 (ミリ秒)
                                // このdelayが終わるまでに PressInteraction.Release や PressInteraction.Cancel が来なければ長押しとみなす
                                if (isPressed) { // まだ押下中であれば長押しアクションを実行
                                    Log.i("SessionTabsUI_Interaction", "Long press detected for session ID: $sessionId via InteractionSource")
                                    if (uiState.sessions.size > 1) {
                                        viewModel.requestDeleteSession(sessionId)
                                    } else {
                                        Log.d("SessionTabsUI_Interaction", "Long press on last session (InteractionSource), delete denied for $sessionId.")
                                        Toast.makeText(context, "Cannot delete the last session", Toast.LENGTH_SHORT).show()
                                    }
                                    pressJob = null // 長押し処理後はジョブをクリア
                                }
                            }
                        }
                        is PressInteraction.Release -> {
                            isPressed = false
                            pressJob?.cancel() // 指が離れたら長押しタイマーをキャンセル
                            pressJob = null
                            Log.d("SessionTabsUI_Interaction", "Release detected for session ID: $sessionId. Cancelling long press timer.")
                        }
                        is PressInteraction.Cancel -> {
                            isPressed = false
                            pressJob?.cancel() // ジェスチャーがキャンセルされたら同様にキャンセル
                            pressJob = null
                            Log.d("SessionTabsUI_Interaction", "Cancel detected for session ID: $sessionId. Cancelling long press timer.")
                        }
                    }
                }
            }

            Tab(
                selected = uiState.currentActiveSessionId == sessionId,
                onClick = {
                    Log.i("SessionTabsUI_TabEvent", "Tab's own onClick triggered for session ID: $sessionId")
                    // 長押しがトリガーされた場合は、通常のクリック（選択）は行わないようにする
                    // ただし、InteractionSourceの仕組み上、ReleaseがonClickより先に評価されるとは限らないため、
                    // このonClickは常に呼ばれる可能性がある。
                    // 長押しでダイアログ表示→ダイアログ外タップでRelease→onClick発動、という流れも考えられる。
                    // より厳密には、長押し処理中にフラグを立ててonClickを抑制するなどの工夫が必要になる場合がある。
                    // 今回はまず、長押し検知ができるかを優先する。
                    if (pressJob == null || pressJob?.isCompleted == true || pressJob?.isCancelled == true) {
                        // 長押し処理が実行されていない、または完了/キャンセルされていれば通常のクリック処理
                        viewModel.selectSession(sessionId)
                    } else {
                        Log.d("SessionTabsUI_TabEvent", "onClick for $sessionId suppressed due to active pressJob.")
                    }
                },
                interactionSource = interactionSource, // TabにInteractionSourceを渡す
                text = { Text(sessionTabInfo?.currentUserName?.take(10) ?: "S ${index + 1}") }
            )
        }

        // 「+」タブ
        if (uiState.sessions.size < PracticeViewModel.MAX_SESSIONS) {
            Tab(
                selected = false,
                onClick = { viewModel.addSession() },
                text = { Text("+") }
            )
        } else if (uiState.activeSessionTabs.isEmpty() && uiState.sessions.isEmpty()) {
            Tab(
                selected = false,
                onClick = { viewModel.addSession() },
                text = { Text("+") }
            )
        }
    }
}

@Composable
fun DeleteUserConfirmationDialog(
    user: User?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    if (user == null) return // 何も表示しない
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete User") },
        text = { Text("Are you sure you want to delete user \"${user.name}\"? All associated throw data will also be deleted. This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // 目立つようにエラーカラーに
            ) {
                Text("Delete User")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

@Composable
fun DeleteSessionConfirmDialog(
    sessionName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete Session") },
        text = { Text("Are you sure you want to delete session \"$sessionName\"? All unsaved data in this session will be lost. This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Session")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvConfigDialog(
    initialWeather: String?,
    initialTemperature: Float?,
    initialHumidity: Float?,
    initialSoil: String?, // Represents Ground
    onDismiss: () -> Unit,
    onReset: () -> Unit, // ★ パラメータ追加
    onApply: (weather: String?, temperature: Float?, humidity: Float?, soil: String?) -> Unit
) {
    var weather by remember { mutableStateOf(initialWeather ?: "") }
    var temperatureText by remember { mutableStateOf(initialTemperature?.toString() ?: "") }
    var humidityText by remember { mutableStateOf(initialHumidity?.toString() ?: "") }
    var ground by remember { mutableStateOf(initialSoil ?: "") } // Ground selection stored in 'soil'

    val weatherOptions = listOf("Sunny", "Cloudy", "Snowy", "Rainy", "Indoors")
    var weatherExpanded by remember { mutableStateOf(false) }

    val groundOptions = listOf("Soil", "Hard Court", "Lawn", "Artificial Grass")
    var groundExpanded by remember { mutableStateOf(false) }

    // リセット時にダイアログ内のローカルステートもクリアするためのLaunchedEffect
    // initialXxx のいずれかがnullに変わったら（ViewModelがリセットされたら）ローカルステートも追従
    // ただし、一部のみnullの場合はクリアしないように調整が必要なら、より複雑なロジックになる
    LaunchedEffect(initialWeather, initialTemperature, initialHumidity, initialSoil) {
        // ViewModel側の値が全てnullになった場合に限り、ダイアログ内の表示もクリアする
        // (ユーザーが意図的に一部のフィールドのみクリアした場合との区別のため)
        // もしくは、onResetが呼ばれたことを示す別のフラグをViewModelから渡すなどの方法も考えられる
        if (initialWeather == null && initialTemperature == null && initialHumidity == null && initialSoil == null) {
            if (weather.isNotEmpty() || temperatureText.isNotEmpty() || humidityText.isNotEmpty() || ground.isNotEmpty()) {
                weather = ""
                temperatureText = ""
                humidityText = ""
                ground = ""
            }
        } else { // ViewModel側の値が更新されたら、ローカルステートもそれに追従
            if (initialWeather != weather) weather = initialWeather ?: ""
            if (initialTemperature?.toString() != temperatureText) temperatureText = initialTemperature?.toString() ?: ""
            if (initialHumidity?.toString() != humidityText) humidityText = initialHumidity?.toString() ?: ""
            if (initialSoil != ground) ground = initialSoil ?: ""
        }
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Environment Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Weather Dropdown
                Box {
                    OutlinedTextField(
                        value = weather,
                        onValueChange = { }, // Not directly changed by typing
                        label = { Text("Weather") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "Select Weather", Modifier.clickable { weatherExpanded = true }) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = weatherExpanded,
                        onDismissRequest = { weatherExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        weatherOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    weather = selectionOption
                                    weatherExpanded = false
                                }
                            )
                        }
                    }
                }

                // Temperature TextField
                OutlinedTextField(
                    value = temperatureText,
                    onValueChange = { temperatureText = it },
                    label = { Text("Temperature (°C)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Humidity TextField
                OutlinedTextField(
                    value = humidityText,
                    onValueChange = { humidityText = it },
                    label = { Text("Humidity (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Ground (Soil) Dropdown
                Box {
                    OutlinedTextField(
                        value = ground,
                        onValueChange = { }, // Not directly changed by typing
                        label = { Text("Ground") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "Select Ground", Modifier.clickable { groundExpanded = true }) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = groundExpanded,
                        onDismissRequest = { groundExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        groundOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    ground = selectionOption
                                    groundExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(
                    weather.ifEmpty { null },
                    temperatureText.toFloatOrNull(),
                    humidityText.toFloatOrNull(),
                    ground.ifEmpty { null } // This is for the 'soil' field
                )
            }) { Text("Apply") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { // 右寄せにする
                Button(onClick = {
                    onReset()
                    // 下のLaunchedEffectがViewModelの変更を検知してローカルステートをクリアするはず
                }) { Text("Reset") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
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
        title = { Text("Add New Distance") },
        text = {
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text("Distance (m)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(distanceText) }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// AngleButton Composable
@Composable
fun AngleButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.size(width = 100.dp, height = 60.dp) // Adjusted size for consistency
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Placeholder for an icon or visual representation of the angle
//            SimpleRectanglePlaceholder(
//                modifier = Modifier.size(width = 40.dp, height = 20.dp), // Example size
//                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
//            )
//            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 10.sp) // Display the label text
        }
    }
}

// DistanceRowItem Composable
@Composable
fun DistanceRowItem(
    distance: Float,
    successCount: Int,
    attemptCount: Int,
    isActive: Boolean,
    onTap: () -> Unit,    // viewModel.selectDistance(distance) とログを含むラムダ
    onLongPress: () -> Unit // viewModel.requestDeleteDistance(distance) を含むラムダ
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent, MaterialTheme.shapes.medium)
            // .clickable(onClick = onTap) // ← この行は削除またはコメントアウト
            .pointerInput(Unit) { // key = Unit または key = distance など、必要に応じて設定
                detectTapGestures(
                    onTap = { _ -> // Offset情報 (_) は今回は使用しない
                        onTap()  // 渡されてきた onTap コールバックを実行
                    },
                    onLongPress = { _ -> // Offset情報 (_) は今回は使用しない
                        onLongPress() // 渡されてきた onLongPress コールバックを実行
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${String.format("%.1f", distance)}m", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("$successCount / $attemptCount", fontSize = 18.sp)
    }
}

// ConfirmExitDialog Composable
@Composable
fun ConfirmExitDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Unsaved Changes") },
        text = { Text("There are unsaved changes. Do you want to save them before exiting?") },
        confirmButton = {
            Button(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            Button(onClick = onDiscard) { Text("Discard") }
        },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

@Composable
fun DeleteDistanceConfirmationDialog(
    distance: Float,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete Distance") },
        text = { Text("Are you sure you want to delete ${String.format("%.1f", distance)}m from the configured distances? This will clear all practice data for this distance.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
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
    errorMessage: String?,
    onDeleteUserClicked: (User) -> Unit, // このパラメータは定義に必要
    currentUserId: Int                 // このパラメータは定義に必要
) {
    var newUserName by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Management") },
        text = {
            Column {
                if (availableUsers.isNotEmpty()) {
                    Text("Select existing user:", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(availableUsers, key = { it.id }) { user ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    // .clickable { onUserSelected(user) } // 行全体のクリックは削除アイコンと競合する可能性があるため、個別の要素に設定
                                    .padding(vertical = 4.dp), // 少しパディングを調整
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween // アイコンを右端に配置
                            ) {
                                // ラジオボタンとユーザー名を含むRow
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f) // 利用可能なスペースを占める
                                        .clickable { onUserSelected(user) } // この部分をクリックしてユーザー選択
                                ) {
                                    RadioButton(
                                        selected = (user.id == currentUser?.id),
                                        onClick = { onUserSelected(user) }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    // Textの Modifier.weight(1f, fill = false) は、Textが不必要にスペースを占めてIconButtonを押し出すのを防ぐ
                                    Text(user.name, modifier = Modifier.weight(1f, fill = false))
                                }

                                // ↓↓↓ ゴミ箱アイコン（削除ボタン）の追加 ↓↓↓
                                val canDeleteThisUser = user.id != currentUserId &&
                                        availableUsers.size > 1 &&
                                        !(user.id == 1 && user.name == "Player 1") // "Player 1" (ID=1) は削除不可とする例

                                IconButton(
                                    onClick = { onDeleteUserClicked(user) },
                                    enabled = canDeleteThisUser
                                ) {
                                    Icon(
                                        Icons.Filled.Delete, // androidx.compose.material.icons.filled.Delete を import してください
                                        contentDescription = "Delete ${user.name}",
                                        tint = if (canDeleteThisUser) MaterialTheme.colorScheme.error else Color.Gray
                                    )
                                }
                                // ↑↑↑ ゴミ箱アイコン（削除ボタン）の追加 ↑↑↑
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                Text("Create new user:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newUserName,
                    onValueChange = { newUserName = it },
                    label = { Text("New user name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newUserName.isNotBlank()) {
                            onAddNewUser(newUserName)
                        }
                        keyboardController?.hide()
                    }),
                    isError = errorMessage != null // エラーメッセージがあればisErrorをtrueに
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
                        }
                    },
                    enabled = newUserName.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Create")
                }
                LaunchedEffect(Unit) {
                    if (availableUsers.isEmpty()) focusRequester.requestFocus()
                }
            }
        },
        confirmButton = {}, // アクションはダイアログ内のボタンで行う
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}