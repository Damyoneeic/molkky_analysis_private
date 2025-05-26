package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.ThrowRecord
import kotlinx.coroutines.flow.Flow

interface IThrowRepository {
    // Draft operations
    suspend fun insertDraft(draft: ThrowDraft): Long // Return generated ID
    suspend fun deleteLastDraftForUser(userId: Int) // Might need re-evaluation for session context
    fun getDraftsForUser(userId: Int): Flow<List<ThrowDraft>>
    fun getDraftCountForUser(userId: Int): Flow<Int>
    suspend fun commitAllDraftsForUser(userId: Int) // Commits all for a user
    suspend fun clearAllDraftsForUser(userId: Int) // Clears all for a user

    // New methods for specific drafts
    suspend fun getDraftsByIds(draftIds: List<Int>): List<ThrowDraft>
    suspend fun deleteDraftsByIds(draftIds: List<Int>)
    suspend fun commitSpecificDraftsToFinalAndDelete(draftsToCommit: List<ThrowDraft>)
    suspend fun clearSpecificDraftsFromTable(draftsToClear: List<ThrowDraft>)


    // ThrowRecord operations
    fun getAllThrowRecords(): Flow<List<ThrowRecord>>
    fun getThrowRecordsForUser(userId: Int): Flow<List<ThrowRecord>>
    suspend fun getThrowRecordById(recordId: Int): ThrowRecord?
    suspend fun updateThrowRecord(record: ThrowRecord)
    suspend fun deleteThrowRecord(record: ThrowRecord)
}