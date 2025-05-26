package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.local.ThrowDao
import com.example.molkky_analysis.data.model.ThrowDraft
import kotlinx.coroutines.flow.Flow

class ThrowRepository(private val throwDao: ThrowDao) {

    suspend fun insertDraft(draft: ThrowDraft) { // [cite: 12]
        throwDao.insertDraft(draft)
    }

    suspend fun deleteLastDraft(userId: Int) { // [cite: 13]
        throwDao.deleteLastDraftForUser(userId)
    }

    fun getDrafts(userId: Int): Flow<List<ThrowDraft>> {
        return throwDao.getDraftsForUser(userId)
    }

    fun getDraftCount(userId: Int): Flow<Int> {
        return throwDao.getDraftCountForUser(userId)
    }

    suspend fun commitDrafts(userId: Int) { // [cite: 12]
        throwDao.commitDraftsToFinalForUser(userId)
    }

    suspend fun clearAllDrafts(userId: Int) {
        throwDao.clearAllDraftsForUser(userId)
    }
}