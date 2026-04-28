package com.example.kasirku.data.repository

import com.example.kasirku.data.local.dao.ProductDao
import com.example.kasirku.data.local.entity.ProductEntity
import com.example.kasirku.domain.model.Product
import com.example.kasirku.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val dao: ProductDao
) : ProductRepository {

    // SEMUA FUNGSI INI SUDAH BENAR
    override fun getAllProducts(): Flow<List<Product>> {
        return dao.getAllProducts().map { entities ->
            entities.map { it.toProduct() }
        }
    }

    override fun getProductById(productId: Int): Flow<Product?> {
        return dao.getProductById(productId).map { it?.toProduct() }
    }

    override fun getProductsByCategory(categoryId: Int): Flow<List<Product>> {
        return dao.getProductsByCategory(categoryId).map { entities ->
            entities.map { it.toProduct() }
        }
    }

    override suspend fun addProduct(product: Product) {
        dao.insert(product.toProductEntity())
    }

    // === PERBAIKAN DI SINI ===
    override suspend fun updateProduct(product: Product) {
        // Gunakan fungsi @Update dari DAO, BUKAN @Insert
        dao.update(product.toProductEntity())
    }

//    override suspend fun deleteProduct(product: Product) {
//        dao.delete(product.toProductEntity())
//    }
    override suspend fun deleteProduct(product: Product) {
        // PANGGIL FUNGSI SOFT DELETE
        dao.softDeleteProduct(product.id)
    }
}

// ================== PERBAIKAN DI SINI ==================

// Fungsi mapping dari Entity (Database) ke Model (Domain/UI)
private fun ProductEntity.toProduct(): Product {
    return Product(
        id = id,
        name = name,
        price = price,
        stock = stock,
        categoryId = categoryId,
        isUnlimited = isUnlimited,
        imageUri = imageUri
    )
}

// Fungsi mapping dari Model (Domain/UI) ke Entity (Database)
private fun Product.toProductEntity(): ProductEntity {
    return ProductEntity(
        id = id,
        name = name,
        price = price,
        stock = stock,
        categoryId = categoryId,
        isUnlimited = isUnlimited,
        imageUri = imageUri
    )
}