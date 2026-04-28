package com.example.kasirku.data.local.dao

import androidx.room.*
import com.example.kasirku.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Update
    suspend fun update(product: ProductEntity)

//    @Delete
//    suspend fun delete(product: ProductEntity)

    // Fungsi ini sudah benar
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    // Tambahkan WHERE isDeleted = 0 juga di sini
    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isDeleted = 0 ORDER BY name ASC")
    fun getProductsByCategory(categoryId: Int): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    fun getProductById(productId: Int): Flow<ProductEntity?>

    // Soft Delete (Sudah Benar)
    @Query("UPDATE products SET isDeleted = 1 WHERE id = :productId")
    suspend fun softDeleteProduct(productId: Int)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId AND isUnlimited = 0")
    suspend fun decreaseStock(productId: Int, quantity: Int)

    // FUNGSI BARU: Kembalikan Stok (Saat Pesanan Dibatalkan)
    @Query("UPDATE products SET stock = stock + :quantity WHERE id = :productId AND isUnlimited = 0")
    suspend fun increaseStock(productId: Int, quantity: Int)
}