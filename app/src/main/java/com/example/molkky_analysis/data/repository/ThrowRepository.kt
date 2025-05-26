package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.local.ThrowDao
import com.example.molkky_analysis.data.model.ThrowDraft
import kotlinx.coroutines.flow.Flow

class ThrowRepository(private val throwDao: ThrowDao) : IThrowRepository { // IThrowRepository を実装

    override suspend fun insertDraft(draft: ThrowDraft) { // override を追加
        throwDao.insertDraft(draft)
    }

    override suspend fun deleteLastDraft(userId: Int) { // override を追加
        throwDao.deleteLastDraftForUser(userId)
    }

    override fun getDrafts(userId: Int): Flow<List<ThrowDraft>> { // override を追加
        return throwDao.getDraftsForUser(userId)
    }

    override fun getDraftCount(userId: Int): Flow<Int> { // override を追加
        return throwDao.getDraftCountForUser(userId)
    }

    override suspend fun commitDrafts(userId: Int) { // override を追加 [cite: 12]
        throwDao.commitDraftsToFinalForUser(userId)
    }

    override suspend fun clearAllDrafts(userId: Int) { // override を追加
        throwDao.clearAllDraftsForUser(userId)
    }
}