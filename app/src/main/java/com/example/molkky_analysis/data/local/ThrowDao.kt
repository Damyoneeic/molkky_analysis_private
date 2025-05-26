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
    suspend fun insertDraft(draft: ThrowDraft) // [cite: 12]

    @Query("DELETE FROM throw_draft WHERE id = (SELECT MAX(id) FROM throw_draft WHERE user_id = :userId)")
    suspend fun deleteLastDraftForUser(userId: Int) // [cite: 12] 仕様書では引数なしだが、ユーザーIDで絞ることを推奨

    @Query("SELECT * FROM throw_draft WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getDraftsForUser(userId: Int): Flow<List<ThrowDraft>>

    @Query("DELETE FROM throw_draft WHERE user_id = :userId")
    suspend fun clearAllDraftsForUser(userId: Int)

    @Query("SELECT COUNT(id) FROM throw_draft WHERE user_id = :userId")
    fun getDraftCountForUser(userId: Int): Flow<Int>


    // --- Final Record Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) //
    suspend fun insertThrowRecords(records: List<ThrowRecord>)


    // --- Transaction for committing drafts (仕様書 5.3 参照) ---
    @Transaction // [cite: 12]
    suspend fun commitDraftsToFinalForUser(userId: Int) { // [cite: 12]
        val draftsToCommit = getDraftsForUserOnce(userId)
        if (draftsToCommit.isNotEmpty()) {
            val recordsToInsert = draftsToCommit.map { draft ->
                ThrowRecord( //
                    // id は autoGenerate のため不要
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
            insertThrowRecords(recordsToInsert) //
            clearAllDraftsForUser(userId) // [cite: 15]
        }
    }

    @Query("SELECT * FROM throw_draft WHERE user_id = :userId")
    suspend fun getDraftsForUserOnce(userId: Int): List<ThrowDraft> // Helper for transaction
}