package com.example.kasirku.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "order_items",
    primaryKeys = ["orderId", "productId"], // Kunci komposit
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE // Jika order dihapus, itemnya ikut terhapus
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT // Jangan biarkan produk dihapus jika masih ada di order
        )
    ]
)
data class OrderItemEntity(
    val orderId: Long,
    val productId: Int,
    val quantity: Int,
    val priceAtSale: Double // Menyimpan harga produk pada saat penjualan
)