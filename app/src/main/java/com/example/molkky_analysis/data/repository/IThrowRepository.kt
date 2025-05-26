package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.ThrowRecord
import kotlinx.coroutines.flow.Flow

interface IThrowRepository {
    // Draft operations (now session-specific)
    suspend fun insertDraft(draft: ThrowDraft) // ThrowDraft object will contain sessionId
    suspend fun deleteLastDraft(userId: Int, sessionId: String)
    fun getDrafts(userId: Int, sessionId: String): Flow<List<ThrowDraft>>
    fun getDraftCount(userId: Int, sessionId: String): Flow<Int>
    suspend fun commitDrafts(userId: Int, sessionId: String)
    suspend fun clearAllDrafts(userId: Int, sessionId: String)

    // ThrowRecord operations (remain unchanged here as ThrowRecord is not session-specific)
    fun getAllThrowRecords(): Flow<List<ThrowRecord>>
    fun getThrowRecordsForUser(userId: Int): Flow<List<ThrowRecord>>
    suspend fun getThrowRecordById(recordId: Int): ThrowRecord?
    suspend fun updateThrowRecord(record: ThrowRecord)
    suspend fun deleteThrowRecord(record: ThrowRecord)
}