package com.example.kasirku.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.kasirku.data.local.dao.UserDao
import com.example.kasirku.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        private const val KEY_LOGGED_IN_USER_ID = "key_logged_in_user_id"
    }

    private fun hashPassword(password: String): String {
        return "hashed_${password}_salt"
    }

    suspend fun registerUser(name: String, email: String, phone: String, address: String, password: String): UserEntity? {
        return withContext(Dispatchers.IO) {
            if (userDao.getUserByEmail(email) != null) {
                return@withContext null
            }
            val newUser = UserEntity(
                name = name,
                email = email,
                phoneNumber = phone,
                passHash = hashPassword(password),
                address = address, // Tambahkan alamat
                logo = null,
                footerNote = "Terima Kasih"
            )
            val userId = userDao.insertUser(newUser)

            // Ambil user yang baru dibuat dengan ID yang benar
            val createdUser = userDao.getUserByEmail(email)
            return@withContext createdUser
        }
    }

    // FUNGSI BARU: Auto login setelah register
    suspend fun autoLoginAfterRegister(user: UserEntity): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                sharedPreferences.edit().putInt(KEY_LOGGED_IN_USER_ID, user.id).apply()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun updateOutletProfile(id: Int, name: String, phone: String, address: String, footer: String, logo: String?) {
        withContext(Dispatchers.IO) {
            val currentUser = userDao.getUserByIdSuspend(id)
            if (currentUser != null) {
                val updatedUser = currentUser.copy(
                    name = name,
                    phoneNumber = phone,
                    address = address,
                    logo = logo,
                    footerNote = footer
                )
                userDao.updateUser(updatedUser)
            }
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUserByEmail(email) ?: return@withContext false
            val isPasswordCorrect = user.passHash == hashPassword(password)
            if (isPasswordCorrect) {
                sharedPreferences.edit().putInt(KEY_LOGGED_IN_USER_ID, user.id).apply()
            }
            return@withContext isPasswordCorrect
        }
    }

    fun getLoggedInUserId(): Int {
        return sharedPreferences.getInt(KEY_LOGGED_IN_USER_ID, -1)
    }

    fun logout() {
        try {
            // Gun commit() bukan apply() untuk memastikan langsung tersimpan
            sharedPreferences.edit()
                .remove(KEY_LOGGED_IN_USER_ID)
                .commit() // ⚠️ Gunakan commit() untuk synchronous operation
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error during logout: ${e.message}")
        }
    }

    // PERBAIKAN: Tambahkan fungsi untuk memastikan session clear
    fun forceClearSession() {
        sharedPreferences.edit()
            .remove(KEY_LOGGED_IN_USER_ID)
            .apply()
    }

    // === FUNGSI YANG DIPERBAIKI ===

    // Fungsi untuk mendapatkan data user lengkap (suspend)
    suspend fun getCurrentUser(): UserEntity? {
        return withContext(Dispatchers.IO) {
            val userId = getLoggedInUserId()
            if (userId == -1) {
                null
            } else {
                userDao.getUserByIdSuspend(userId)
            }
        }
    }

    // FUNGSI BARU: Get user sebagai Flow untuk ViewModel
    fun getCurrentUserFlow(): Flow<UserEntity?> {
        val userId = getLoggedInUserId()
        return if (userId == -1) {
            flowOf(null)
        } else {
            userDao.getUserById(userId)
        }
    }
}