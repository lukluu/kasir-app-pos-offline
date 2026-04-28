package com.example.kasirku.data.repository

import com.example.kasirku.data.local.dao.CategoryDao
import com.example.kasirku.data.local.entity.CategoryEntity
import com.example.kasirku.data.local.entity.PaymentMethodEntity
import com.example.kasirku.domain.model.Category
import com.example.kasirku.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun getCategories(): Flow<List<Category>> {
        return dao.getAllCategories().map { entities ->
            entities.map { it.toCategory() }
        }
    }

    override suspend fun addCategory(category: Category) {
        dao.insert(category.toCategoryEntity())
    }

    // === PERBAIKAN DI SINI ===
    override suspend fun deleteCategory(category: Category) {
        dao.softDelete(category.id)
    }

    override suspend fun updateCategory(category: Category) {
        dao.update(category.toCategoryEntity())
    }


    override suspend fun initDefaultCategory() {
        if (dao.getCount() == 0) {
            val defaults = listOf(
                "Makanan", "Minuman"
            ).map { CategoryEntity(name = it) }
            dao.insertAll(defaults)
        }
    }
}

// Mapper
private fun CategoryEntity.toCategory(): Category {
    return Category(id = id, name = name)
}

private fun Category.toCategoryEntity(): CategoryEntity {
    // Pastikan isDeleted defaultnya false
    return CategoryEntity(id = id, name = name, isDeleted = false)
}