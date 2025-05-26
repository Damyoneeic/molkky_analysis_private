package com.example.molkky_analysis.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
        androidx.room.Index(value = ["timestamp"])
    ]
)
data class ThrowDraft(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0, // Changed to var
    @ColumnInfo(name = "user_id")
    val userId: Int,
    val distance: Float,
    val angle: String, // CHECK(angle IN ('LEFT','CENTER','RIGHT'))
    val weather: String?,
    val humidity: Float?,
    val temperature: Float?,
    val soil: String?,
    @ColumnInfo(name = "molkky_weight")
    val molkkyWeight: Float?,
    @ColumnInfo(name = "is_success")
    val isSuccess: Boolean, // CHECK(is_success IN (0,1))
    val timestamp: Long // UNIX ms
)