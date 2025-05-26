package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.model.User
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    fun getAllUsers(): Flow<List<User>>
    suspend fun getUserById(userId: Int): User?
    suspend fun insertUser(user: User): Long // 挿入されたユーザーのIDを返す
    // 必要に応じて updateUser, deleteUser なども追加
}