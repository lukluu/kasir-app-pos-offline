package com.example.kasirku.domain.model

import java.util.Date


data class Order(
    val id: Long,
    val orderNumber: String,
    val createdAt: Date,
    val totalPrice: Double,
    val items: List<OrderItem>,
    val customer: Customer?,
    val note: String?,
    val discount: Double,
    val amountPaid: Double = 0.0,
    val paymentStatus: String,
    val cancelReason: String?,
    val paymentMethod: String
)

data class OrderItem(
    val product: Product,
    val quantity: Int,
    val priceAtSale: Double
)