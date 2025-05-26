package com.example.molkky_analysis.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.User // Userモデルをインポート
import com.example.molkky_analysis.data.repository.IThrowRepository
import com.example.molkky_analysis.data.repository.IUserRepository // IUserRepositoryをインポート
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class PracticeViewModel(
    private val throwRepository: IThrowRepository,
    private val userRepository: IUserRepository, // ★ UserRepository を注入
    private var currentActiveUserId: Int // ★ ViewModel初期化時のユーザーID、変更可能にする
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState(currentUserId = currentActiveUserId))

    // 現在のユーザーのドラフトのみを監視するように変更
    private var draftsFlow: StateFlow<List<ThrowDraft>> =
        throwRepository.getDrafts(currentActiveUserId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val availableUsers: StateFlow<List<User>> = userRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val practiceSessionState: StateFlow<PracticeUiState> =
        combine(
            draftsFlow,
            _uiState,
            availableUsers // 利用可能なユーザーリストもcombineに含める
        ) { drafts, currentLocalState, users ->
            val currentUser = users.find { it.id == currentLocalState.currentUserId }
            val grouped = drafts.groupBy { it.distance }
                .mapValues { entry ->
                    entry.value.map { draft ->
                        SessionThrowDisplayData(draft.distance, draft.isSuccess, draft.angle)
                    }
                }
            val allDisplayDistances = currentLocalState.configuredDistances.distinct().sorted()

            currentLocalState.copy(
                currentUserName = currentUser?.name ?: "Loading...",
                throwsGroupedByDistance = grouped,
                canUndo = drafts.isNotEmpty(),
                isDirty = drafts.isNotEmpty(),
                configuredDistances = allDisplayDistances,
                availableUsers = users // UIStateにもユーザーリストを渡す
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _uiState.value.copy(currentUserId = currentActiveUserId) // 初期値
        )

    init {
        // ViewModel初期化時に現在のユーザー名を設定
        viewModelScope.launch {
            val user = userRepository.getUserById(currentActiveUserId)
            _uiState.update { it.copy(currentUserName = user?.name ?: "Player $currentActiveUserId") }
        }
    }


    private fun refreshDraftsFlow() {
        draftsFlow = throwRepository.getDrafts(currentActiveUserId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        // combine フローが再評価されるように、_uiStateも更新トリガーをかける
        _uiState.update { it.copy(currentUserId = currentActiveUserId) }
    }

    fun switchUser(userId: Int) {
        viewModelScope.launch {
            // ユーザー切り替え前に現在のセッションのダーティチェック
            if (practiceSessionState.value.isDirty) {
                // TODO: 未保存データがある場合の処理をユーザーに確認する (ダイアログなど)
                // ここでは一旦、ダーティなら切り替え前に保存を促すメッセージを出すか、
                // ViewModelが自動保存するか、破棄するかなどのポリシーを決める必要がある
                // 今回はまず、ダーティな場合はrequestExitConfirmationを呼んで既存のダイアログフローを使う
                _uiState.update { it.copy(showUserSwitchConfirmDialog = true, pendingUserSwitchId = userId) }
                return@launch // ダイアログの応答を待つ
            }
            performUserSwitch(userId)
        }
    }

    private fun performUserSwitch(userId: Int) {
        currentActiveUserId = userId
        _uiState.update {
            it.copy(
                currentUserId = userId,
                // ユーザー切り替え時に練習セッションの状態をリセットする (距離リストなど)
                configuredDistances = listOf(3.0f, 3.5f, 4.0f), // デフォルトに戻す例
                activeDistance = 3.0f,
                sessionWeather = null, // 環境設定もリセット
                sessionHumidity = null,
                sessionTemperature = null,
                sessionSoil = null,
                sessionMolkkyWeight = null,
                showUserSwitchConfirmDialog = false,
                pendingUserSwitchId = null
            )
        }
        refreshDraftsFlow() // 新しいユーザーのドラフトを読み込む
        viewModelScope.launch {
            val user = userRepository.getUserById(userId)
            _uiState.update { it.copy(currentUserName = user?.name ?: "Player $userId") }
        }
    }

    fun confirmAndSwitchUser(saveCurrent: Boolean) {
        val pendingUserId = _uiState.value.pendingUserSwitchId
        if (pendingUserId == null) {
            _uiState.update { it.copy(showUserSwitchConfirmDialog = false) }
            return
        }

        viewModelScope.launch {
            if (saveCurrent && practiceSessionState.value.isDirty) {
                throwRepository.commitDrafts(practiceSessionState.value.currentUserId) // 前のユーザーのドラフトを保存
            } else if (!saveCurrent && practiceSessionState.value.isDirty) {
                throwRepository.clearAllDrafts(practiceSessionState.value.currentUserId) // 前のユーザーのドラフトを破棄
            }
            // 状態をリセットしてからユーザーを切り替える
            _uiState.update { it.copy(isDirty = false, canUndo = false, showUserSwitchConfirmDialog = false) } // isDirtyなどを強制リセット
            performUserSwitch(pendingUserId)
        }
    }

    fun cancelUserSwitch() {
        _uiState.update { it.copy(showUserSwitchConfirmDialog = false, pendingUserSwitchId = null) }
    }


    fun addNewUser(userName: String) {
        viewModelScope.launch {
            if (userName.isNotBlank()) {
                // 同じ名前のユーザーがいないか確認 (オプション)
                val existingUser = availableUsers.value.find { it.name.equals(userName, ignoreCase = true) }
                if (existingUser == null) {
                    val newUser = User(name = userName, created_at = System.currentTimeMillis())
                    val newUserId = userRepository.insertUser(newUser)
                    if (newUserId > 0) { // 挿入成功
                        // 新規ユーザーに即座に切り替える
                        performUserSwitch(newUserId.toInt())
                        dismissUserDialog() // ダイアログを閉じる
                    } else {
                        // 挿入失敗時のエラーハンドリング
                        _uiState.update { it.copy(userDialogErrorMessage = "Failed to create user.") }
                    }
                } else {
                    // ユーザー名重複時のエラーハンドリング
                    _uiState.update { it.copy(userDialogErrorMessage = "User name already exists.") }
                }
            } else {
                // ユーザー名が空の場合のエラーハンドリング
                _uiState.update { it.copy(userDialogErrorMessage = "User name cannot be empty.") }
            }
        }
    }

    // --- 既存のメソッド群 ---
    fun addThrow(isSuccess: Boolean) {
        viewModelScope.launch {
            val currentUiState = practiceSessionState.value
            val currentDistance = currentUiState.activeDistance ?: return@launch
            val currentAngle = currentUiState.selectedAngle

            val newDraft = ThrowDraft(
                userId = currentActiveUserId, // ★ 現在アクティブなユーザーIDを使用
                distance = currentDistance,
                angle = currentAngle,
                isSuccess = isSuccess,
                timestamp = Date().time,
                weather = currentUiState.sessionWeather,
                humidity = currentUiState.sessionHumidity,
                temperature = currentUiState.sessionTemperature,
                soil = currentUiState.sessionSoil,
                molkkyWeight = currentUiState.sessionMolkkyWeight
            )
            throwRepository.insertDraft(newDraft)
        }
    }

    fun undo() {
        viewModelScope.launch {
            if (practiceSessionState.value.canUndo) {
                throwRepository.deleteLastDraft(currentActiveUserId) // ★ 現在アクティブなユーザーIDを使用
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            if (practiceSessionState.value.isDirty) {
                throwRepository.commitDrafts(currentActiveUserId) // ★ 現在アクティブなユーザーIDを使用
                _uiState.update { it.copy(showExitConfirmDialog = false) }
            }
        }
    }

    fun selectDistance(distance: Float) { /* 変更なし */ _uiState.update { it.copy(activeDistance = distance) } }
    fun selectAngle(angle: String) { /* 変更なし */ _uiState.update { it.copy(selectedAngle = angle) } }
    fun requestAddDistance() { /* 変更なし */ _uiState.update { it.copy(showAddDistanceDialog = true) } }
    fun cancelAddDistance() { /* 変更なし */ _uiState.update { it.copy(showAddDistanceDialog = false) } }
    fun confirmAddDistance(newDistanceStr: String) { /* 変更なし */
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
    fun requestExitConfirmation() { /* 変更なし */
        if (practiceSessionState.value.isDirty) {
            _uiState.update { it.copy(showExitConfirmDialog = true) }
        }
    }
    fun confirmSaveAndExit() { /* 変更なし */ save() }
    fun confirmDiscardAndExit() { /* 変更なし */
        viewModelScope.launch {
            throwRepository.clearAllDrafts(currentActiveUserId) // ★ 現在アクティブなユーザーIDを使用
            _uiState.update { it.copy(showExitConfirmDialog = false) }
        }
    }
    fun cancelExit() { /* 変更なし */ _uiState.update { it.copy(showExitConfirmDialog = false) } }

    fun onNameButtonClicked() { /* 変更なし */ _uiState.update { it.copy(showUserDialog = true, userDialogErrorMessage = null) } } // エラーメッセージをリセット
    fun dismissUserDialog() { /* 変更なし */ _uiState.update { it.copy(showUserDialog = false, userDialogErrorMessage = null) } }

    fun onEnvConfigButtonClicked() { /* 変更なし */ _uiState.update { it.copy(showEnvConfigDialog = true) } }
    fun dismissEnvConfigDialog() { /* 変更なし */ _uiState.update { it.copy(showEnvConfigDialog = false) } }
    fun updateSessionWeather(weather: String?) { /* 変更なし */ _uiState.update { it.copy(sessionWeather = weather) } }
    fun updateSessionHumidity(humidity: Float?) { /* 変更なし */ _uiState.update { it.copy(sessionHumidity = humidity) } }
    fun updateSessionTemperature(temperature: Float?) { /* 変更なし */ _uiState.update { it.copy(sessionTemperature = temperature) } }
    fun updateSessionSoil(soil: String?) { /* 変更なし */ _uiState.update { it.copy(sessionSoil = soil) } }
    fun updateSessionMolkkyWeight(weight: Float?) { /* 変更なし */ _uiState.update { it.copy(sessionMolkkyWeight = weight) } }
}