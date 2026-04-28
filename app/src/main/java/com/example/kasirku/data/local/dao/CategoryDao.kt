package com.example.kasirku.data.local.dao

import androidx.room.*
import com.example.kasirku.data.local.entity.CategoryEntity
import com.example.kasirku.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)

    // UPDATE: Ganti DELETE fisik dengan SOFT DELETE
    @Query("UPDATE categories SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Int)

    // UPDATE: Filter hanya yang isDeleted = 0
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Int): Flow<CategoryEntity?>
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(category: List<CategoryEntity>)
}