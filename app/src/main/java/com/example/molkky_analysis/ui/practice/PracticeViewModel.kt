package com.example.molkky_analysis.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.User
import com.example.molkky_analysis.data.repository.IThrowRepository
import com.example.molkky_analysis.data.repository.IUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

// secret number is 3000
class PracticeViewModel(
    private val throwRepository: IThrowRepository,
    private val userRepository: IUserRepository,
    initialUserId: Int
) : ViewModel() {

    private val _currentActiveUserIdFlow = MutableStateFlow(initialUserId)

    // Cache for session configurations per user
    private val userSessionTabsCache = mutableMapOf<Int, List<SessionTabUiInfo>>()
    private val userActiveSessionIdCache = mutableMapOf<Int, String>()

    // Default session factory
    private fun createDefaultSessionTabs(): List<SessionTabUiInfo> {
        val defaultSession = SessionTabUiInfo(displayName = "S1")
        return listOf(defaultSession)
    }
    private fun getInitialActiveSessionId(tabs: List<SessionTabUiInfo>): String = tabs.firstOrNull()?.id ?: UUID.randomUUID().toString() // Ensure a valid ID if tabs are empty


    // Internal mutable state for all UI configurations including sessions
    private val _internalUiState = MutableStateFlow(
        PracticeUiState(
            currentUserId = initialUserId,
            sessionTabs = userSessionTabsCache.getOrElse(initialUserId) { createDefaultSessionTabs() },
            activeSessionId = userActiveSessionIdCache.getOrElse(initialUserId) {
                getInitialActiveSessionId(userSessionTabsCache.getOrElse(initialUserId) { createDefaultSessionTabs() })
            }
        )
    )

    // Flow for the active session ID
    private val activeSessionIdFlow: StateFlow<String> = _internalUiState
        .map { it.activeSessionId }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _internalUiState.value.activeSessionId)

    // Drafts flow is now specific to the active user AND active session
    private val draftsFlow: StateFlow<List<ThrowDraft>> = combine(
        _currentActiveUserIdFlow,
        activeSessionIdFlow
    ) { userId, sessionId -> Pair(userId, sessionId) }
        .flatMapLatest { (userId, sessionId) ->
            if (sessionId.isNotEmpty()) {
                throwRepository.getDrafts(userId, sessionId)
            } else {
                flowOf(emptyList()) // No active session, no drafts
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    val availableUsers: StateFlow<List<User>> = userRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val practiceSessionState: StateFlow<PracticeUiState> =
        combine(
            draftsFlow, // Now session-specific drafts
            _internalUiState,
            availableUsers,
            _currentActiveUserIdFlow
        ) { sessionDrafts, internalState, users, activeUserId -> // Renamed drafts to sessionDrafts
            val currentUser = users.find { it.id == activeUserId }
            val activeSession = internalState.sessionTabs.find { it.id == internalState.activeSessionId }
                ?: internalState.sessionTabs.firstOrNull() // Fallback to first session
                ?: SessionTabUiInfo(displayName = "Error", id = UUID.randomUUID().toString()) // Absolute fallback with a valid new ID

            // Group throws from the current session's drafts
            val groupedThrows = sessionDrafts.groupBy { it.distance }
                .mapValues { entry ->
                    entry.value.map { draft ->
                        SessionThrowDisplayData(draft.distance, draft.isSuccess, draft.angle)
                    }
                }

            internalState.copy(
                currentUserId = activeUserId,
                currentUserName = currentUser?.name ?: "Player $activeUserId",
                throwsGroupedByDistance = groupedThrows, // Now reflects session-specific drafts
                canUndo = sessionDrafts.isNotEmpty(),    // Based on session-specific drafts
                isDirty = sessionDrafts.isNotEmpty(),    // Based on session-specific drafts
                availableUsers = users,
                currentSessionConfiguredDistances = activeSession.configuredDistances.distinct().sorted(),
                currentSessionActiveDistance = activeSession.activeDistance,
                currentSessionWeather = activeSession.sessionWeather,
                currentSessionHumidity = activeSession.sessionHumidity,
                currentSessionTemperature = activeSession.sessionTemperature,
                currentSessionSoil = activeSession.sessionSoil,
                currentSessionMolkkyWeight = activeSession.sessionMolkkyWeight,
                // Ensure activeSessionId in the emitted state is also correct
                activeSessionId = activeSession.id
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _internalUiState.value // Use a fully initialized PracticeUiState here
        )

    init {
        viewModelScope.launch {
            val user = userRepository.getUserById(_currentActiveUserIdFlow.value)
            _internalUiState.update {
                val currentId = it.currentUserId // Use currentUserId from state for consistency
                val tabs = userSessionTabsCache.getOrElse(currentId) { createDefaultSessionTabs() }
                val activeId = userActiveSessionIdCache.getOrElse(currentId) { getInitialActiveSessionId(tabs) }
                it.copy(
                    currentUserName = user?.name ?: "Player $currentId",
                    sessionTabs = tabs,
                    activeSessionId = activeId.ifEmpty { tabs.firstOrNull()?.id ?: UUID.randomUUID().toString() } // Ensure activeId is not empty
                )
            }
        }
    }

    fun selectSession(sessionId: String) {
        _internalUiState.update { state ->
            if (state.sessionTabs.any { it.id == sessionId }) {
                state.copy(activeSessionId = sessionId)
            } else {
                // If session ID is invalid, perhaps revert to the first tab or do nothing
                if (state.sessionTabs.isNotEmpty()) {
                    state.copy(activeSessionId = state.sessionTabs.first().id)
                } else {
                    // This case should ideally not happen if there's always at least one default tab
                    val defaultTabs = createDefaultSessionTabs()
                    state.copy(sessionTabs = defaultTabs, activeSessionId = defaultTabs.first().id)
                }
            }
        }
    }

    fun addSession() {
        _internalUiState.update { state ->
            val newSessionNumber = state.sessionTabs.size + 1
            var newDisplayName = "S$newSessionNumber"
            var count = newSessionNumber
            while(state.sessionTabs.any { it.displayName == newDisplayName}) {
                count++
                newDisplayName = "S$count"
            }
            val newSession = SessionTabUiInfo(displayName = newDisplayName) // ID is auto-generated
            state.copy(
                sessionTabs = state.sessionTabs + newSession,
                activeSessionId = newSession.id
            )
        }
    }

    fun removeSession(sessionIdToRemove: String) {
        _internalUiState.update { state ->
            if (state.sessionTabs.size <= 1) return@update state

            // Consider warning the user or handling drafts associated with sessionIdToRemove
            viewModelScope.launch {
                throwRepository.clearAllDrafts(state.currentUserId, sessionIdToRemove)
            } // Example: clear drafts of removed session

            val newTabs = state.sessionTabs.filterNot { it.id == sessionIdToRemove }
            val newActiveSessionId = if (state.activeSessionId == sessionIdToRemove) {
                newTabs.firstOrNull()?.id ?: UUID.randomUUID().toString() // Ensure valid ID
            } else {
                state.activeSessionId
            }
            state.copy(sessionTabs = newTabs, activeSessionId = newActiveSessionId.ifEmpty{newTabs.firstOrNull()?.id ?: UUID.randomUUID().toString()})
        }
    }

    private fun performUserSwitch(newUserId: Int) {
        val outgoingUserId = _internalUiState.value.currentUserId
        if (outgoingUserId != newUserId) {
            userSessionTabsCache[outgoingUserId] = _internalUiState.value.sessionTabs
            userActiveSessionIdCache[outgoingUserId] = _internalUiState.value.activeSessionId
        }

        _currentActiveUserIdFlow.value = newUserId

        val newInitialTabs = userSessionTabsCache.getOrElse(newUserId) { createDefaultSessionTabs() }
        val newInitialActiveId = userActiveSessionIdCache.getOrElse(newUserId) { getInitialActiveSessionId(newInitialTabs) }

        _internalUiState.update {
            it.copy(
                currentUserId = newUserId,
                sessionTabs = newInitialTabs,
                activeSessionId = newInitialActiveId.ifEmpty { newInitialTabs.firstOrNull()?.id ?: UUID.randomUUID().toString() },
                selectedAngle = "CENTER",
                showUserSwitchConfirmDialog = false,
                pendingUserSwitchId = null,
                userDialogErrorMessage = null
            )
        }
        viewModelScope.launch {
            val user = userRepository.getUserById(newUserId)
            _internalUiState.update { it.copy(currentUserName = user?.name ?: "Player $newUserId") }
        }
    }

    fun switchUser(userId: Int) {
        viewModelScope.launch {
            if (practiceSessionState.value.isDirty && practiceSessionState.value.currentUserId != userId) {
                _internalUiState.update { it.copy(showUserSwitchConfirmDialog = true, pendingUserSwitchId = userId) }
                return@launch
            }
            performUserSwitch(userId)
        }
    }

    fun cancelUserSwitch() {
        _internalUiState.update { currentState ->
            currentState.copy(
                showUserSwitchConfirmDialog = false,
                pendingUserSwitchId = null
            )
        }
    }

    fun confirmAndSwitchUser(saveCurrent: Boolean) {
        val pendingUserId = _internalUiState.value.pendingUserSwitchId ?: return
        val previousUserId = practiceSessionState.value.currentUserId
        val activeSessionIdForSaveOrDiscard = _internalUiState.value.activeSessionId

        viewModelScope.launch {
            if (practiceSessionState.value.isDirty) {
                if (saveCurrent) {
                    throwRepository.commitDrafts(previousUserId, activeSessionIdForSaveOrDiscard)
                } else {
                    throwRepository.clearAllDrafts(previousUserId, activeSessionIdForSaveOrDiscard)
                }
            }
            performUserSwitch(pendingUserId)
        }
    }

    fun addNewUser(userName: String) {
        viewModelScope.launch {
            if (userName.isNotBlank()) {
                val usersList = practiceSessionState.value.availableUsers
                val existingUser = usersList.find { it.name.equals(userName, ignoreCase = true) }

                if (existingUser == null) {
                    val newUser = User(name = userName, created_at = System.currentTimeMillis())
                    val newUserId = userRepository.insertUser(newUser)
                    if (newUserId > 0) {
                        performUserSwitch(newUserId.toInt())
                        dismissUserDialog()
                    } else {
                        _internalUiState.update { it.copy(userDialogErrorMessage = "Failed to create user.") }
                    }
                } else {
                    _internalUiState.update { it.copy(userDialogErrorMessage = "User name already exists.") }
                }
            } else {
                _internalUiState.update { it.copy(userDialogErrorMessage = "User name cannot be empty.") }
            }
        }
    }

    fun addThrow(isSuccess: Boolean) {
        viewModelScope.launch {
            val currentCombinedState = practiceSessionState.value // Use the publicly exposed state for decisions
            val activeSessionFromInternal = _internalUiState.value.sessionTabs.find { it.id == _internalUiState.value.activeSessionId }
                ?: return@launch // Should always find an active session

            val distanceForThrow = activeSessionFromInternal.activeDistance ?: return@launch
            val userIdForDraft = _currentActiveUserIdFlow.value
            val sessionIdForDraft = activeSessionFromInternal.id

            val newDraft = ThrowDraft(
                userId = userIdForDraft,
                sessionId = sessionIdForDraft,
                distance = distanceForThrow,
                angle = currentCombinedState.selectedAngle, // Use angle from combined state
                isSuccess = isSuccess,
                timestamp = Date().time,
                weather = activeSessionFromInternal.sessionWeather,
                humidity = activeSessionFromInternal.sessionHumidity,
                temperature = activeSessionFromInternal.sessionTemperature,
                soil = activeSessionFromInternal.sessionSoil,
                molkkyWeight = activeSessionFromInternal.sessionMolkkyWeight
            )
            throwRepository.insertDraft(newDraft)
        }
    }

    fun undo() {
        viewModelScope.launch {
            if (practiceSessionState.value.canUndo) {
                throwRepository.deleteLastDraft(_currentActiveUserIdFlow.value, _internalUiState.value.activeSessionId)
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            if (practiceSessionState.value.isDirty) {
                throwRepository.commitDrafts(_currentActiveUserIdFlow.value, _internalUiState.value.activeSessionId)
                // Don't close exit confirm dialog here directly, it's for specific exit flow.
                // Save just saves. If part of exit flow, confirmSaveAndExit handles dialog.
            }
        }
    }

    fun confirmDiscardAndExit() {
        viewModelScope.launch {
            if (_internalUiState.value.activeSessionId.isNotEmpty()) { // Ensure there's an active session to clear
                throwRepository.clearAllDrafts(_currentActiveUserIdFlow.value, _internalUiState.value.activeSessionId)
            }
            _internalUiState.update { it.copy(showExitConfirmDialog = false) }
        }
    }

    fun selectAngle(angle: String) { _internalUiState.update { it.copy(selectedAngle = angle) } }

    fun selectDistanceForActiveSession(distance: Float) {
        _internalUiState.update { state ->
            val updatedTabs = state.sessionTabs.map {
                if (it.id == state.activeSessionId) {
                    it.copy(activeDistance = distance)
                } else it
            }
            state.copy(sessionTabs = updatedTabs)
        }
    }

    fun requestAddDistance() { _internalUiState.update { it.copy(showAddDistanceDialog = true) } }
    fun cancelAddDistance() { _internalUiState.update { it.copy(showAddDistanceDialog = false) } }

    fun confirmAddDistanceToActiveSession(newDistanceStr: String) {
        val newDistance = newDistanceStr.toFloatOrNull()
        _internalUiState.update { state ->
            if (newDistance != null && newDistance > 0) {
                val updatedTabs = state.sessionTabs.map { tab ->
                    if (tab.id == state.activeSessionId) {
                        val updatedDistances = (tab.configuredDistances + newDistance).distinct().sorted()
                        tab.copy(
                            configuredDistances = updatedDistances,
                            activeDistance = newDistance
                        )
                    } else tab
                }
                state.copy(sessionTabs = updatedTabs, showAddDistanceDialog = false)
            } else {
                state.copy(showAddDistanceDialog = false)
            }
        }
    }

    fun updateActiveSessionWeather(weather: String?) {
        _internalUiState.update { state ->
            state.copy(sessionTabs = state.sessionTabs.map {
                if (it.id == state.activeSessionId) it.copy(sessionWeather = weather) else it
            })
        }
    }
    fun updateActiveSessionHumidity(humidity: Float?) {
        _internalUiState.update { state ->
            state.copy(sessionTabs = state.sessionTabs.map {
                if (it.id == state.activeSessionId) it.copy(sessionHumidity = humidity) else it
            })
        }
    }
    fun updateActiveSessionTemperature(temperature: Float?) {
        _internalUiState.update { state ->
            state.copy(sessionTabs = state.sessionTabs.map {
                if (it.id == state.activeSessionId) it.copy(sessionTemperature = temperature) else it
            })
        }
    }
    fun updateActiveSessionSoil(soil: String?) {
        _internalUiState.update { state ->
            state.copy(sessionTabs = state.sessionTabs.map {
                if (it.id == state.activeSessionId) it.copy(sessionSoil = soil) else it
            })
        }
    }
    fun updateActiveSessionMolkkyWeight(weight: Float?) {
        _internalUiState.update { state ->
            state.copy(sessionTabs = state.sessionTabs.map {
                if (it.id == state.activeSessionId) it.copy(sessionMolkkyWeight = weight) else it
            })
        }
    }

    fun requestExitConfirmation() {
        if (practiceSessionState.value.isDirty) {
            _internalUiState.update { it.copy(showExitConfirmDialog = true) }
        }
        // If not dirty, the UI should navigate away directly if requestExitConfirmation is part of a "Return" button logic
    }
    fun confirmSaveAndExit() { // This is called by UI when user confirms save on exit
        viewModelScope.launch {
            if (practiceSessionState.value.isDirty) {
                throwRepository.commitDrafts(_currentActiveUserIdFlow.value, _internalUiState.value.activeSessionId)
            }
            _internalUiState.update { it.copy(showExitConfirmDialog = false) }
            // Navigation is handled by the UI after this method completes
        }
    }
    fun cancelExit() { _internalUiState.update { it.copy(showExitConfirmDialog = false) } }
    fun onNameButtonClicked() { _internalUiState.update { it.copy(showUserDialog = true, userDialogErrorMessage = null) } }
    fun dismissUserDialog() { _internalUiState.update { it.copy(showUserDialog = false, userDialogErrorMessage = null) } }
    fun onEnvConfigButtonClicked() { _internalUiState.update { it.copy(showEnvConfigDialog = true) } }
    fun dismissEnvConfigDialog() { _internalUiState.update { it.copy(showEnvConfigDialog = false) } }
}