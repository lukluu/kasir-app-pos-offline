package com.example.kasirku.domain.model

data class Customer(
    val id: Int,
    val name: String,
    val phoneNumber: String?,
    val address: String?
)