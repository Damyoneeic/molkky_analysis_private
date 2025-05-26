package com.example.molkky_analysis.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Delete
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.ThrowRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ThrowDao {
    // --- Draft Operations (now session-specific) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ThrowDraft) // draft object now contains sessionId

    @Query("DELETE FROM throw_draft WHERE id = (SELECT MAX(id) FROM throw_draft WHERE user_id = :userId AND session_id = :sessionId)")
    suspend fun deleteLastDraftForUserAndSession(userId: Int, sessionId: String)

    @Query("SELECT * FROM throw_draft WHERE user_id = :userId AND session_id = :sessionId ORDER BY timestamp DESC")
    fun getDraftsForUserAndSession(userId: Int, sessionId: String): Flow<List<ThrowDraft>>

    @Query("DELETE FROM throw_draft WHERE user_id = :userId AND session_id = :sessionId")
    suspend fun clearAllDraftsForUserAndSession(userId: Int, sessionId: String)

    @Query("SELECT COUNT(id) FROM throw_draft WHERE user_id = :userId AND session_id = :sessionId")
    fun getDraftCountForUserAndSession(userId: Int, sessionId: String): Flow<Int>


    // --- Final Record Operations (remain largely user-specific, not session-specific for ThrowRecord) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThrowRecords(records: List<ThrowRecord>)

    @Query("SELECT * FROM throw_record ORDER BY timestamp DESC")
    fun getAllThrowRecords(): Flow<List<ThrowRecord>>

    @Query("SELECT * FROM throw_record WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getThrowRecordsForUser(userId: Int): Flow<List<ThrowRecord>>

    @Query("SELECT * FROM throw_record WHERE id = :recordId")
    suspend fun getThrowRecordById(recordId: Int): ThrowRecord?

    @Update
    suspend fun updateThrowRecord(record: ThrowRecord)

    @Delete
    suspend fun deleteThrowRecord(record: ThrowRecord)

    // --- Transaction for committing drafts (now session-specific) ---
    @Transaction
    suspend fun commitDraftsToFinalForUserAndSession(userId: Int, sessionId: String) {
        val draftsToCommit = getDraftsForUserAndSessionOnce(userId, sessionId) // Helper needs update
        if (draftsToCommit.isNotEmpty()) {
            val recordsToInsert = draftsToCommit.map { draft ->
                ThrowRecord(
                    // id is auto-generated for ThrowRecord
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
                    // sessionId is NOT part of ThrowRecord as per original schema
                )
            }
            insertThrowRecords(recordsToInsert)
            clearAllDraftsForUserAndSession(userId, sessionId) // Clear only the committed session's drafts
        }
    }

    // Helper for transaction, now session-specific
    @Query("SELECT * FROM throw_draft WHERE user_id = :userId AND session_id = :sessionId")
    suspend fun getDraftsForUserAndSessionOnce(userId: Int, sessionId: String): List<ThrowDraft>
}