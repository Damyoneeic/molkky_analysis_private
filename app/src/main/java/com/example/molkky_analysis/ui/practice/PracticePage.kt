package com.example.molkky_analysis.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.compose.ui.window.Dialog // Not directly used, AlertDialog is used
import com.example.molkky_analysis.data.model.User
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
// import androidx.compose.material.icons.filled.Close // Not used directly for tab removal icon
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController


@Composable
fun SimpleRectanglePlaceholder(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) {
    Box(modifier = modifier.size(24.dp).background(color, shape = RectangleShape))
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
                viewModel.confirmSaveAndExit() // VM handles dialog close
                onReturnToHome() // Navigate after VM logic
            },
            onDiscard = {
                viewModel.confirmDiscardAndExit() // VM handles dialog close
                onReturnToHome() // Navigate after VM logic
            },
            onCancel = viewModel::cancelExit
        )
    }

    if (uiState.showAddDistanceDialog) {
        AddDistanceDialog(
            onDismiss = viewModel::cancelAddDistance,
            onConfirm = viewModel::confirmAddDistanceToActiveSession
        )
    }

    if (uiState.showUserDialog) {
        UserManagementDialog(
            availableUsers = uiState.availableUsers,
            currentUser = uiState.availableUsers.find { it.id == uiState.currentUserId },
            onDismiss = viewModel::dismissUserDialog,
            onUserSelected = { user ->
                viewModel.switchUser(user.id)
                // viewModel.dismissUserDialog() // Dialog usually closed by action or explicit close button
            },
            onAddNewUser = viewModel::addNewUser,
            errorMessage = uiState.userDialogErrorMessage
        )
    }

    if (uiState.showUserSwitchConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::cancelUserSwitch, // This was the previously problematic line
            title = { Text("Switch User") },
            text = { Text("Current session has unsaved changes. Save them before switching user?") }, // Clarified text
            confirmButton = { Button(onClick = { viewModel.confirmAndSwitchUser(saveCurrent = true) }) { Text("Save and Switch") } },
            dismissButton = { Button(onClick = { viewModel.confirmAndSwitchUser(saveCurrent = false) }) { Text("Discard and Switch") } }
        )
    }

    if (uiState.showEnvConfigDialog) {
        EnvConfigDialog(
            initialWeather = uiState.currentSessionWeather,
            initialTemperature = uiState.currentSessionTemperature,
            initialHumidity = uiState.currentSessionHumidity,
            initialSoil = uiState.currentSessionSoil,
            initialMolkkyWeight = uiState.currentSessionMolkkyWeight,
            onDismiss = viewModel::dismissEnvConfigDialog,
            onApply = { weather, temp, humidity, soil, molkkyWeight ->
                viewModel.updateActiveSessionWeather(weather)
                viewModel.updateActiveSessionTemperature(temp)
                viewModel.updateActiveSessionHumidity(humidity)
                viewModel.updateActiveSessionSoil(soil)
                viewModel.updateActiveSessionMolkkyWeight(molkkyWeight)
                viewModel.dismissEnvConfigDialog()
            }
        )
    }

    BackHandler(enabled = uiState.isDirty) { viewModel.requestExitConfirmation() }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp, vertical = 8.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = viewModel::onNameButtonClicked) {
                    SimpleRectanglePlaceholder()
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.currentUserName.take(10) + if (uiState.currentUserName.length > 10) "..." else "")
                }
                Button(onClick = viewModel::onEnvConfigButtonClicked) {
                    SimpleRectanglePlaceholder()
                    Spacer(Modifier.width(8.dp))
                    Text("Env Config")
                }
            }
            Spacer(Modifier.height(8.dp))

            val activeSessionIndex = uiState.sessionTabs.indexOfFirst { it.id == uiState.activeSessionId }.coerceAtLeast(0)
            if (uiState.sessionTabs.isNotEmpty()) { // Ensure tabs exist before rendering TabRow
                ScrollableTabRow(
                    selectedTabIndex = activeSessionIndex,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.sessionTabs.forEach { session -> // Removed index from forEach as it's not used
                        Tab(
                            selected = uiState.activeSessionId == session.id,
                            onClick = { viewModel.selectSession(session.id) },
                            text = { Text(session.displayName) },
                            modifier = Modifier.pointerInput(session.id) {
                                detectTapGestures(
                                    onLongPress = {
                                        if (uiState.sessionTabs.size > 1) {
                                            // Consider a confirmation dialog before removing a session with drafts
                                            viewModel.removeSession(session.id)
                                        }
                                    }
                                )
                            }
                        )
                    }
                    Tab(
                        selected = false,
                        onClick = viewModel::addSession,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Session")
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
                val isThrowButtonEnabled = uiState.currentSessionActiveDistance != null
                Button(
                    onClick = { if (isThrowButtonEnabled) viewModel.addThrow(true) },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isThrowButtonEnabled) Color(0xFFADDDCE) else MaterialTheme.colorScheme.surfaceVariant),
                    enabled = isThrowButtonEnabled
                ) { Text("OK", fontSize = 18.sp) }
                Button(
                    onClick = { if (isThrowButtonEnabled) viewModel.addThrow(false) },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isThrowButtonEnabled) Color(0xFFFFC0CB) else MaterialTheme.colorScheme.surfaceVariant),
                    enabled = isThrowButtonEnabled
                ) { Text("FAIL", fontSize = 18.sp) }
            }
            Spacer(Modifier.height(16.dp))

            if (uiState.currentSessionConfiguredDistances.isEmpty()) {
                Text("Add practice distances for this session using '+ Add Distance'.", modifier = Modifier.padding(vertical = 20.dp))
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.currentSessionConfiguredDistances, key = { distanceValue -> "dist_${uiState.activeSessionId}_$distanceValue" }) { distanceValue ->
                        val throwsAtThisDistance = uiState.throwsGroupedByDistance[distanceValue] ?: emptyList()
                        val successes = throwsAtThisDistance.count { it.isSuccess }
                        val attempts = throwsAtThisDistance.size
                        DistanceRowItem(
                            distance = distanceValue,
                            successCount = successes,
                            attemptCount = attempts,
                            isActive = uiState.currentSessionActiveDistance == distanceValue,
                            onTap = { viewModel.selectDistanceForActiveSession(distanceValue) },
                            onLongPress = { /* TODO: Edit distance for active session */ }
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
                Button(onClick = viewModel::requestAddDistance, modifier = Modifier.weight(1f)) {
                    SimpleRectanglePlaceholder()
                    Spacer(Modifier.width(4.dp))
                    Text("Add Distance")
                }
                Button(onClick = viewModel::undo, enabled = uiState.canUndo, modifier = Modifier.weight(1f)) {
                    SimpleRectanglePlaceholder()
                    Spacer(Modifier.width(4.dp))
                    Text("Undo")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = viewModel::save, enabled = uiState.isDirty) { Text("Save Session") }
                Button(onClick = {
                    if (uiState.isDirty) viewModel.requestExitConfirmation() else onReturnToHome()
                }) { Text("Return") }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvConfigDialog(
    initialWeather: String?,
    initialTemperature: Float?,
    initialHumidity: Float?,
    initialSoil: String?,
    initialMolkkyWeight: Float?,
    onDismiss: () -> Unit,
    onApply: (weather: String?, temperature: Float?, humidity: Float?, soil: String?, molkkyWeight: Float?) -> Unit
) {
    var weather by remember { mutableStateOf(initialWeather ?: "") }
    var temperatureText by remember { mutableStateOf(initialTemperature?.toString() ?: "") }
    var humidityText by remember { mutableStateOf(initialHumidity?.toString() ?: "") }
    var ground by remember { mutableStateOf(initialSoil ?: "") }
    var molkkyWeightText by remember { mutableStateOf(initialMolkkyWeight?.toString() ?: "") }

    val weatherOptions = listOf("", "Sunny", "Cloudy", "Snowy", "Rainy", "Indoors")
    var weatherExpanded by remember { mutableStateOf(false) }

    val groundOptions = listOf("", "Soil", "Hard Court", "Lawn", "Artificial Grass")
    var groundExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Environment Configuration (for current session)") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Box {
                        OutlinedTextField(
                            value = weather,
                            onValueChange = { },
                            label = { Text("Weather") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "Select Weather", Modifier.clickable { weatherExpanded = true }) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = weatherExpanded, onDismissRequest = { weatherExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                            weatherOptions.forEach { selectionOption ->
                                DropdownMenuItem(text = { Text(selectionOption.ifEmpty { "(Clear)" }) }, onClick = { weather = selectionOption; weatherExpanded = false })
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = temperatureText,
                        onValueChange = { temperatureText = it },
                        label = { Text("Temperature (°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = humidityText,
                        onValueChange = { humidityText = it },
                        label = { Text("Humidity (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Box {
                        OutlinedTextField(
                            value = ground,
                            onValueChange = { },
                            label = { Text("Ground") },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "Select Ground", Modifier.clickable { groundExpanded = true }) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = groundExpanded, onDismissRequest = { groundExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                            groundOptions.forEach { selectionOption ->
                                DropdownMenuItem(text = { Text(selectionOption.ifEmpty { "(Clear)" }) }, onClick = { ground = selectionOption; groundExpanded = false })
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = molkkyWeightText,
                        onValueChange = { molkkyWeightText = it },
                        label = { Text("Molkky Weight (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(
                    weather.ifEmpty { null },
                    temperatureText.toFloatOrNull(),
                    humidityText.toFloatOrNull(),
                    ground.ifEmpty { null },
                    molkkyWeightText.toFloatOrNull()
                )
            }) { Text("Apply to Session") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDistanceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var distanceText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Distance (to current session)") },
        text = {
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text("Distance (m)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = { Button(onClick = { onConfirm(distanceText) }) { Text("Add") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AngleButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.size(width = 100.dp, height = 60.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SimpleRectanglePlaceholder(
                modifier = Modifier.size(width = 40.dp, height = 20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 10.sp)
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
        Text("${String.format("%.1f", distance)}m", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
        title = { Text("Unsaved Session Changes") }, // Clarified title
        text = { Text("The current session has unsaved changes. Save them before exiting?") }, // Clarified text
        confirmButton = { Button(onClick = onSave) { Text("Save Session") } }, // Clarified text
        dismissButton = { Button(onClick = onDiscard) { Text("Discard Session") } }, // Clarified text
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true) // Added for consistency
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
        title = { Text("User Management") },
        text = {
            Column {
                if (availableUsers.isNotEmpty()) {
                    Text("Select existing user:", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(availableUsers, key = { user -> "user_select_${user.id}" }) { user -> // Unique key
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserSelected(user); /* onDismiss() // Optionally close here */ }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (user.id == currentUser?.id), onClick = { onUserSelected(user); /* onDismiss() */ })
                                Spacer(Modifier.width(8.dp))
                                Text(user.name)
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
                        if (newUserName.isNotBlank()) onAddNewUser(newUserName)
                        keyboardController?.hide()
                    }),
                    isError = errorMessage != null
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { if (newUserName.isNotBlank()) onAddNewUser(newUserName) },
                    enabled = newUserName.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Create") }
                LaunchedEffect(Unit) { if (availableUsers.isEmpty() && newUserName.isEmpty()) focusRequester.requestFocus() }
            }
        },
        confirmButton = {}, // Actions are inline
        dismissButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}