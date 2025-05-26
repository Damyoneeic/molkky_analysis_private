package com.example.molkky_analysis.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.repository.ThrowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class PracticeViewModel(
    private val throwRepository: ThrowRepository,
    private val userId: Int // ViewModel生成時にユーザーIDを渡す
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState(currentUserId = userId))
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    // Drafts from repository
    private val draftsFlow: StateFlow<List<ThrowDraft>> = throwRepository.getDrafts(userId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Derived state based on drafts
    val practiceSessionState: StateFlow<PracticeUiState> =
        combine(
            draftsFlow,
            _uiState // For activeDistance, selectedAngle etc.
        ) { drafts, currentDynamicState ->
            val grouped = drafts.groupBy { it.distance }
                .mapValues { entry ->
                    entry.value.map { draft ->
                        SessionThrowDisplayData(draft.distance, draft.isSuccess, draft.angle)
                    }
                }
            currentDynamicState.copy(
                throwsGroupedByDistance = grouped,
                canUndo = drafts.isNotEmpty(),
                isDirty = drafts.isNotEmpty()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value)


    fun addThrow(isSuccess: Boolean) { // [cite: 16]
        viewModelScope.launch {
            val currentDistance = uiState.value.activeDistance ?: return@launch // アクティブな距離がなければ追加しない
            val currentAngle = uiState.value.selectedAngle
            // TODO: 環境設定値を取得するロジック
            val newDraft = ThrowDraft(
                userId = userId,
                distance = currentDistance,
                angle = currentAngle,
                isSuccess = isSuccess,
                timestamp = Date().time,
                // 以下は環境設定から取得
                weather = null, // Placeholder
                humidity = null, // Placeholder
                temperature = null, // Placeholder
                soil = null, // Placeholder
                molkkyWeight = null // Placeholder
            )
            throwRepository.insertDraft(newDraft)
        }
    }

    fun undo() { // [cite: 16]
        viewModelScope.launch {
            if (uiState.value.canUndo) {
                throwRepository.deleteLastDraft(userId)
            }
        }
    }

    fun save() { // [cite: 16]
        viewModelScope.launch {
            if (uiState.value.isDirty) {
                throwRepository.commitDrafts(userId)
                // ダイアログを閉じるためにisDirtyとshowExitConfirmDialogを更新
                _uiState.value = uiState.value.copy(isDirty = false, canUndo = false, showExitConfirmDialog = false)
            }
        }
    }

    fun selectDistance(distance: Float) {
        _uiState.value = uiState.value.copy(activeDistance = distance)
    }

    fun selectAngle(angle: String) {
        _uiState.value = uiState.value.copy(selectedAngle = angle)
    }

    // --- Exit Confirmation Dialog Logic (仕様書5.4, 6章) ---
    fun requestExitConfirmation() {
        if (uiState.value.isDirty) {
            _uiState.value = uiState.value.copy(showExitConfirmDialog = true)
        } else {
            // ダーティでなければ、ViewModelは直接画面遷移をトリガーせず、
            // UI側で onReturnToHome を呼ぶべき。ViewModelは状態を提供する。
            // このメソッドは、UIが onReturnToHome を呼ぶべきかどうかを判断するのに使える。
        }
    }

    fun confirmSaveAndExit() { // [cite: 17] Dialog "Save"
        save() // saveメソッド内でisDirtyとshowExitConfirmDialogが更新される
        // 実際の画面遷移はUI側 (PracticePage) で行う
    }

    fun confirmDiscardAndExit() { // [cite: 17] Dialog "Discard"
        viewModelScope.launch {
            throwRepository.clearAllDrafts(userId)
            _uiState.value = uiState.value.copy(isDirty = false, canUndo = false, showExitConfirmDialog = false)
            // 実際の画面遷移はUI側 (PracticePage) で行う
        }
    }

    fun cancelExit() { // [cite: 17] Dialog "Cancel"
        _uiState.value = uiState.value.copy(showExitConfirmDialog = false)
    }

    // TODO: Name Button, Env Config, Session Tabs, Add Distance Row logic
}