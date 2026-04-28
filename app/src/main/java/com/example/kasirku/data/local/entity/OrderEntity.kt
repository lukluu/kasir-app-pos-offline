package com.example.kasirku.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

// 1. Tambahkan status CANCELLED
enum class PaymentStatus {
    PAID, UNPAID, CANCELLED
}

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val createdAt: Date,
    val totalPrice: Double,
    val subtotal: Double,
    val discount: Double = 0.0,
    val customerId: Int? = null,
    val amountPaid: Double = 0.0,
    val note: String? = null,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val paymentMethod: String = "Tunai",
    // 2. Tambahkan kolom baru untuk menyimpan alasan
    val cancelReason: String? = null
)