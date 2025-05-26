package com.example.molkky_analysis.ui.practice

import com.example.molkky_analysis.data.model.ThrowDraft

data class SessionState(
    val sessionId: Int, // セッションタブの一意なID (例: 0-4)
    var currentUserId: Int,
    var currentUserName: String,
    var throwsGroupedByDistance: Map<Float, List<SessionThrowDisplayData>> = emptyMap(),
    var configuredDistances: List<Float> = listOf(4.0f),
    var activeDistance: Float? = 4.0f,
    var selectedAngle: String = "CENTER",
    var sessionWeather: String? = null,
    var sessionHumidity: Float? = null,
    var sessionTemperature: Float? = null,
    var sessionSoil: String? = null,
    var sessionMolkkyWeight: Float? = null,
    var canUndo: Boolean = false,
    var isDirty: Boolean = false, // このセッションの下書きに基づくダーティフラグ
    var drafts: List<ThrowDraft> = emptyList() // このセッションのメモリ内下書き
)