package com.example.kasirku.domain.model

data class Category(
    val id: Int,
    val name: String,
    val isDeleted: Boolean = false
)