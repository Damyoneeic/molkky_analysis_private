package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.model.ThrowDraft
import kotlinx.coroutines.flow.Flow

interface IThrowRepository {
    suspend fun insertDraft(draft: ThrowDraft)
    suspend fun deleteLastDraft(userId: Int)
    fun getDrafts(userId: Int): Flow<List<ThrowDraft>>
    fun getDraftCount(userId: Int): Flow<Int>
    suspend fun commitDrafts(userId: Int)
    suspend fun clearAllDrafts(userId: Int)
}