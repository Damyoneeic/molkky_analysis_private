package com.example.molkky_analysis.ui.practice

import androidx.compose.material.icons.filled.Delete
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.molkky_analysis.ui.theme.Molkky_analysisTheme
import com.example.molkky_analysis.data.model.User
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.input.pointer.pointerInput // Import this for detectTapGestures
import androidx.compose.foundation.gestures.detectTapGestures // Import this for detectTapGestures

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
                onReturnToHome()
            },
            onDiscard = {
                viewModel.confirmDiscardAndExit()
                onReturnToHome()
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

    if (uiState.showUserDialog) {
        UserManagementDialog(
            availableUsers = uiState.availableUsers,
            currentUser = uiState.availableUsers.find { it.id == uiState.currentUserId },
            onDismiss = viewModel::dismissUserDialog,
            onUserSelected = { user ->
                viewModel.switchUser(user.id)
                // viewModel.dismissUserDialog() // Dialog closed by selection or explicit close button
            },
            onAddNewUser = { userName ->
                viewModel.addNewUser(userName)
            },
            onDeleteUserClicked = { user ->
                viewModel.requestDeleteUser(user)
            },
            errorMessage = uiState.userDialogErrorMessage,
            currentUserId = uiState.currentUserId
        )
    }

    if (uiState.showUserSwitchConfirmDialog) {
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
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        )
    }

    // Only one EnvConfigDialog block is needed
    if (uiState.showEnvConfigDialog) {
        EnvConfigDialog(
            initialWeather = uiState.sessionWeather,
            initialTemperature = uiState.sessionTemperature,
            initialHumidity = uiState.sessionHumidity,
            initialSoil = uiState.sessionSoil, // Ground maps to soil
            onDismiss = viewModel::dismissEnvConfigDialog,
            onApply = { weather, temperature, humidity, soil ->
                viewModel.updateSessionWeather(weather)
                viewModel.updateSessionTemperature(temperature)
                viewModel.updateSessionHumidity(humidity)
                viewModel.updateSessionSoil(soil) // Ground maps to soil
                viewModel.dismissEnvConfigDialog()
            }
        )
    }


    // New: Delete Distance Confirmation Dialog
    if (uiState.showDeleteDistanceConfirmDialog) {
        DeleteDistanceConfirmationDialog(
            distance = uiState.distanceToDelete ?: 0f, // Provide a default or handle null
            onConfirm = viewModel::confirmDeleteDistance,
            onCancel = viewModel::cancelDeleteDistance
        )
    }

    if (uiState.showDeleteUserConfirmDialog) {
        DeleteUserConfirmationDialog(
            user = uiState.userToDelete,
            onConfirm = viewModel::confirmDeleteUser,
            onCancel = viewModel::cancelDeleteUser
        )
    }


    // --- Back Handler for Unsaved Data ---
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = viewModel::onNameButtonClicked) {
//                    SimpleRectanglePlaceholder()
//                    Spacer(Modifier.width(8.dp))
                    Text(uiState.currentUserName)
                }
                Button(onClick = viewModel::onEnvConfigButtonClicked) {
//                    SimpleRectanglePlaceholder()
//                    Spacer(Modifier.width(8.dp))
                    Text("Env Config")
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Session:", style = MaterialTheme.typography.titleSmall)
                listOf("1", "2", "+").forEach { tabLabel ->
                    Button(onClick = { /* TODO: viewModel.selectSession(tabLabel) */ }) {
                        Text(tabLabel)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                val isButtonActuallyEnabled = uiState.activeDistance != null

                Button(
                    onClick = {
                        if (isButtonActuallyEnabled) {
                            viewModel.addThrow(true)
                        }
                    },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isButtonActuallyEnabled) Color(0xFFADDDCE) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isButtonActuallyEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = isButtonActuallyEnabled
                ) { Text("OK", fontSize = 18.sp) }

                Button(
                    onClick = {
                        if (isButtonActuallyEnabled) {
                            viewModel.addThrow(false)
                        }
                    },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isButtonActuallyEnabled) Color(0xFFFFC0CB) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isButtonActuallyEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = isButtonActuallyEnabled
                ) { Text("FAIL", fontSize = 18.sp) }
            }
            Spacer(Modifier.height(16.dp))

            if (uiState.configuredDistances.isEmpty()) {
                Text("Add practice distances using the '+ Add Distance' button.", modifier = Modifier.padding(vertical = 20.dp))
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.configuredDistances, key = { it }) { distance ->
                        val throwsAtThisDistance = uiState.throwsGroupedByDistance[distance] ?: emptyList()
                        val successes = throwsAtThisDistance.count { it.isSuccess }
                        val attempts = throwsAtThisDistance.size
                        DistanceRowItem(
                            distance = distance,
                            successCount = successes,
                            attemptCount = attempts,
                            isActive = uiState.activeDistance == distance,
                            onTap = { viewModel.selectDistance(distance) },
                            onLongPress = { viewModel.requestDeleteDistance(distance) } // Add long press action
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = viewModel::requestAddDistance,
                    modifier = Modifier.weight(1f)
                ) {
//                    SimpleRectanglePlaceholder()
//                    Spacer(Modifier.width(4.dp))
                    Text("Add Distance")
                }
                Button(
                    onClick = viewModel::undo,
                    enabled = uiState.canUndo,
                    modifier = Modifier.weight(1f)
                ) {
//                    SimpleRectanglePlaceholder()
//                    Spacer(Modifier.width(4.dp))
                    Text("Undo")
                }
            }
            Spacer(Modifier.height(16.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvConfigDialog(
    initialWeather: String?,
    initialTemperature: Float?,
    initialHumidity: Float?,
    initialSoil: String?, // Represents Ground
    onDismiss: () -> Unit,
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
            Button(onClick = onDismiss) { Text("Cancel") }
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
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) } // Enable long press detection
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${String.format("%.1f", distance)}m", fontSize = 18.sp, fontWeight = FontWeight.Bold) // Format distance
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