package com.example.molkky_analysis.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molkky_analysis.data.model.ThrowRecord
import com.example.molkky_analysis.data.model.User
import com.example.molkky_analysis.data.repository.IThrowRepository
import com.example.molkky_analysis.data.repository.IUserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map // mapオペレータをインポート
import kotlinx.coroutines.flow.stateIn
// import kotlinx.coroutines.flow.update // _uiStateがないため、updateは直接使わない
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AnalysisViewModel(
    private val throwRepository: IThrowRepository,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val _selectedUserIdFlow = MutableStateFlow<Int?>(null) // nullで全ユーザー
    private val _isLoadingData = MutableStateFlow(true) // ★ データロード状態を管理

    val availableUsers: StateFlow<List<User>> = userRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val processedAnalysisDataFlow: StateFlow<Pair<List<DistanceSuccessRateData>, SummaryStats>> =
        _selectedUserIdFlow.flatMapLatest { userId ->
            _isLoadingData.value = true // ★ ユーザー選択変更時にロード開始
            val recordsFlow = if (userId == null) {
                throwRepository.getAllThrowRecords()
            } else {
                throwRepository.getThrowRecordsForUser(userId)
            }
            recordsFlow.map { records ->
                val plotData = calculatePlotData(records)
                val summaryStats = calculateSummaryStats(records)
                _isLoadingData.value = false // ★ データ処理後にロード完了
                plotData to summaryStats
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            initialValue = Pair(emptyList<DistanceSuccessRateData>(), SummaryStats()) // 初期値
        )

    val uiState: StateFlow<AnalysisUiState> = combine(
        processedAnalysisDataFlow,
        availableUsers,
        _selectedUserIdFlow,
        _isLoadingData // ★ isLoading状態をcombineに含める
    ) { (plotData, summaryStats), users, selectedId, isLoading ->
        AnalysisUiState(
            isLoading = isLoading, // ★ 管理しているisLoading状態を使用
            availableUsers = users,
            selectedUserId = selectedId,
            plotData = plotData,
            summaryStats = summaryStats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalysisUiState(isLoading = true) // UI全体の初期状態
    )

    private fun calculatePlotData(records: List<ThrowRecord>): List<DistanceSuccessRateData> {
        if (records.isEmpty()) return emptyList()

        return records
            .groupBy { it.distance }
            .map { (distance, throwsAtDistance) ->
                val totalThrows = throwsAtDistance.size
                val successes = throwsAtDistance.count { it.isSuccess }
                val successRate = if (totalThrows > 0) successes.toFloat() / totalThrows else 0f

                val errorMargin = if (totalThrows > 0) {
                    val p_hat = successRate
                    val z = 1.96f
                    if (p_hat == 0f || p_hat == 1f) {
                        null
                    } else {
                        z * sqrt(p_hat * (1 - p_hat) / totalThrows)
                    }
                } else {
                    null
                }

                DistanceSuccessRateData(
                    distance = distance,
                    successRate = successRate,
                    totalThrows = totalThrows,
                    successes = successes,
                    errorMargin = errorMargin
                )
            }
            .sortedBy { it.distance }
    }

    private fun calculateSummaryStats(records: List<ThrowRecord>): SummaryStats {
        if (records.isEmpty()) return SummaryStats()

        val totalThrows = records.size
        val overallSuccesses = records.count { it.isSuccess }
        val overallSuccessRate = if (totalThrows > 0) overallSuccesses.toFloat() / totalThrows else null
        val averageDistance = if (records.isNotEmpty()) records.map { it.distance }.average().toFloat() else null


        val successRateByAngle = records
            .groupBy { it.angle }
            .mapValues { (_, throwsInAngle) ->
                val totalInAngle = throwsInAngle.size
                val successesInAngle = throwsInAngle.count { it.isSuccess }
                if (totalInAngle > 0) successesInAngle.toFloat() / totalInAngle else null
            }

        return SummaryStats(
            totalThrows = totalThrows,
            averageDistance = averageDistance,
            overallSuccessRate = overallSuccessRate,
            successRateByAngle = successRateByAngle
        )
    }

    fun selectUser(userId: Int?) {
        // _isLoadingData.value = true // flatMapLatest の開始時に設定するので、ここでも設定可能だが必須ではない
        _selectedUserIdFlow.value = userId
        // ★ エラーの原因となっていた _uiState.update の行は削除 (または上記のように _isLoadingData を使う)
    }
}