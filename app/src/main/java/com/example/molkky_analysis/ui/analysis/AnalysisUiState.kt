package com.example.molkky_analysis.ui.analysis

import com.example.molkky_analysis.data.model.User

// プロット用のデータポイント
data class DistanceSuccessRateData(
    val distance: Float,
    val successRate: Float,    // 0.0 to 1.0
    val totalThrows: Int,
    val successes: Int,
    val errorMargin: Float? = null // 例: 成功率 ± errorMargin
)

data class AnalysisUiState(
    val isLoading: Boolean = true,
    val availableUsers: List<User> = emptyList(),
    val selectedUserId: Int? = null, // null の場合は全ユーザー
    val plotData: List<DistanceSuccessRateData> = emptyList(),
    val summaryStats: SummaryStats = SummaryStats() // サマリー統計用
)

// サマリー統計用データクラス (spec.rtf より)
data class SummaryStats(
    val totalThrows: Int = 0,
    val averageDistance: Float? = null, // 投擲があった場合のみ
    val overallSuccessRate: Float? = null, // 投擲があった場合のみ
    val successRateByAngle: Map<String, Float?> = emptyMap() // LEFT, CENTER, RIGHT
)