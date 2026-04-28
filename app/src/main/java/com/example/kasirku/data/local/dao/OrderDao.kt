package com.example.kasirku.data.local.dao

import androidx.room.*
import com.example.kasirku.data.local.entity.*
import kotlinx.coroutines.flow.Flow


data class OrderWithCustomer(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: CustomerEntity?
)

data class OrderItemWithProduct(
    @Embedded val item: OrderItemEntity,
    @Relation(
        parentColumn = "productId",
        entityColumn = "id"
    )
    val product: ProductEntity
)


@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun createOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        val orderId = insertOrder(order)
        val itemsWithOrderId = items.map { it.copy(orderId = orderId) }
        insertOrderItems(itemsWithOrderId)
    }

    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersWithCustomer(): Flow<List<OrderWithCustomer>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderWithCustomerById(orderId: Long): Flow<OrderWithCustomer?>

    @Transaction
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsWithProduct(orderId: Long): List<OrderItemWithProduct>

    @Query("UPDATE orders SET paymentStatus = :status WHERE id = :orderId")
    suspend fun updatePaymentStatus(orderId: Long, status: PaymentStatus)

    @Query("UPDATE orders SET paymentStatus = 'PAID', paymentMethod = :method WHERE id = :orderId")
    suspend fun updatePaymentStatusToPaid(orderId: Long, method: String)

    @Query("UPDATE orders SET paymentStatus = 'PAID', paymentMethod = :method, amountPaid = :amount WHERE id = :orderId")
    suspend fun updatePaymentStatusToPaid(orderId: Long, method: String, amount: Double)
    @Query("UPDATE orders SET paymentStatus = 'CANCELLED', cancelReason = :reason WHERE id = :orderId")
    suspend fun cancelOrderWithReason(orderId: Long, reason: String)


    @Update
    suspend fun updateOrder(order: OrderEntity)

    // Hapus semua item lama dari order ini (sebelum insert item baru hasil edit)
    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: Long)



    @Transaction
    @Query("""
        SELECT * FROM orders 
        WHERE createdAt BETWEEN :startDate AND :endDate 
        AND paymentStatus = :status
        ORDER BY createdAt DESC
    """)
    fun getOrdersByDateAndStatus(startDate: Long, endDate: Long, status: PaymentStatus): Flow<List<OrderWithCustomer>>

    // 2. Laporan Produk Terlaris (Perlu join rumit, kita olah di Repository/ViewModel saja agar simpel)
    // Kita gunakan query getOrderItems yang sudah ada, nanti difilter by date di logic kotlin.
    @Transaction
    @Query("SELECT * FROM order_items JOIN orders ON order_items.orderId = orders.id WHERE orders.createdAt BETWEEN :startDate AND :endDate AND orders.paymentStatus = 'PAID'")
    suspend fun getSoldItemsByDate(startDate: Long, endDate: Long): List<OrderItemWithProduct>

}