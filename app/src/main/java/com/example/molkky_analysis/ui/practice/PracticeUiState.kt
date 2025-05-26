package com.example.molkky_analysis.ui.practice

import com.example.molkky_analysis.data.model.User // Userモデルをインポート

data class PracticeUiState(
    val currentUserId: Int = 1, // Default to Player 1 if available
    val currentUserName: String = "Player 1",
    val throwsGroupedByDistance: Map<Float, List<SessionThrowDisplayData>> = emptyMap(),
    val configuredDistances: List<Float> = listOf(4.0f),
    val activeDistance: Float? = 4.0f,
    val selectedAngle: String = "CENTER",

    val sessionWeather: String? = null,
    val sessionHumidity: Float? = null,
    val sessionTemperature: Float? = null,
    val sessionSoil: String? = null,
    val sessionMolkkyWeight: Float? = null,

    val canUndo: Boolean = false,
    val isDirty: Boolean = false,

    val showExitConfirmDialog: Boolean = false,
    val showAddDistanceDialog: Boolean = false,
    val showUserDialog: Boolean = false,
    val showEnvConfigDialog: Boolean = false,

    val availableUsers: List<User> = emptyList(),
    val userDialogErrorMessage: String? = null,
    val showUserSwitchConfirmDialog: Boolean = false,
    val pendingUserSwitchId: Int? = null,

    val showDeleteDistanceConfirmDialog: Boolean = false,
    val distanceToDelete: Float? = null,
    val showDeleteUserConfirmDialog: Boolean = false,
    val userToDelete: User? = null

)

data class SessionThrowDisplayData(
    val distance: Float,
    val isSuccess: Boolean,
    val angle: String
)