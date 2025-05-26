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
import kotlinx.coroutines.flow.distinctUntilChanged // ★ 追加
import kotlinx.coroutines.flow.flatMapLatest // ★ 追加
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map // ★ 追加 (distinctUntilChangedBy と併用する場合など)
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class PracticeViewModel(
    private val throwRepository: IThrowRepository,
    private val userRepository: IUserRepository,
    initialUserId: Int // ★ 初期ユーザーIDを引数名変更
) : ViewModel() {

    // currentActiveUserId を MutableStateFlow で管理
    private val _currentActiveUserIdFlow = MutableStateFlow(initialUserId) // ★

    // _currentActiveUserIdFlow の変更に応じて draftsFlow を動的に切り替える
    private val draftsFlow: StateFlow<List<ThrowDraft>> = _currentActiveUserIdFlow
        .flatMapLatest { userId -> // ★ flatMapLatest を使用
            android.util.Log.d("ViewModelFlows", "draftsFlow subscribing to new userId: $userId")
            throwRepository.getDrafts(userId)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _uiState = MutableStateFlow(
        PracticeUiState(
            currentUserId = initialUserId,
            activeDistance = 4.0f,
            configuredDistances = listOf(4.0f)
        )
    )

    val availableUsers: StateFlow<List<User>> = userRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val practiceSessionState: StateFlow<PracticeUiState> =
        combine(
            draftsFlow,
            _uiState,
            availableUsers,
            _currentActiveUserIdFlow // ★ アクティブなユーザーIDのFlowもcombineに含める
        ) { drafts, currentLocalState, users, activeUserId ->
            android.util.Log.d("ViewModelFlows", "Combine invoked. currentActiveUserId from Flow: $activeUserId, Drafts count: ${drafts.size}. currentLocalState.activeDistance: ${currentLocalState.activeDistance}")
            val currentUser = users.find { it.id == activeUserId } // ★ combineのactiveUserIdを使用
            val grouped = drafts.groupBy { it.distance }
                .mapValues { entry ->
                    entry.value.map { draft ->
                        SessionThrowDisplayData(draft.distance, draft.isSuccess, draft.angle)
                    }
                }
            val allDisplayDistances = currentLocalState.configuredDistances.distinct().sorted()

            currentLocalState.copy(
                currentUserId = activeUserId, // ★ combineのactiveUserIdを使用
                currentUserName = currentUser?.name ?: "Player $activeUserId",
                throwsGroupedByDistance = grouped,
                canUndo = drafts.isNotEmpty(),
                isDirty = drafts.isNotEmpty(),
                configuredDistances = allDisplayDistances,
                availableUsers = users
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _uiState.value.copy(currentUserId = initialUserId) // 初期値
        )

    init {
        viewModelScope.launch {
            // 初期ユーザー名を設定 (これは _currentActiveUserIdFlow を購読しても良い)
            val user = userRepository.getUserById(_currentActiveUserIdFlow.value)
            _uiState.update { it.copy(currentUserName = user?.name ?: "Player ${_currentActiveUserIdFlow.value}") }
        }
    }

    private fun performUserSwitch(userId: Int) {
        _currentActiveUserIdFlow.value = userId
        _uiState.update {
            it.copy(
                currentUserId = userId,
                // 修正: configuredDistances を PracticeUiState の初期値に合わせる
                configuredDistances = listOf(4.0f),
                // 修正: activeDistance を PracticeUiState の初期値に合わせる
                activeDistance = 4.0f,
                sessionWeather = null,
                sessionHumidity = null,
                sessionTemperature = null,
                sessionSoil = null,
                sessionMolkkyWeight = null,
                showUserSwitchConfirmDialog = false,
                pendingUserSwitchId = null
            )
        }
    }


    fun switchUser(userId: Int) { // ロジックはほぼ変更なし
        viewModelScope.launch {
            if (practiceSessionState.value.isDirty && practiceSessionState.value.currentUserId != userId) {
                _uiState.update { it.copy(showUserSwitchConfirmDialog = true, pendingUserSwitchId = userId) }
                return@launch
            }
            performUserSwitch(userId)
        }
    }

    fun confirmAndSwitchUser(saveCurrent: Boolean) { // ロジックはほぼ変更なし
        val pendingUserId = _uiState.value.pendingUserSwitchId
        if (pendingUserId == null) {
            _uiState.update { it.copy(showUserSwitchConfirmDialog = false) }
            return
        }
        val previousUserId = practiceSessionState.value.currentUserId

        viewModelScope.launch {
            if (saveCurrent && practiceSessionState.value.isDirty) {
                throwRepository.commitDrafts(previousUserId)
            } else if (!saveCurrent && practiceSessionState.value.isDirty) {
                throwRepository.clearAllDrafts(previousUserId)
            }
            // isDirtyなどはdraftsFlowの変更を通じてpracticeSessionStateが更新されるので、
            // ここでの強制リセットは不要になるかもしれないが、念のため残すか、
            // performUserSwitch後の状態を信頼する。
            _uiState.update { it.copy(showUserSwitchConfirmDialog = false) } // ダイアログを閉じる
            performUserSwitch(pendingUserId)
        }
    }
    fun cancelUserSwitch() { /* 変更なし */ _uiState.update { it.copy(showUserSwitchConfirmDialog = false, pendingUserSwitchId = null) } }


    fun addNewUser(userName: String) { // ほぼ変更なし、performUserSwitch を使う
        viewModelScope.launch {
            if (userName.isNotBlank()) {
                val existingUser = availableUsers.value.find { it.name.equals(userName, ignoreCase = true) }
                if (existingUser == null) {
                    val newUser = User(name = userName, created_at = System.currentTimeMillis())
                    val newUserId = userRepository.insertUser(newUser)
                    if (newUserId > 0) {
                        performUserSwitch(newUserId.toInt()) // ★ performUserSwitch を呼び出す
                        dismissUserDialog()
                    } else {
                        _uiState.update { it.copy(userDialogErrorMessage = "Failed to create user.") }
                    }
                } else {
                    _uiState.update { it.copy(userDialogErrorMessage = "User name already exists.") }
                }
            } else {
                _uiState.update { it.copy(userDialogErrorMessage = "User name cannot be empty.") }
            }
        }
    }

    fun addThrow(isSuccess: Boolean) {
        viewModelScope.launch {
            val currentUiState = practiceSessionState.value // combineされた最新の状態
            val currentDistance = currentUiState.activeDistance
            val activeUserIdForDraft = _currentActiveUserIdFlow.value // ★ 常に最新のユーザーIDを使用

            android.util.Log.d(
                "ViewModelAddThrow",
                "addThrow called. currentActiveUserId (from Flow): $activeUserIdForDraft, " +
                        "activeDistance from _uiState: ${_uiState.value.activeDistance}, " +
                        "activeDistance from practiceSessionState: $currentDistance"
            )

            if (currentDistance == null) {
                android.util.Log.w("ViewModelAddThrow", "addThrow returning early: currentDistance from practiceSessionState is null.")
                return@launch
            }

            val newDraft = ThrowDraft(
                userId = activeUserIdForDraft, // ★ _currentActiveUserIdFlow.value を使用
                distance = currentDistance,
                angle = currentUiState.selectedAngle,
                isSuccess = isSuccess,
                timestamp = Date().time,
                weather = currentUiState.sessionWeather,
                humidity = currentUiState.sessionHumidity,
                temperature = currentUiState.sessionTemperature,
                soil = currentUiState.sessionSoil,
                molkkyWeight = currentUiState.sessionMolkkyWeight
            )
            android.util.Log.d("ViewModelAddThrow", "Attempting to insert draft for userId $activeUserIdForDraft: $newDraft")
            try {
                throwRepository.insertDraft(newDraft)
                android.util.Log.d("ViewModelAddThrow", "Draft insertion call completed for userId $activeUserIdForDraft.")
            } catch (e: Exception) {
                android.util.Log.e("ViewModelAddThrow", "Error inserting draft for userId $activeUserIdForDraft", e)
            }
        }
    }
    // ... 他のメソッド (undo, save, confirmDiscardAndExitなども currentActiveUserId の代わりに _currentActiveUserIdFlow.value を参照するように変更)
    fun undo() {
        viewModelScope.launch {
            if (practiceSessionState.value.canUndo) {
                throwRepository.deleteLastDraft(_currentActiveUserIdFlow.value) // ★
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            if (practiceSessionState.value.isDirty) {
                throwRepository.commitDrafts(_currentActiveUserIdFlow.value) // ★
                _uiState.update { it.copy(showExitConfirmDialog = false) }
            }
        }
    }
    fun confirmDiscardAndExit() {
        viewModelScope.launch {
            throwRepository.clearAllDrafts(_currentActiveUserIdFlow.value) // ★
            _uiState.update { it.copy(showExitConfirmDialog = false) }
        }
    }
    // ... (selectDistance, selectAngle, etc. は _uiState を更新するので変更なし)
    fun selectDistance(distance: Float) { _uiState.update { it.copy(activeDistance = distance) } }
    fun selectAngle(angle: String) { _uiState.update { it.copy(selectedAngle = angle) } }
    fun requestAddDistance() { _uiState.update { it.copy(showAddDistanceDialog = true) } }
    fun cancelAddDistance() { _uiState.update { it.copy(showAddDistanceDialog = false) } }
    fun confirmAddDistance(newDistanceStr: String) {
        val newDistance = newDistanceStr.toFloatOrNull()
        if (newDistance != null && newDistance > 0) {
            _uiState.update { currentState ->
                val updatedDistances = (currentState.configuredDistances + newDistance).distinct().sorted()
                currentState.copy(
                    configuredDistances = updatedDistances,
                    activeDistance = newDistance,
                    showAddDistanceDialog = false
                )
            }
        } else {
            _uiState.update { it.copy(showAddDistanceDialog = false) }
        }
    }
    fun requestExitConfirmation() {
        if (practiceSessionState.value.isDirty) {
            _uiState.update { it.copy(showExitConfirmDialog = true) }
        }
    }
    fun confirmSaveAndExit() { save() }

    fun cancelExit() { _uiState.update { it.copy(showExitConfirmDialog = false) } }

    fun onNameButtonClicked() { _uiState.update { it.copy(showUserDialog = true, userDialogErrorMessage = null) } }
    fun dismissUserDialog() { _uiState.update { it.copy(showUserDialog = false, userDialogErrorMessage = null) } }

    fun onEnvConfigButtonClicked() { _uiState.update { it.copy(showEnvConfigDialog = true) } }
    fun dismissEnvConfigDialog() { _uiState.update { it.copy(showEnvConfigDialog = false) } }
    fun updateSessionWeather(weather: String?) { _uiState.update { it.copy(sessionWeather = weather) } }
    fun updateSessionHumidity(humidity: Float?) { _uiState.update { it.copy(sessionHumidity = humidity) } }
    fun updateSessionTemperature(temperature: Float?) { _uiState.update { it.copy(sessionTemperature = temperature) } }
    fun updateSessionSoil(soil: String?) { _uiState.update { it.copy(sessionSoil = soil) } }
    fun updateSessionMolkkyWeight(weight: Float?) { _uiState.update { it.copy(sessionMolkkyWeight = weight) } }

    fun requestDeleteDistance(distance: Float) {
        _uiState.update { it.copy(showDeleteDistanceConfirmDialog = true, distanceToDelete = distance) }
    }

    fun confirmDeleteDistance() {
        viewModelScope.launch {
            _uiState.value.distanceToDelete?.let { distance ->
                _uiState.update { currentState ->
                    val updatedDistances = currentState.configuredDistances.filter { it != distance }
                    currentState.copy(
                        configuredDistances = updatedDistances,
                        activeDistance = if (currentState.activeDistance == distance) null else currentState.activeDistance,
                        showDeleteDistanceConfirmDialog = false,
                        distanceToDelete = null
                    )
                }
                // Also clear any drafts associated with this distance if necessary,
                // though the spec doesn't explicitly state this for "configured distances"
                // but rather for "throws grouped by distance" which are drafts.
                // For now, we only remove it from the configured list.
                // If the intention is to delete ALL drafts at this distance, you'd add:
                // throwRepository.clearDraftsForDistance(currentActiveUserIdFlow.value, distance) // This method doesn't exist in ThrowDao/Repository currently
            }
        }
    }

    fun cancelDeleteDistance() {
        _uiState.update { it.copy(showDeleteDistanceConfirmDialog = false, distanceToDelete = null) }
    }

}