package com.example.molkky_analysis.data.repository

import com.example.molkky_analysis.data.local.UserDao
import com.example.molkky_analysis.data.model.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) : IUserRepository {
    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }

    override suspend fun getUserById(userId: Int): User? {
        return userDao.getUserById(userId)
    }

    override suspend fun insertUser(user: User): Long {
        return userDao.insertUser(user)
    }
}