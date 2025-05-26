package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.local.ThrowDao
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.ThrowRecord
import kotlinx.coroutines.flow.Flow

class ThrowRepository(private val throwDao: ThrowDao) : IThrowRepository {

    // Draft operations (now session-specific)
    override suspend fun insertDraft(draft: ThrowDraft) { throwDao.insertDraft(draft) }
    override suspend fun deleteLastDraft(userId: Int, sessionId: String) { throwDao.deleteLastDraftForUserAndSession(userId, sessionId) }
    override fun getDrafts(userId: Int, sessionId: String): Flow<List<ThrowDraft>> = throwDao.getDraftsForUserAndSession(userId, sessionId)
    override fun getDraftCount(userId: Int, sessionId: String): Flow<Int> = throwDao.getDraftCountForUserAndSession(userId, sessionId)
    override suspend fun commitDrafts(userId: Int, sessionId: String) { throwDao.commitDraftsToFinalForUserAndSession(userId, sessionId) }
    override suspend fun clearAllDrafts(userId: Int, sessionId: String) { throwDao.clearAllDraftsForUserAndSession(userId, sessionId) }

    // ThrowRecord operations
    override fun getAllThrowRecords(): Flow<List<ThrowRecord>> = throwDao.getAllThrowRecords()
    override fun getThrowRecordsForUser(userId: Int): Flow<List<ThrowRecord>> = throwDao.getThrowRecordsForUser(userId)
    override suspend fun getThrowRecordById(recordId: Int): ThrowRecord? = throwDao.getThrowRecordById(recordId)
    override suspend fun updateThrowRecord(record: ThrowRecord) { throwDao.updateThrowRecord(record) }
    override suspend fun deleteThrowRecord(record: ThrowRecord) { throwDao.deleteThrowRecord(record) }
}