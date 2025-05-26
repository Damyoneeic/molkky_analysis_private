package com.example.molkky_analysis.ui.practice

import com.example.molkky_analysis.data.model.User

// SessionState.kt が同じパッケージに定義されていることを前提とします

data class PracticeUiState(
    val isLoading: Boolean = true, // 初期ロード中フラグ
    val sessions: Map<Int, SessionState> = mapOf(
        0 to SessionState( // デフォルトの初期セッション (セッションID 0)
            sessionId = 0,
            currentUserId = 1, // デフォルトユーザーID (例: Player 1)
            currentUserName = "Player 1", // デフォルトユーザー名
            configuredDistances = listOf(4.0f),
            activeDistance = 4.0f,
            selectedAngle = "CENTER",
            // 他のSessionStateの初期値はSessionStateクラスのデフォルト値を使用
            drafts = emptyList(),
            throwsGroupedByDistance = emptyMap(),
            canUndo = false,
            isDirty = false
        )
    ),
    val activeSessionTabs: List<Int> = listOf(0), // 現在開いているタブのセッションIDリスト
    val currentActiveSessionId: Int = 0,          // 現在選択されているタブのセッションID

    // --- 以下はセッションに依存しない、または現在のセッションに対するダイアログの状態など ---
    val availableUsers: List<User> = emptyList(),   // ユーザー切り替え用

    // ダイアログ表示フラグ
    val showExitConfirmDialog: Boolean = false,
    val showAddDistanceDialog: Boolean = false,     // 現在アクティブなセッションに対して動作
    val showUserDialog: Boolean = false,            // 現在アクティブなセッションのユーザー操作、またはグローバルなユーザー追加
    val showEnvConfigDialog: Boolean = false,       // 現在アクティブなセッションの環境設定

    // ユーザー操作関連
    val userDialogErrorMessage: String? = null,
    val showUserSwitchConfirmDialog: Boolean = false, // 現在のセッション内でのユーザー切り替え確認
    val pendingUserSwitchTargetId: Int? = null,       // 切り替え対象のユーザーID
    val pendingUserSwitchSessionId: Int? = null,      // ユーザー切り替えがペンディング中のセッションID

    // 距離削除確認ダイアログ
    val showDeleteDistanceConfirmDialog: Boolean = false,
    val distanceToDelete: Float? = null,

    // ユーザー削除確認ダイアログ (これはグローバルなユーザー削除)
    val showDeleteUserConfirmDialog: Boolean = false,
    val userToDelete: User? = null,

    // ★★★ Session Deletion Dialog State ★★★
    val showDeleteSessionConfirmDialog: Boolean = false,
    val sessionToDeleteId: Int? = null
    // ★★★ End of Session Deletion Dialog State ★★★

) {
    // 現在アクティブなセッションのSessionStateを簡単に取得するためのヘルパー
    val currentSessionState: SessionState? get() = sessions[currentActiveSessionId]
}

// このデータクラスは変更なし (PracticePageなどで投擲結果の表示に使われる)
data class SessionThrowDisplayData(
    val distance: Float,
    val isSuccess: Boolean,
    val angle: String
)