package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.local.ThrowDao
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.ThrowRecord
import kotlinx.coroutines.flow.Flow

class ThrowRepository(private val throwDao: ThrowDao) : IThrowRepository {

    // Draft operations
    override suspend fun insertDraft(draft: ThrowDraft): Long = throwDao.insertDraft(draft)
    override suspend fun deleteLastDraftForUser(userId: Int) { throwDao.deleteLastDraftForUser(userId) } // ★1
    override fun getDraftsForUser(userId: Int): Flow<List<ThrowDraft>> = throwDao.getDraftsForUser(userId) // ★1

    // ↓↓↓ ここのメソッド名を修正 ↓↓↓
    override fun getDraftCountForUser(userId: Int): Flow<Int> = throwDao.getDraftCountForUser(userId) // ★2 メソッド名を getDraftCountForUser に変更
    // ↑↑↑ ここのメソッド名を修正 ↑↑↑

    override suspend fun commitAllDraftsForUser(userId: Int) { throwDao.commitAllDraftsToFinalForUser(userId) } // ★1
    override suspend fun clearAllDraftsForUser(userId: Int) { throwDao.clearAllDraftsForUser(userId) } // ★1

    // New methods for session-specific draft management
    override suspend fun getDraftsByIds(draftIds: List<Int>): List<ThrowDraft> = throwDao.getDraftsByIds(draftIds)
    override suspend fun deleteDraftsByIds(draftIds: List<Int>) = throwDao.deleteDraftsByIds(draftIds)
    override suspend fun commitSpecificDraftsToFinalAndDelete(draftsToCommit: List<ThrowDraft>) = throwDao.commitSpecificDraftsToFinalAndDelete(draftsToCommit)
    override suspend fun clearSpecificDraftsFromTable(draftsToClear: List<ThrowDraft>) = throwDao.clearSpecificDraftsFromTable(draftsToClear)


    // ThrowRecord operations
    override fun getAllThrowRecords(): Flow<List<ThrowRecord>> = throwDao.getAllThrowRecords()
    override fun getThrowRecordsForUser(userId: Int): Flow<List<ThrowRecord>> = throwDao.getThrowRecordsForUser(userId)
    override suspend fun getThrowRecordById(recordId: Int): ThrowRecord? = throwDao.getThrowRecordById(recordId)
    override suspend fun updateThrowRecord(record: ThrowRecord) = throwDao.updateThrowRecord(record)
    override suspend fun deleteThrowRecord(record: ThrowRecord) = throwDao.deleteThrowRecord(record)
}