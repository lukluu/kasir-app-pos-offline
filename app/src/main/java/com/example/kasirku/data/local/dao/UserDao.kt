package com.example.kasirku.data.local.dao

import androidx.room.*
import com.example.kasirku.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // Nama fungsi diubah agar cocok dengan AuthRepository
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity)

    // Fungsi diubah untuk mencari berdasarkan email, bukan username
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    // Fungsi ini tetap berguna untuk keperluan lain
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdSuspend(id: Int): UserEntity?


    @Update
    suspend fun updateUser(user: UserEntity)


}