package com.example.molkky_analysis.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update // Added
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.ThrowRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ThrowDao {
    // --- Draft Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: ThrowDraft): Long // Returns the new rowId

    @Query("DELETE FROM throw_draft WHERE id = (SELECT MAX(id) FROM throw_draft WHERE user_id = :userId)")
    suspend fun deleteLastDraftForUser(userId: Int) // This might need to be more specific if drafts are not session-isolated in DB

    @Query("SELECT * FROM throw_draft WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getDraftsForUser(userId: Int): Flow<List<ThrowDraft>>

    @Query("DELETE FROM throw_draft WHERE user_id = :userId")
    suspend fun clearAllDraftsForUser(userId: Int) // Clears ALL drafts for a user

    @Query("SELECT COUNT(id) FROM throw_draft WHERE user_id = :userId")
    fun getDraftCountForUser(userId: Int): Flow<Int>

    // New methods for session-specific draft management (by ID)
    @Query("DELETE FROM throw_draft WHERE id IN (:draftIds)")
    suspend fun deleteDraftsByIds(draftIds: List<Int>)

    @Query("SELECT * FROM throw_draft WHERE id IN (:draftIds)")
    suspend fun getDraftsByIds(draftIds: List<Int>): List<ThrowDraft>

    // --- Final Record Operations ---
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

    // --- Transactions ---
    // Old commit: commits ALL drafts for a user
    @Transaction
    suspend fun commitAllDraftsToFinalForUser(userId: Int) {
        val draftsToCommit = getDraftsForUserOnce(userId) // This is a suspend fun
        if (draftsToCommit.isNotEmpty()) {
            val recordsToInsert = draftsToCommit.map { draft ->
                ThrowRecord(
                    userId = draft.userId, distance = draft.distance, angle = draft.angle,
                    weather = draft.weather, humidity = draft.humidity, temperature = draft.temperature,
                    soil = draft.soil, molkkyWeight = draft.molkkyWeight,
                    isSuccess = draft.isSuccess, timestamp = draft.timestamp
                )
            }
            insertThrowRecords(recordsToInsert)
            clearAllDraftsForUser(userId) // Clears all for the user
        }
    }

    // New transaction: commits a specific list of drafts
    @Transaction
    suspend fun commitSpecificDraftsToFinalAndDelete(draftsToCommit: List<ThrowDraft>) {
        if (draftsToCommit.isNotEmpty()) {
            val recordsToInsert = draftsToCommit.map { draft ->
                ThrowRecord(
                    // id is autoGenerate for ThrowRecord
                    userId = draft.userId, distance = draft.distance, angle = draft.angle,
                    weather = draft.weather, humidity = draft.humidity, temperature = draft.temperature,
                    soil = draft.soil, molkkyWeight = draft.molkkyWeight,
                    isSuccess = draft.isSuccess, timestamp = draft.timestamp
                )
            }
            insertThrowRecords(recordsToInsert)
            val draftIds = draftsToCommit.map { it.id }.filter { it > 0 }
            if (draftIds.isNotEmpty()) {
                deleteDraftsByIds(draftIds)
            }
        }
    }

    // New transaction: clears a specific list of drafts
    @Transaction
    suspend fun clearSpecificDraftsFromTable(draftsToClear: List<ThrowDraft>) {
        if (draftsToClear.isNotEmpty()) {
            val draftIds = draftsToClear.map { it.id }.filter { it > 0 }
            if (draftIds.isNotEmpty()) {
                deleteDraftsByIds(draftIds)
            }
        }
    }


    @Query("SELECT * FROM throw_draft WHERE user_id = :userId")
    suspend fun getDraftsForUserOnce(userId: Int): List<ThrowDraft> // Helper for transaction
}