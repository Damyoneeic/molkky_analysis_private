package com.example.molkky_analysis.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.ThrowRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ThrowDao {
    // --- Draft Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ThrowDraft)

    @Query("DELETE FROM throw_draft WHERE id = (SELECT MAX(id) FROM throw_draft WHERE user_id = :userId)")
    suspend fun deleteLastDraftForUser(userId: Int)

    @Query("SELECT * FROM throw_draft WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getDraftsForUser(userId: Int): Flow<List<ThrowDraft>>

    @Query("DELETE FROM throw_draft WHERE user_id = :userId")
    suspend fun clearAllDraftsForUser(userId: Int)

    @Query("SELECT COUNT(id) FROM throw_draft WHERE user_id = :userId")
    fun getDraftCountForUser(userId: Int): Flow<Int>


    // --- Final Record Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThrowRecords(records: List<ThrowRecord>)

    // ★ 新しいメソッド: 全てのThrowRecordをタイムスタンプの降順で取得
    @Query("SELECT * FROM throw_record ORDER BY timestamp DESC")
    fun getAllThrowRecords(): Flow<List<ThrowRecord>>

    // ★ 新しいメソッド: 特定のユーザーのThrowRecordをタイムスタンプの降順で取得
    @Query("SELECT * FROM throw_record WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getThrowRecordsForUser(userId: Int): Flow<List<ThrowRecord>>

    // ★ 新しいメソッド: IDで特定のThrowRecordを取得 (編集・削除用)
    @Query("SELECT * FROM throw_record WHERE id = :recordId")
    suspend fun getThrowRecordById(recordId: Int): ThrowRecord?

    // ★ 新しいメソッド: ThrowRecordを更新 (編集用)
    @androidx.room.Update
    suspend fun updateThrowRecord(record: ThrowRecord)

    // ★ 新しいメソッド: ThrowRecordを削除 (削除用)
    @androidx.room.Delete
    suspend fun deleteThrowRecord(record: ThrowRecord)

    // --- Transaction for committing drafts ---
    @Transaction
    suspend fun commitDraftsToFinalForUser(userId: Int) {
        val draftsToCommit = getDraftsForUserOnce(userId)
        if (draftsToCommit.isNotEmpty()) {
            val recordsToInsert = draftsToCommit.map { draft ->
                ThrowRecord(
                    userId = draft.userId,
                    distance = draft.distance,
                    angle = draft.angle,
                    weather = draft.weather,
                    humidity = draft.humidity,
                    temperature = draft.temperature,
                    soil = draft.soil,
                    molkkyWeight = draft.molkkyWeight,
                    isSuccess = draft.isSuccess,
                    timestamp = draft.timestamp
                )
            }
            insertThrowRecords(recordsToInsert)
            clearAllDraftsForUser(userId)
        }
    }

    @Query("SELECT * FROM throw_draft WHERE user_id = :userId")
    suspend fun getDraftsForUserOnce(userId: Int): List<ThrowDraft> // Helper for transaction
}