package com.example.kasirku.domain.model

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val stock: Int,
    val isUnlimited: Boolean = true,
    val categoryId: Int,
    val imageUri: String? = null,
    val isDeleted: Boolean = false
)
