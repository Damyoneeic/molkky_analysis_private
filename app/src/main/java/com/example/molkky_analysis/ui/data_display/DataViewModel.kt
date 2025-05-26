package com.example.molkky_analysis.ui.data_display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molkky_analysis.data.model.ThrowRecord
import com.example.molkky_analysis.data.model.User
import com.example.molkky_analysis.data.repository.IThrowRepository
import com.example.molkky_analysis.data.repository.IUserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// UI表示用のデータクラス
data class DisplayableThrowRecord(
    val record: ThrowRecord,
    val userName: String,
    val formattedTimestamp: String
)

data class DataUiState(
    val throwRecords: List<DisplayableThrowRecord> = emptyList(),
    val isLoading: Boolean = true,
    val selectedUserFilter: User? = null, // ユーザーでフィルタリングする場合
    val availableUsers: List<User> = emptyList() // フィルター用ユーザーリスト
)

class DataViewModel(
    private val throwRepository: IThrowRepository,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val allUsersFlow = userRepository.getAllUsers()
    private val allThrowRecordsFlow = throwRepository.getAllThrowRecords() // TODO: ユーザーフィルターに応じてFlowを切り替える

    val uiState: StateFlow<DataUiState> = combine(
        allThrowRecordsFlow,
        allUsersFlow
    ) { records, users ->
        val displayableRecords = records.map { record ->
            val userName = users.find { it.id == record.userId }?.name ?: "Unknown User"
            DisplayableThrowRecord(
                record = record,
                userName = userName,
                formattedTimestamp = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(record.timestamp))
            )
        }
        DataUiState(
            throwRecords = displayableRecords,
            isLoading = false,
            availableUsers = users
            // selectedUserFilter は別途更新
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataUiState(isLoading = true)
    )

    fun deleteThrowRecord(record: ThrowRecord) {
        viewModelScope.launch {
            throwRepository.deleteThrowRecord(record)
        }
    }

    // TODO: ユーザーフィルターの選択・解除ロジック
    // TODO: レコード編集ダイアログ表示と更新ロジック
    // TODO: 新規レコード作成ロジック (DataPageから直接作成する場合)
}