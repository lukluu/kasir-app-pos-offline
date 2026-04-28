package com.example.kasirku.domain.repository

import com.example.kasirku.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    fun getProductById(productId: Int): Flow<Product?> // <-- TAMBAHAN
    fun getProductsByCategory(categoryId: Int): Flow<List<Product>>
    suspend fun addProduct(product: Product)
    suspend fun updateProduct(product: Product)

    suspend fun deleteProduct(product: Product)
}