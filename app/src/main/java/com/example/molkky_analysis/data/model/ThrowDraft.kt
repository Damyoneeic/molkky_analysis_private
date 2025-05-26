package com.example.molkky_analysis.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID // Import UUID

@Entity(
    tableName = "throw_draft",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["user_id"]),
        androidx.room.Index(value = ["timestamp"]),
        androidx.room.Index(value = ["user_id", "session_id"]) // Index for session-specific queries
    ]
)
data class ThrowDraft(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "user_id")
    val userId: Int,
    @ColumnInfo(name = "session_id") // New field to identify the session
    val sessionId: String, // This will typically be the SessionTabUiInfo.id (a UUID)
    val distance: Float,
    val angle: String,
    val weather: String?,
    val humidity: Float?,
    val temperature: Float?,
    val soil: String?,
    @ColumnInfo(name = "molkky_weight")
    val molkkyWeight: Float?,
    @ColumnInfo(name = "is_success")
    val isSuccess: Boolean,
    val timestamp: Long
)