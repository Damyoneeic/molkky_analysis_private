package com.example.molkky_analysis.ui.practice

import android.content.Context
import android.content.SharedPreferences
import android.util.Log // Logクラスをインポート
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.User
import com.example.molkky_analysis.data.repository.IThrowRepository
import com.example.molkky_analysis.data.repository.IUserRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce // debounce をインポート
import kotlinx.coroutines.flow.firstOrNull // firstOrNull をインポート
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class PracticeViewModel(
    private val throwRepository: IThrowRepository,
    private val userRepository: IUserRepository,
    private val context: Context // For SharedPreferences
) : ViewModel() {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("PracticeSessionPrefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(PracticeUiState()) // Initial default state with isLoading = true
    val practiceSessionState: StateFlow<PracticeUiState> = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PracticeUiState() // Default state includes isLoading = true
    )

    // availableUsers はDBからのユーザーリストを保持
    val availableUsers: StateFlow<List<User>> = userRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        Log.d("ViewModelInit", "ViewModel init started")
        // _uiState は PracticeUiState() で初期化される際に isLoading = true になっている
        // なので、ここでの _uiState.update { it.copy(isLoading = true) } は不要な場合があるが、明示的に行う
        if (!_uiState.value.isLoading) { // 二重設定を防ぐ
            _uiState.update { it.copy(isLoading = true) }
        }
        Log.d("ViewModelInit", "isLoading state is: ${_uiState.value.isLoading}")

        viewModelScope.launch {
            Log.d("ViewModelInit", "Coroutine for usersDeferred and loadSessions started")
            val usersDeferred = async(Dispatchers.IO) {
                Log.d("ViewModelInit", "usersDeferred started")
                val users = userRepository.getAllUsers().firstOrNull() ?: emptyList()
                Log.d("ViewModelInit", "usersDeferred completed with ${users.size} users")
                users
            }
            loadSessionsFromPreferences(usersDeferred)
            Log.d("ViewModelInit", "loadSessionsFromPreferences call queued/started")
        }

        viewModelScope.launch {
            Log.d("ViewModelInit", "Coroutine for availableUsers.collect started")
            availableUsers.collect { users ->
                Log.d("ViewModelInit", "availableUsers.collect received ${users.size} users. Current isLoading: ${_uiState.value.isLoading}")

                _uiState.update { currentPracticeState ->
                    if (currentPracticeState.isLoading) {
                        Log.d("ViewModelInit", "availableUsers.collect - isLoading is true. Updating availableUsers if different.")
                        return@update if (currentPracticeState.availableUsers != users) {
                            currentPracticeState.copy(availableUsers = users)
                        } else {
                            currentPracticeState
                        }
                    }

                    var sessionsActuallyChanged = false
                    var newSessionsMap = currentPracticeState.sessions

                    currentPracticeState.sessions.forEach { (sessionId, sessionData) ->
                        val userEntityForSession = users.find { it.id == sessionData.currentUserId }
                        var sessionModifiedThisIteration = false
                        var tempSessionDataHolder = sessionData

                        if (userEntityForSession != null) {
                            if (sessionData.currentUserName != userEntityForSession.name) {
                                tempSessionDataHolder = tempSessionDataHolder.copy(currentUserName = userEntityForSession.name)
                                sessionModifiedThisIteration = true
                                Log.d("ViewModelInit", "Session $sessionId: UserName updated to ${userEntityForSession.name}")
                            }
                        } else if (sessionData.currentUserId != 0 && sessionData.currentUserId != DEFAULT_USER_ID) {
                            val defaultUserEntity = users.find { it.id == DEFAULT_USER_ID } ?: User(DEFAULT_USER_ID, DEFAULT_USER_NAME, System.currentTimeMillis())
                            if (sessionData.currentUserId != defaultUserEntity.id || sessionData.currentUserName != defaultUserEntity.name) {
                                tempSessionDataHolder = tempSessionDataHolder.copy(
                                    currentUserId = defaultUserEntity.id,
                                    currentUserName = defaultUserEntity.name
                                )
                                sessionModifiedThisIteration = true
                                Log.d("ViewModelInit", "Session $sessionId: User ID ${sessionData.currentUserId} not found, falling back to default user ${defaultUserEntity.name}")
                            }
                        }

                        if (sessionModifiedThisIteration) {
                            if (newSessionsMap === currentPracticeState.sessions) { // 最初の変更時にのみ新しいマップを作成
                                newSessionsMap = currentPracticeState.sessions.toMutableMap()
                            }
                            (newSessionsMap as MutableMap)[sessionId] = tempSessionDataHolder
                            sessionsActuallyChanged = true
                        }
                    }

                    val availableUsersListActuallyChanged = currentPracticeState.availableUsers != users

                    if (sessionsActuallyChanged || availableUsersListActuallyChanged) {
                        Log.d("ViewModelInit", "availableUsers.collect - Changes detected: sessionsActuallyChanged=$sessionsActuallyChanged, availableUsersListActuallyChanged=$availableUsersListActuallyChanged. Updating state.")
                        currentPracticeState.copy(
                            sessions = newSessionsMap,
                            availableUsers = if (availableUsersListActuallyChanged) users else currentPracticeState.availableUsers
                        )
                    } else {
                        Log.d("ViewModelInit", "availableUsers.collect - No effective changes. Not updating state.")
                        currentPracticeState
                    }
                }
            }
        }

        viewModelScope.launch {
            Log.d("ViewModelInit", "Coroutine for _uiState.collect (savePrefs) started")
            _uiState
                .debounce(1000L)
                .collect { state ->
                    if (!state.isLoading) {
                        Log.d("ViewModelInit", "_uiState.collect saving to preferences (debounced) for active tabs: ${state.activeSessionTabs}")
                        saveSessionsToPreferences(state)
                    } else {
                        Log.d("ViewModelInit", "_uiState.collect - isLoading is true, skipping save")
                    }
                }
        }
        Log.d("ViewModelInit", "ViewModel init bottom reached (coroutines launched)")
    }

    companion object {
        private const val MAX_SESSIONS = 5
        private const val DEFAULT_USER_ID = 1
        private const val DEFAULT_USER_NAME = "Player 1"
    }

    // --- Session Management ---
    fun selectSession(sessionId: Int) {
        _uiState.update {
            if (it.sessions.containsKey(sessionId) && it.currentActiveSessionId != sessionId) {
                Log.d("SessionManagement", "Selecting session $sessionId")
                it.copy(currentActiveSessionId = sessionId)
            } else {
                it
            }
        }
    }

    fun addSession() {
        _uiState.update { currentState ->
            if (currentState.sessions.size < MAX_SESSIONS) {
                val newSessionId = (0 until MAX_SESSIONS).firstOrNull { id -> !currentState.sessions.containsKey(id) }
                if (newSessionId != null) {
                    Log.d("SessionManagement", "Adding new session $newSessionId")
                    val defaultUser = currentState.availableUsers.find { it.id == DEFAULT_USER_ID }
                        ?: User(DEFAULT_USER_ID, DEFAULT_USER_NAME, System.currentTimeMillis()) // フォールバック
                    val newSession = createDefaultSessionState(
                        newSessionId,
                        defaultUser.id,
                        defaultUser.name
                    )
                    val updatedSessions = currentState.sessions + (newSessionId to newSession)
                    val updatedActiveTabs = (currentState.activeSessionTabs + newSessionId).distinct().sorted()
                    currentState.copy(
                        sessions = updatedSessions,
                        activeSessionTabs = updatedActiveTabs,
                        currentActiveSessionId = newSessionId
                    )
                } else {
                    Log.w("SessionManagement", "Could not find available new session ID.")
                    currentState
                }
            } else {
                Log.w("SessionManagement", "Max sessions reached, cannot add new session.")
                currentState
            }
        }
    }

    fun closeSession(sessionId: Int) {
        _uiState.update { currentState ->
            if (currentState.sessions.size > 1 && currentState.sessions.containsKey(sessionId)) {
                Log.d("SessionManagement", "Closing session $sessionId")
                val sessionToClose = currentState.sessions[sessionId]
                if (sessionToClose != null && sessionToClose.drafts.isNotEmpty()) {
                    Log.d("SessionManagement", "Clearing ${sessionToClose.drafts.size} drafts for closed session $sessionId from DB.")
                    viewModelScope.launch(Dispatchers.IO) {
                        throwRepository.clearSpecificDraftsFromTable(sessionToClose.drafts)
                    }
                }

                val updatedSessions = currentState.sessions - sessionId
                val updatedActiveTabs = currentState.activeSessionTabs - sessionId

                sharedPreferences.edit {
                    Log.d("SessionManagement", "Clearing SharedPreferences for session $sessionId")
                    val prefix = "session_${sessionId}_"
                    listOf("userId", "userName", "configuredDistances", "activeDistance", "selectedAngle",
                        "sessionWeather", "sessionHumidity", "sessionTemperature", "sessionSoil", "sessionMolkkyWeight", "draftIds")
                        .forEach { suffix -> remove("$prefix$suffix") }
                } // apply is handled by saveSessionsToPreferences if needed or implicitly by some SharedPreferences implementations

                currentState.copy(
                    sessions = updatedSessions,
                    activeSessionTabs = updatedActiveTabs,
                    currentActiveSessionId = if (currentState.currentActiveSessionId == sessionId) {
                        updatedActiveTabs.firstOrNull() ?: updatedSessions.keys.firstOrNull() ?: 0
                    } else {
                        currentState.currentActiveSessionId
                    }
                )
            } else {
                Log.w("SessionManagement", "Cannot close session $sessionId. Conditions not met (size>1, exists).")
                currentState
            }
        }
    }

    private fun updateCurrentSession(updateBlock: (SessionState) -> SessionState) {
        _uiState.update { currentState ->
            currentState.currentSessionState?.let { currentSession ->
                val updatedSession = updateBlock(currentSession)
                if (updatedSession != currentSession) { // 実際に変更があった場合のみ更新
                    currentState.copy(
                        sessions = currentState.sessions + (currentState.currentActiveSessionId to updatedSession)
                    )
                } else {
                    currentState // 変更なし
                }
            } ?: currentState
        }
    }

    fun addThrow(isSuccess: Boolean) {
        val currentSession = _uiState.value.currentSessionState ?: return
        val activeDistance = currentSession.activeDistance ?: return
        Log.d("ViewModelThrows", "addThrow called for session ${currentSession.sessionId}, user ${currentSession.currentUserId}, success: $isSuccess")

        val newDraft = ThrowDraft(
            userId = currentSession.currentUserId,
            distance = activeDistance,
            angle = currentSession.selectedAngle,
            weather = currentSession.sessionWeather,
            humidity = currentSession.sessionHumidity,
            temperature = currentSession.sessionTemperature,
            soil = currentSession.sessionSoil,
            molkkyWeight = currentSession.sessionMolkkyWeight,
            isSuccess = isSuccess,
            timestamp = Date().time
        )

        viewModelScope.launch(Dispatchers.IO) {
            Log.d("ViewModelThrows", "Inserting draft: $newDraft")
            throwRepository.insertDraft(newDraft) // newDraft.idがRoomによって更新されることを期待
            Log.d("ViewModelThrows", "Draft inserted with ID: ${newDraft.id}")

            updateCurrentSession { session ->
                val updatedDrafts = session.drafts + newDraft
                session.copy(
                    drafts = updatedDrafts,
                    throwsGroupedByDistance = groupDraftsForDisplay(updatedDrafts),
                    isDirty = true,
                    canUndo = true
                )
            }
        }
    }

    fun undo() {
        val currentSession = _uiState.value.currentSessionState ?: return
        if (!currentSession.canUndo || currentSession.drafts.isEmpty()) {
            Log.d("ViewModelThrows", "Undo called but not possible for session ${currentSession.sessionId}")
            return
        }
        val draftToUndo = currentSession.drafts.last()
        Log.d("ViewModelThrows", "Undo called for session ${currentSession.sessionId}, draft ID: ${draftToUndo.id}")

        viewModelScope.launch(Dispatchers.IO) {
            throwRepository.deleteDraftsByIds(listOf(draftToUndo.id))
            updateCurrentSession { session ->
                val updatedDrafts = session.drafts.dropLast(1)
                session.copy(
                    drafts = updatedDrafts,
                    throwsGroupedByDistance = groupDraftsForDisplay(updatedDrafts),
                    canUndo = updatedDrafts.isNotEmpty(),
                    isDirty = updatedDrafts.isNotEmpty() || session.isDirty // isDirtyは他の設定変更も考慮
                )
            }
        }
    }

    fun saveCurrentSession() {
        val currentSession = _uiState.value.currentSessionState ?: return
        if (!currentSession.isDirty || currentSession.drafts.isEmpty()) {
            Log.d("ViewModelSession", "SaveCurrentSession called for session ${currentSession.sessionId}, but no dirty drafts to save.")
            return
        }
        val draftsToSave = currentSession.drafts
        Log.d("ViewModelSession", "Saving ${draftsToSave.size} drafts for session ${currentSession.sessionId}")

        viewModelScope.launch(Dispatchers.IO) {
            throwRepository.commitSpecificDraftsToFinalAndDelete(draftsToSave)
            updateCurrentSession { session ->
                session.copy(
                    drafts = emptyList(),
                    throwsGroupedByDistance = emptyMap(),
                    isDirty = false, // Drafts saved, isDirty for drafts is false. Other config might still be "dirty".
                    canUndo = false
                )
            }
        }
    }

    fun discardDraftsForCurrentSession() {
        val currentSession = _uiState.value.currentSessionState ?: return
        if (currentSession.drafts.isEmpty()) {
            Log.d("ViewModelSession", "DiscardDrafts called for session ${currentSession.sessionId}, but no drafts to discard.")
            return
        }
        val draftsToClear = currentSession.drafts
        Log.d("ViewModelSession", "Discarding ${draftsToClear.size} drafts for session ${currentSession.sessionId}")

        viewModelScope.launch(Dispatchers.IO) {
            throwRepository.clearSpecificDraftsFromTable(draftsToClear)
            updateCurrentSession { session ->
                session.copy(
                    drafts = emptyList(),
                    throwsGroupedByDistance = emptyMap(),
                    isDirty = false,
                    canUndo = false
                )
            }
        }
    }

    fun selectAngle(angle: String) {
        updateCurrentSession {
            if (it.selectedAngle != angle) {
                Log.d("ViewModelInput", "Angle selected: $angle for session ${it.sessionId}")
                it.copy(selectedAngle = angle, isDirty = true)
            } else it
        }
    }

    fun selectDistance(distance: Float) {
        updateCurrentSession {
            if (it.activeDistance != distance) {
                Log.d("ViewModelInput", "Distance selected: $distance for session ${it.sessionId}")
                it.copy(activeDistance = distance, isDirty = true)
            } else it
        }
    }

    fun confirmAddDistance(newDistanceStr: String) {
        val newDistance = newDistanceStr.toFloatOrNull()
        if (newDistance != null && newDistance > 0) {
            updateCurrentSession { session ->
                if (!session.configuredDistances.contains(newDistance)) {
                    Log.d("ViewModelInput", "Adding distance: $newDistance to session ${session.sessionId}")
                    val updatedDistances = (session.configuredDistances + newDistance).distinct().sorted()
                    session.copy(
                        configuredDistances = updatedDistances,
                        activeDistance = newDistance, // Optionally switch to new distance
                        isDirty = true
                    )
                } else session
            }
        }
        _uiState.update { it.copy(showAddDistanceDialog = false) }
    }

    fun requestDeleteDistance(distance: Float) {
        _uiState.update { it.copy(showDeleteDistanceConfirmDialog = true, distanceToDelete = distance) }
    }

    fun confirmDeleteDistance() {
        val distanceToDelete = _uiState.value.distanceToDelete ?: return
        Log.d("ViewModelInput", "Confirming delete distance: $distanceToDelete for current session")
        updateCurrentSession { session ->
            if (!session.configuredDistances.contains(distanceToDelete)) return@updateCurrentSession session

            val updatedDistances = session.configuredDistances.filter { it != distanceToDelete }
            val remainingDrafts = session.drafts.filter { it.distance != distanceToDelete }
            val draftsActuallyDeleted = session.drafts.filter { it.distance == distanceToDelete }

            if (draftsActuallyDeleted.isNotEmpty()) {
                Log.d("ViewModelInput", "Deleting ${draftsActuallyDeleted.size} drafts for distance $distanceToDelete from DB.")
                viewModelScope.launch(Dispatchers.IO) {
                    throwRepository.deleteDraftsByIds(draftsActuallyDeleted.map { it.id })
                }
            }
            session.copy(
                configuredDistances = updatedDistances,
                activeDistance = if (session.activeDistance == distanceToDelete) updatedDistances.firstOrNull() else session.activeDistance,
                drafts = remainingDrafts,
                throwsGroupedByDistance = groupDraftsForDisplay(remainingDrafts),
                isDirty = true,
                canUndo = remainingDrafts.isNotEmpty()
            )
        }
        _uiState.update { it.copy(showDeleteDistanceConfirmDialog = false, distanceToDelete = null) }
    }
    fun cancelDeleteDistance() = _uiState.update { it.copy(showDeleteDistanceConfirmDialog = false, distanceToDelete = null) }

    fun switchUserForCurrentSession(targetUserId: Int) {
        val currentSession = _uiState.value.currentSessionState ?: return
        if (currentSession.currentUserId == targetUserId) return // Already the selected user

        Log.d("ViewModelUser", "Attempting to switch user for session ${currentSession.sessionId} to user ID $targetUserId")
        if (currentSession.isDirty && currentSession.drafts.isNotEmpty()) {
            Log.d("ViewModelUser", "Session ${currentSession.sessionId} is dirty, showing switch confirmation dialog.")
            _uiState.update {
                it.copy(
                    showUserSwitchConfirmDialog = true,
                    pendingUserSwitchTargetId = targetUserId,
                    pendingUserSwitchSessionId = currentSession.sessionId
                )
            }
            return
        }
        performUserSwitchForSession(currentSession.sessionId, targetUserId, currentSession.drafts.isNotEmpty())
    }

    private fun performUserSwitchForSession(sessionId: Int, targetUserId: Int, clearPreviousUserDrafts: Boolean) {
        Log.d("ViewModelUser", "Performing user switch for session $sessionId to user ID $targetUserId. Clear drafts: $clearPreviousUserDrafts")
        viewModelScope.launch(Dispatchers.IO) {
            val sessionToUpdate = _uiState.value.sessions[sessionId] // Get the session state *before* DB calls
            if (clearPreviousUserDrafts && sessionToUpdate != null && sessionToUpdate.drafts.isNotEmpty()) {
                Log.d("ViewModelUser", "Clearing ${sessionToUpdate.drafts.size} drafts for previous user in session $sessionId.")
                throwRepository.clearSpecificDraftsFromTable(sessionToUpdate.drafts)
            }

            val targetUser = userRepository.getUserById(targetUserId) // Fetch from DB to ensure name is current

            if (targetUser != null) {
                _uiState.update { currentState ->
                    currentState.sessions[sessionId]?.let { oldSession ->
                        val updatedSession = oldSession.copy(
                            currentUserId = targetUserId,
                            currentUserName = targetUser.name,
                            drafts = if (clearPreviousUserDrafts) emptyList() else oldSession.drafts, // Drafts should be cleared if user changes
                            throwsGroupedByDistance = if (clearPreviousUserDrafts) emptyMap() else groupDraftsForDisplay(oldSession.drafts),
                            isDirty = if (clearPreviousUserDrafts) false else oldSession.isDirty, // Reset dirty for drafts
                            canUndo = if (clearPreviousUserDrafts) false else oldSession.canUndo
                        )
                        Log.d("ViewModelUser", "Session $sessionId updated to user ${targetUser.name}")
                        currentState.copy(
                            sessions = currentState.sessions + (sessionId to updatedSession),
                            showUserSwitchConfirmDialog = false,
                            pendingUserSwitchTargetId = null,
                            pendingUserSwitchSessionId = null
                        )
                    } ?: currentState.also { Log.e("ViewModelUser", "Session $sessionId not found during performUserSwitch update.") }
                }
            } else {
                Log.e("ViewModelUser", "Target user with ID $targetUserId not found in DB.")
                // Optionally update UI with an error message if targetUser is null
            }
        }
    }

    fun confirmAndSwitchUser(saveCurrent: Boolean) {
        val targetUserId = _uiState.value.pendingUserSwitchTargetId ?: return
        val sessionIdForSwitch = _uiState.value.pendingUserSwitchSessionId ?: return
        val sessionForSwitch = _uiState.value.sessions[sessionIdForSwitch] ?: return
        Log.d("ViewModelUser", "Confirming user switch for session $sessionIdForSwitch to user ID $targetUserId. Save current: $saveCurrent")

        viewModelScope.launch(Dispatchers.IO) {
            if (saveCurrent && sessionForSwitch.isDirty && sessionForSwitch.drafts.isNotEmpty()) {
                Log.d("ViewModelUser", "Saving ${sessionForSwitch.drafts.size} drafts before switching user.")
                throwRepository.commitSpecificDraftsToFinalAndDelete(sessionForSwitch.drafts)
            } else if (!saveCurrent && sessionForSwitch.isDirty && sessionForSwitch.drafts.isNotEmpty()) {
                Log.d("ViewModelUser", "Discarding ${sessionForSwitch.drafts.size} drafts before switching user.")
                throwRepository.clearSpecificDraftsFromTable(sessionForSwitch.drafts)
            }
            performUserSwitchForSession(sessionIdForSwitch, targetUserId, true) // Always clear for the new user context in the session
        }
    }
    fun cancelUserSwitch() {
        Log.d("ViewModelUser", "User switch cancelled.")
        _uiState.update { it.copy(showUserSwitchConfirmDialog = false, pendingUserSwitchTargetId = null, pendingUserSwitchSessionId = null) }
    }

    fun addNewUserAndSwitch(userName: String) {
        Log.d("ViewModelUser", "Attempting to add new user: $userName and switch current session.")
        viewModelScope.launch(Dispatchers.IO) {
            if (userName.isNotBlank()) {
                // Check against latest user list from DB to prevent race conditions
                val allUsers = userRepository.getAllUsers().firstOrNull() ?: emptyList()
                val existingUser = allUsers.find { it.name.equals(userName, ignoreCase = true) }

                if (existingUser == null) {
                    val newUserId = userRepository.insertUser(User(name = userName, created_at = System.currentTimeMillis()))
                    if (newUserId > 0) {
                        Log.d("ViewModelUser", "New user $userName (ID: $newUserId) created.")
                        val currentSessionId = _uiState.value.currentActiveSessionId
                        performUserSwitchForSession(currentSessionId, newUserId.toInt(), true)
                        // Dialog dismissal should ideally happen on Main thread after state update is observed
                        viewModelScope.launch(Dispatchers.Main) {
                            _uiState.update { it.copy(showUserDialog = false, userDialogErrorMessage = null) }
                        }
                    } else {
                        Log.e("ViewModelUser", "Failed to insert new user $userName into DB.")
                        _uiState.update { it.copy(userDialogErrorMessage = "Failed to create user.") }
                    }
                } else {
                    Log.w("ViewModelUser", "User name $userName already exists.")
                    _uiState.update { it.copy(userDialogErrorMessage = "User name already exists.") }
                }
            } else {
                _uiState.update { it.copy(userDialogErrorMessage = "User name cannot be empty.") }
            }
        }
    }

    fun requestDeleteUser(user: User) {
        Log.d("ViewModelUser", "Requesting delete for user: ${user.name} (ID: ${user.id})")
        if (availableUsers.value.size <= 1) {
            _uiState.update { it.copy(userDialogErrorMessage = "Cannot delete the last user.") }
            return
        }
        val isActiveInAnySession = _uiState.value.sessions.values.any { it.currentUserId == user.id }
        if (isActiveInAnySession) {
            _uiState.update { it.copy(userDialogErrorMessage = "User is active in a session. Switch user in those sessions first.") }
            return
        }
        if (user.id == DEFAULT_USER_ID && user.name == DEFAULT_USER_NAME) {
            _uiState.update { it.copy(userDialogErrorMessage = "Cannot delete the default '${DEFAULT_USER_NAME}'.") }
            return
        }
        _uiState.update { it.copy(showDeleteUserConfirmDialog = true, userToDelete = user) }
    }

    fun confirmDeleteUser() {
        val userToDelete = _uiState.value.userToDelete ?: return
        Log.d("ViewModelUser", "Confirming delete for user: ${userToDelete.name} (ID: ${userToDelete.id})")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                throwRepository.clearAllDraftsForUser(userToDelete.id) // Clear drafts first if FK doesn't cascade drafts
                // User deletion will cascade to ThrowRecord via FK
                userRepository.deleteUser(userToDelete)
                Log.d("ViewModelUser", "User ${userToDelete.name} deleted from DB.")
                _uiState.update { it.copy(showDeleteUserConfirmDialog = false, userToDelete = null, userDialogErrorMessage = null) }
            } catch(e: Exception) {
                Log.e("ViewModelUser", "Error deleting user ${userToDelete.name}", e)
                _uiState.update { it.copy(userDialogErrorMessage = "Error deleting user: ${e.localizedMessage}") }
            }
        }
    }
    fun cancelDeleteUser() {
        Log.d("ViewModelUser", "User deletion cancelled.")
        _uiState.update { it.copy(showDeleteUserConfirmDialog = false, userToDelete = null) }
    }


    fun updateSessionWeather(weather: String?) = updateCurrentSession {
        if (it.sessionWeather != weather) it.copy(sessionWeather = weather, isDirty = true) else it
    }
    fun updateSessionHumidity(humidity: Float?) = updateCurrentSession {
        if (it.sessionHumidity != humidity) it.copy(sessionHumidity = humidity, isDirty = true) else it
    }
    fun updateSessionTemperature(temperature: Float?) = updateCurrentSession {
        if (it.sessionTemperature != temperature) it.copy(sessionTemperature = temperature, isDirty = true) else it
    }
    fun updateSessionSoil(soil: String?) = updateCurrentSession {
        if (it.sessionSoil != soil) it.copy(sessionSoil = soil, isDirty = true) else it
    }
    fun updateSessionMolkkyWeight(weight: Float?) = updateCurrentSession { // Added this method
        if (it.sessionMolkkyWeight != weight) it.copy(sessionMolkkyWeight = weight, isDirty = true) else it
    }


    fun resetEnvironmentConfiguration() {
        Log.d("ViewModelEnv", "Resetting environment configuration for current session.")
        updateCurrentSession {
            it.copy(
                sessionWeather = null, sessionHumidity = null, sessionTemperature = null,
                sessionSoil = null, sessionMolkkyWeight = null, isDirty = true
            )
        }
    }

    fun requestExitConfirmation() {
        if (_uiState.value.sessions.values.any { it.isDirty && it.drafts.isNotEmpty() }) {
            Log.d("ViewModelNavigation", "Requesting exit confirmation due to dirty drafts.")
            _uiState.update { it.copy(showExitConfirmDialog = true) }
        } else {
            Log.d("ViewModelNavigation", "No dirty drafts, exit confirmation not needed (or handle config-only dirty state).")
            // If no dirty drafts, and we want to allow exiting without prompt for config-only changes:
            // This depends on the desired behavior. For now, only draft-dirty prompts.
        }
    }

    fun confirmSaveAndExit() {
        Log.d("ViewModelNavigation", "Confirm save and exit.")
        viewModelScope.launch(Dispatchers.IO) {
            val sessionsToSave = _uiState.value.sessions.values.filter { it.isDirty && it.drafts.isNotEmpty() }
            Log.d("ViewModelNavigation", "Found ${sessionsToSave.size} sessions with dirty drafts to save.")
            sessionsToSave.forEach { session ->
                Log.d("ViewModelNavigation", "Saving drafts for session ${session.sessionId}")
                throwRepository.commitSpecificDraftsToFinalAndDelete(session.drafts)
            }
            _uiState.update { currentState ->
                val newSessions = currentState.sessions.mapValues { (_, session) ->
                    if (sessionsToSave.any { it.sessionId == session.sessionId}) {
                        session.copy(drafts = emptyList(), throwsGroupedByDistance = emptyMap(), isDirty = false, canUndo = false)
                    } else {
                        session
                    }
                }
                currentState.copy(sessions = newSessions, showExitConfirmDialog = false)
            }
            // Note: onReturnToHome() should be called by the UI after this completes or state reflects exit readiness
        }
    }

    fun confirmDiscardAndExit() {
        Log.d("ViewModelNavigation", "Confirm discard and exit.")
        viewModelScope.launch(Dispatchers.IO) {
            val sessionsToDiscard = _uiState.value.sessions.values.filter { it.drafts.isNotEmpty() }
            Log.d("ViewModelNavigation", "Found ${sessionsToDiscard.size} sessions with drafts to discard.")
            sessionsToDiscard.forEach { session ->
                Log.d("ViewModelNavigation", "Discarding drafts for session ${session.sessionId}")
                throwRepository.clearSpecificDraftsFromTable(session.drafts)
            }
            _uiState.update { currentState ->
                val newSessions = currentState.sessions.mapValues { (_, session) ->
                    session.copy(drafts = emptyList(), throwsGroupedByDistance = emptyMap(), isDirty = false, canUndo = false)
                }
                currentState.copy(sessions = newSessions, showExitConfirmDialog = false)
            }
            // Note: onReturnToHome() should be called by the UI
        }
    }
    fun cancelExit() {
        Log.d("ViewModelNavigation", "Exit cancelled.")
        _uiState.update { it.copy(showExitConfirmDialog = false) }
    }

    fun requestAddDistanceDialog() = _uiState.update { it.copy(showAddDistanceDialog = true) }
    fun cancelAddDistanceDialog() = _uiState.update { it.copy(showAddDistanceDialog = false) }
    fun onNameButtonClicked() = _uiState.update { it.copy(showUserDialog = true, userDialogErrorMessage = null) }
    fun dismissUserDialog() = _uiState.update { it.copy(showUserDialog = false, userDialogErrorMessage = null, showDeleteUserConfirmDialog = false, userToDelete = null) }
    fun onEnvConfigButtonClicked() = _uiState.update { it.copy(showEnvConfigDialog = true) }
    fun dismissEnvConfigDialog() = _uiState.update { it.copy(showEnvConfigDialog = false) }


    private fun loadSessionsFromPreferences(usersDeferred: Deferred<List<User>>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ViewModelLoad", "loadSessionsFromPreferences started")
                val users = usersDeferred.await()
                Log.d("ViewModelLoad", "usersDeferred awaited, ${users.size} users available for session setup.")

                val activeTabsString = sharedPreferences.getString("activeSessionTabs", "0") ?: "0"
                val activeSessionTabsLoaded = activeTabsString.split(',')
                    .mapNotNull { it.toIntOrNull() }
                    .ifEmpty { listOf(0) } // Default to session 0 if empty
                    .distinct()
                    .take(MAX_SESSIONS)
                var currentActiveSessionIdLoaded = sharedPreferences.getInt("currentActiveSessionId", activeSessionTabsLoaded.firstOrNull() ?: 0)

                val loadedSessionsMap = mutableMapOf<Int, SessionState>()
                val draftLoadingTasks = mutableListOf<Deferred<Pair<Int, List<ThrowDraft>>>>()

                Log.d("ViewModelLoad", "Attempting to load sessions for tabs: $activeSessionTabsLoaded")

                for (sessionId in activeSessionTabsLoaded) {
                    Log.d("ViewModelLoad", "Processing session ID from prefs: $sessionId")
                    val defaultUserForSession = users.find { it.id == DEFAULT_USER_ID }
                        ?: User(DEFAULT_USER_ID, DEFAULT_USER_NAME, System.currentTimeMillis()).also {
                            Log.w("ViewModelLoad", "Default user ID $DEFAULT_USER_ID not found in DB, using fallback temporary User object.")
                        }

                    val userIdFromPrefs = sharedPreferences.getInt("session_${sessionId}_userId", defaultUserForSession.id)
                    val userForSession = users.find { it.id == userIdFromPrefs }
                        ?: defaultUserForSession.also {
                            Log.w("ViewModelLoad", "User ID $userIdFromPrefs for session $sessionId not found in DB, falling back to default user ${defaultUserForSession.name}.")
                        }
                    val userName = userForSession.name

                    val configuredDistsString = sharedPreferences.getString("session_${sessionId}_configuredDistances", "4.0") ?: "4.0"
                    val configuredDists = configuredDistsString.split(',')
                        .mapNotNull { it.toFloatOrNull() }
                        .ifEmpty { listOf(4.0f) }

                    val activeDistString = sharedPreferences.getString("session_${sessionId}_activeDistance", null) // Read as String first
                    val activeDist = activeDistString?.toFloatOrNull() ?: configuredDists.firstOrNull() ?: 4.0f


                    val angle = sharedPreferences.getString("session_${sessionId}_selectedAngle", "CENTER") ?: "CENTER"
                    val weather = sharedPreferences.getString("session_${sessionId}_sessionWeather", null)
                    val humidity = sharedPreferences.getString("session_${sessionId}_sessionHumidity", null)?.toFloatOrNull()
                    val temp = sharedPreferences.getString("session_${sessionId}_sessionTemperature", null)?.toFloatOrNull()
                    val soil = sharedPreferences.getString("session_${sessionId}_sessionSoil", null)
                    val molkkyWeight = sharedPreferences.getString("session_${sessionId}_sessionMolkkyWeight", null)?.toFloatOrNull()

                    val draftIdsString = sharedPreferences.getString("session_${sessionId}_draftIds", "") ?: ""
                    val draftIds = draftIdsString.split(',')
                        .mapNotNull { it.toIntOrNull() }
                        .filter { it > 0 }

                    if (draftIds.isNotEmpty()) {
                        Log.d("ViewModelLoad", "Session $sessionId: Queuing draft loading for IDs: $draftIds")
                        draftLoadingTasks.add(async(Dispatchers.IO) {
                            sessionId to throwRepository.getDraftsByIds(draftIds)
                        })
                    } else {
                        Log.d("ViewModelLoad", "Session $sessionId: No draft IDs found in prefs.")
                    }

                    loadedSessionsMap[sessionId] = SessionState(
                        sessionId, userIdFromPrefs, userName, emptyMap(), configuredDists, activeDist, angle,
                        weather, humidity, temp, soil, molkkyWeight,
                        false, false, emptyList() // Drafts and related flags will be updated after awaitAll
                    )
                }
                Log.d("ViewModelLoad", "Initial loadedSessionsMap (before drafts are populated): Keys ${loadedSessionsMap.keys}")

                if (loadedSessionsMap.isEmpty()) {
                    Log.w("ViewModelLoad", "No sessions were loaded from SharedPreferences or active tabs list was empty. Creating a default session 0.")
                    val defaultUser = users.find { it.id == DEFAULT_USER_ID }
                        ?: User(DEFAULT_USER_ID, DEFAULT_USER_NAME, System.currentTimeMillis())
                    loadedSessionsMap[0] = createDefaultSessionState(0, defaultUser.id, defaultUser.name)
                    currentActiveSessionIdLoaded = 0
                    Log.d("ViewModelLoad", "Default session 0 created. Active Session ID set to 0.")
                }

                if (!loadedSessionsMap.containsKey(currentActiveSessionIdLoaded)) {
                    Log.w("ViewModelLoad", "Saved currentActiveSessionId ($currentActiveSessionIdLoaded) is not in loaded sessions. Resetting to first available: ${loadedSessionsMap.keys.firstOrNull() ?: 0}")
                    currentActiveSessionIdLoaded = loadedSessionsMap.keys.firstOrNull() ?: 0 // Fallback if active ID is somehow invalid
                }

                val loadedDraftsResults = draftLoadingTasks.awaitAll()
                Log.d("ViewModelLoad", "All draft loading tasks awaited. Results count: ${loadedDraftsResults.size}")
                loadedDraftsResults.forEach { (sId, sDrafts) ->
                    loadedSessionsMap[sId]?.let { session ->
                        Log.d("ViewModelLoad", "Session $sId: Loaded ${sDrafts.size} drafts from DB: $sDrafts")
                        loadedSessionsMap[sId] = session.copy(
                            drafts = sDrafts,
                            throwsGroupedByDistance = groupDraftsForDisplay(sDrafts),
                            isDirty = sDrafts.isNotEmpty() || session.isDirty, // Preserve existing dirty if config was dirty
                            canUndo = sDrafts.isNotEmpty()
                        )
                    }
                }
                Log.d("ViewModelLoad", "Final loadedSessionsMap (after drafts are populated): Keys ${loadedSessionsMap.keys}")

                _uiState.update {
                    Log.d("ViewModelLoad", "Updating _uiState with loaded sessions. isLoading will be set to false.")
                    it.copy(
                        sessions = loadedSessionsMap,
                        activeSessionTabs = loadedSessionsMap.keys.toList().sorted(),
                        currentActiveSessionId = currentActiveSessionIdLoaded,
                        isLoading = false, // Crucial: set isLoading to false *after* all data is ready
                        availableUsers = users // Ensure availableUsers is also up-to-date in the state
                    )
                }
                Log.d("ViewModelLoad", "loadSessionsFromPreferences finished successfully. isLoading is now false.")
            } catch (e: Exception) {
                Log.e("ViewModelLoad", "CRITICAL ERROR in loadSessionsFromPreferences", e)
                _uiState.update { it.copy(isLoading = false) } // Ensure loading is stopped even on error
            }
        }
    }

    private fun saveSessionsToPreferences(state: PracticeUiState) {
        // This function should not run if isLoading is true, handled by the caller (.debounce().collect{ if !isLoading })
        try {
            Log.d("ViewModelSave", "saveSessionsToPreferences started for active tabs: ${state.activeSessionTabs}, currentActive: ${state.currentActiveSessionId}")
            sharedPreferences.edit {
                putString("activeSessionTabs", state.activeSessionTabs.joinToString(","))
                putInt("currentActiveSessionId", state.currentActiveSessionId)

                val allPrefKeys = sharedPreferences.all.keys
                val sessionKeysToRemove = allPrefKeys.filter { it.startsWith("session_") }.toMutableSet()

                state.activeSessionTabs.forEach { sessionId ->
                    state.sessions[sessionId]?.let { sessionState ->
                        val prefix = "session_${sessionId}_"
                        Log.d("ViewModelSave", "Saving session $sessionId: User=${sessionState.currentUserName}, Drafts=${sessionState.drafts.size}")

                        putInt("${prefix}userId", sessionState.currentUserId)
                        putString("${prefix}userName", sessionState.currentUserName)
                        putString("${prefix}configuredDistances", sessionState.configuredDistances.joinToString(","))
                        // Store floats as strings to avoid precision issues with getFloat default value
                        sessionState.activeDistance?.let { ad -> putString("${prefix}activeDistance", ad.toString()) } ?: remove("${prefix}activeDistance")
                        putString("${prefix}selectedAngle", sessionState.selectedAngle)

                        sessionState.sessionWeather?.let { putString("${prefix}sessionWeather", it) } ?: remove("${prefix}sessionWeather")
                        sessionState.sessionHumidity?.let { putString("${prefix}sessionHumidity", it.toString()) } ?: remove("${prefix}sessionHumidity")
                        sessionState.sessionTemperature?.let { putString("${prefix}sessionTemperature", it.toString()) } ?: remove("${prefix}sessionTemperature")
                        sessionState.sessionSoil?.let { putString("${prefix}sessionSoil", it) } ?: remove("${prefix}sessionSoil")
                        sessionState.sessionMolkkyWeight?.let { putString("${prefix}sessionMolkkyWeight", it.toString()) } ?: remove("${prefix}sessionMolkkyWeight")

                        putString("${prefix}draftIds", sessionState.drafts.joinToString(",") { it.id.toString() })

                        sessionKeysToRemove.removeAll { it.startsWith(prefix) }
                    }
                }
                sessionKeysToRemove.forEach { keyToRemove ->
                    Log.d("ViewModelSave", "Removing orphaned SharedPreferences key: $keyToRemove")
                    remove(keyToRemove)
                }
                apply()
            }
            Log.d("ViewModelSave", "saveSessionsToPreferences finished successfully.")
        } catch (e: Exception) {
            Log.e("ViewModelSave", "Error in saveSessionsToPreferences", e)
        }
    }

    private fun createDefaultSessionState(sessionId: Int, defaultUserId: Int, defaultUserName: String): SessionState {
        Log.d("ViewModelDefault", "Creating default session state for ID $sessionId, User: $defaultUserName ($defaultUserId)")
        return SessionState(
            sessionId = sessionId,
            currentUserId = defaultUserId,
            currentUserName = defaultUserName,
            configuredDistances = listOf(4.0f),
            activeDistance = 4.0f,
            selectedAngle = "CENTER"
        )
    }

    private fun groupDraftsForDisplay(drafts: List<ThrowDraft>): Map<Float, List<SessionThrowDisplayData>> {
        return drafts.groupBy { it.distance }
            .mapValues { entry ->
                entry.value.map { draft ->
                    SessionThrowDisplayData(draft.distance, draft.isSuccess, draft.angle)
                }
            }
    }
}