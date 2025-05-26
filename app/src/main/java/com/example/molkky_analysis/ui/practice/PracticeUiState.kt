package com.example.molkky_analysis.ui.practice

// 仕様書のサンプル (6章) とPracticePageの要素を考慮
data class PracticeUiState(
    val currentUserId: Int = 1, // 仮のデフォルト。実際には選択/設定されるべき
    val currentUserName: String = "Player 1", // 仮
    val throwsGroupedByDistance: Map<Float, List<SessionThrowDisplayData>> = emptyMap(),
    val activeDistance: Float? = null,
    val selectedAngle: String = "CENTER", // 'LEFT', 'CENTER', 'RIGHT'
    // TODO: Environment Config State
    val canUndo: Boolean = false, // [cite: 16]
    val isDirty: Boolean = false, // Draft件数 > 0 で true [cite: 16]
    val showExitConfirmDialog: Boolean = false, // [cite: 17]
    // TODO: セッションタブの状態
    // TODO: ユーザー選択ダイアログ、Env Configダイアログの表示状態
)

// UI表示用の簡略化された投擲データ
data class SessionThrowDisplayData(
    val distance: Float,
    val isSuccess: Boolean,
    val angle: String
    // 必要に応じて他の情報も追加
)