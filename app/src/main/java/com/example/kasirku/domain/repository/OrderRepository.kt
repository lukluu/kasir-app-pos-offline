package com.example.kasirku.domain.repository

import com.example.kasirku.domain.model.Customer
import com.example.kasirku.domain.model.Order
import com.example.kasirku.domain.model.Product
import kotlinx.coroutines.flow.Flow
import java.util.Date

// com/example/kasirku/domain/repository/OrderRepository.kt
interface OrderRepository {
    suspend fun saveOrder(cart: Map<Product, Int>, customer: Customer?, note: String?, discount: Double, isPaid: Boolean, paymentMethod: String, amountPaid: Double): Result<Long>
    suspend fun cancelOrder(id: Long, reason: String)

    suspend fun updateOrder(orderId: Long, cart: Map<Product, Int>, customer: Customer?, note: String?, discount: Double, isPaid: Boolean, paymentMethod: String, amountPaid: Double): Result<Long>

    fun getReportOrders(startDate: Date, endDate: Date, status: String): Flow<List<Order>>
    suspend fun getBestSellingProducts(startDate: Date, endDate: Date): List<Pair<Product, Int>> // Product & Qty
    // Tambahan:
    fun getAllOrders(): Flow<List<Order>>
    fun getOrderById(id: Long): Flow<Order?>
    suspend fun payOrder(id: Long)
    suspend fun resetOrderSequence()
    // Update signature
    suspend fun payOrderDetail(id: Long, paymentMethod: String, amountPaid: Double)

    suspend fun getOrderDetailsForReorder(orderId: Long): Map<Product, Int>
}