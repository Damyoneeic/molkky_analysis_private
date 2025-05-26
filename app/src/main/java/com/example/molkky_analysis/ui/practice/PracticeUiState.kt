package com.example.molkky_analysis.ui.practice

import com.example.molkky_analysis.data.model.User
import java.util.UUID // For unique session IDs

// Holds configuration for each session tab
data class SessionTabUiInfo(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    // Configuration specific to this session tab
    val configuredDistances: List<Float> = listOf(3.0f, 3.5f, 4.0f),
    val activeDistance: Float? = 3.0f, // The distance selected as active for this session tab
    val sessionWeather: String? = null,
    val sessionHumidity: Float? = null,
    val sessionTemperature: Float? = null,
    val sessionSoil: String? = null,
    val sessionMolkkyWeight: Float? = null // Keep this if it's part of env config
)

data class PracticeUiState(
    val currentUserId: Int = 1, // Remains the ID of the logged-in user
    val currentUserName: String = "Player 1",

    // Data related to all drafts for the current user (not per-session-tab)
    val throwsGroupedByDistance: Map<Float, List<SessionThrowDisplayData>> = emptyMap(),
    val canUndo: Boolean = false, // True if any drafts exist for the current user
    val isDirty: Boolean = false, // True if any drafts exist for the current user

    // UI state for the PracticePage, derived from the active session tab
    val currentSessionConfiguredDistances: List<Float> = listOf(3.0f, 3.5f, 4.0f),
    val currentSessionActiveDistance: Float? = 3.0f,
    val selectedAngle: String = "CENTER", // Angle for the next throw, can be global

    // Environment settings for the next throw, derived from the active session tab
    val currentSessionWeather: String? = null,
    val currentSessionHumidity: Float? = null,
    val currentSessionTemperature: Float? = null,
    val currentSessionSoil: String? = null,
    val currentSessionMolkkyWeight: Float? = null,

    // Dialog visibility states
    val showExitConfirmDialog: Boolean = false,
    val showAddDistanceDialog: Boolean = false,
    val showUserDialog: Boolean = false,
    val showEnvConfigDialog: Boolean = false,
    val showUserSwitchConfirmDialog: Boolean = false,

    // User management related states
    val availableUsers: List<User> = emptyList(),
    val userDialogErrorMessage: String? = null,
    val pendingUserSwitchId: Int? = null,

    // Session Tab Management
    val sessionTabs: List<SessionTabUiInfo> = listOf(SessionTabUiInfo(displayName = "S1")), // Default first session
    val activeSessionId: String = sessionTabs.firstOrNull()?.id ?: "" // ID of the active session tab
)

// This remains unchanged, represents a single throw in the UI list for a distance
data class SessionThrowDisplayData(
    val distance: Float,
    val isSuccess: Boolean,
    val angle: String
)