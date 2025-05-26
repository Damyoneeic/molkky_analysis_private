package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.ThrowRecord // ThrowRecordをインポート
import kotlinx.coroutines.flow.Flow

interface IThrowRepository {
    // Draft operations
    suspend fun insertDraft(draft: ThrowDraft)
    suspend fun deleteLastDraft(userId: Int)
    fun getDrafts(userId: Int): Flow<List<ThrowDraft>>
    fun getDraftCount(userId: Int): Flow<Int>
    suspend fun commitDrafts(userId: Int)
    suspend fun clearAllDrafts(userId: Int)

    // ThrowRecord operations
    fun getAllThrowRecords(): Flow<List<ThrowRecord>> // ★ 追加
    fun getThrowRecordsForUser(userId: Int): Flow<List<ThrowRecord>> // ★ 追加
    suspend fun getThrowRecordById(recordId: Int): ThrowRecord? // ★ 追加
    suspend fun updateThrowRecord(record: ThrowRecord) // ★ 追加
    suspend fun deleteThrowRecord(record: ThrowRecord) // ★ 追加
}