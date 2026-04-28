package com.example.kasirku.data.local.dao

import androidx.room.*
import com.example.kasirku.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ganti REPLACE jadi IGNORE agar aman
    suspend fun insert(customer: CustomerEntity): Long

    @Update // Gunakan ini khusus untuk edit
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>
}