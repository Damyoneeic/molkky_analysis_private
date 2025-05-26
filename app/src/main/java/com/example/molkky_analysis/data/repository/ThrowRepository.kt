package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.local.ThrowDao
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.ThrowRecord // ThrowRecordをインポート
import kotlinx.coroutines.flow.Flow

class ThrowRepository(private val throwDao: ThrowDao) : IThrowRepository {

    // Draft operations
    override suspend fun insertDraft(draft: ThrowDraft) { throwDao.insertDraft(draft) }
    override suspend fun deleteLastDraft(userId: Int) { throwDao.deleteLastDraftForUser(userId) }
    override fun getDrafts(userId: Int): Flow<List<ThrowDraft>> = throwDao.getDraftsForUser(userId)
    override fun getDraftCount(userId: Int): Flow<Int> = throwDao.getDraftCountForUser(userId)
    override suspend fun commitDrafts(userId: Int) { throwDao.commitDraftsToFinalForUser(userId) }
    override suspend fun clearAllDrafts(userId: Int) { throwDao.clearAllDraftsForUser(userId) }

    // ThrowRecord operations
    override fun getAllThrowRecords(): Flow<List<ThrowRecord>> { // ★ 追加
        return throwDao.getAllThrowRecords()
    }
    override fun getThrowRecordsForUser(userId: Int): Flow<List<ThrowRecord>> { // ★ 追加
        return throwDao.getThrowRecordsForUser(userId)
    }
    override suspend fun getThrowRecordById(recordId: Int): ThrowRecord? { // ★ 追加
        return throwDao.getThrowRecordById(recordId)
    }
    override suspend fun updateThrowRecord(record: ThrowRecord) { // ★ 追加
        throwDao.updateThrowRecord(record)
    }
    override suspend fun deleteThrowRecord(record: ThrowRecord) { // ★ 追加
        throwDao.deleteThrowRecord(record)
    }
}