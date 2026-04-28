package com.example.kasirku.data.repository

import com.example.kasirku.data.local.dao.OrderDao
import com.example.kasirku.data.local.dao.OrderWithCustomer
import com.example.kasirku.data.local.dao.ProductDao
import com.example.kasirku.data.local.entity.OrderEntity
import com.example.kasirku.data.local.entity.OrderItemEntity
import com.example.kasirku.data.local.entity.PaymentStatus
import com.example.kasirku.domain.model.Customer
import com.example.kasirku.domain.model.Order
import com.example.kasirku.domain.model.OrderItem
import com.example.kasirku.domain.model.Product
import com.example.kasirku.domain.repository.OrderRepository
import com.example.kasirku.ui.utils.OrderCounter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao,
    private val productDao: ProductDao, // Inject ProductDao untuk update stok
    private val orderCounter: OrderCounter
) : OrderRepository {

    // --- SAVE ORDER (BARU) ---
    override suspend fun saveOrder(
        cart: Map<Product, Int>,
        customer: Customer?,
        note: String?,
        discount: Double,
        isPaid: Boolean,
        paymentMethod: String,
        amountPaid: Double,
    ): Result<Long> {
        return try {
            val now = Date()
            val subtotal = cart.entries.sumOf { (product, quantity) -> product.price * quantity }
            val total = subtotal - discount

            val orderEntity = OrderEntity(
                orderNumber = generateOrderNumber(now),
                createdAt = now,
                subtotal = subtotal,
                totalPrice = total,
                discount = discount,
                amountPaid = amountPaid,
                customerId = customer?.id,
                note = note?.ifBlank { null },
                paymentMethod = paymentMethod,
                paymentStatus = if (isPaid) PaymentStatus.PAID else PaymentStatus.UNPAID
            )

            val orderItemEntities = cart.map { (product, quantity) ->
                OrderItemEntity(
                    orderId = 0,
                    productId = product.id,
                    quantity = quantity,
                    priceAtSale = product.price
                )
            }

            // 1. Simpan Order & Items
            val newOrderId = orderDao.insertOrder(orderEntity)
            val itemsWithOrderId = orderItemEntities.map { it.copy(orderId = newOrderId) }
            orderDao.insertOrderItems(itemsWithOrderId)

            // 2. KURANGI STOK (Hanya jika tidak unlimited)
            cart.forEach { (product, quantity) ->
                if (!product.isUnlimited) {
                    productDao.decreaseStock(product.id, quantity)
                }
            }

            Result.success(newOrderId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }


    // --- CANCEL ORDER ---
    override suspend fun cancelOrder(id: Long, reason: String) {
        // 1. Ambil item yang ada di pesanan ini untuk kembalikan stok
        val items = orderDao.getOrderItemsWithProduct(id)

        // 2. Update status jadi CANCELLED
        orderDao.cancelOrderWithReason(id, reason)

        // 3. KEMBALIKAN STOK
        items.forEach { itemRelation ->
            val product = itemRelation.product
            val qty = itemRelation.item.quantity

            // Cek di entity product aslinya apakah unlimited (dari relasi)
            if (!product.isUnlimited) {
                productDao.increaseStock(product.id, qty)
            }
        }
    }


    // --- UPDATE ORDER ---
    override suspend fun updateOrder(
        orderId: Long,
        cart: Map<Product, Int>,
        customer: Customer?,
        note: String?,
        discount: Double,
        isPaid: Boolean,
        paymentMethod: String,
        amountPaid: Double
    ): Result<Long> {
        return try {
            // 1. KEMBALIKAN STOK LAMA DULU
            val oldItems = orderDao.getOrderItemsWithProduct(orderId)
            oldItems.forEach { itemRel ->
                if (!itemRel.product.isUnlimited) {
                    productDao.increaseStock(itemRel.product.id, itemRel.item.quantity)
                }
            }

            // 2. Update Data Order Utama
            val existingOrder = orderDao.getOrderWithCustomerById(orderId).first()?.order
                ?: return Result.failure(Exception("Order not found"))

            // Hitung ulang total
            val subtotal = cart.entries.sumOf { (product, quantity) -> product.price * quantity }
            val total = subtotal - discount

            val updatedOrderEntity = existingOrder.copy(
                subtotal = subtotal,
                totalPrice = total,
                discount = discount,
                customerId = customer?.id,
                amountPaid = amountPaid,
                note = note?.ifBlank { null },
                paymentMethod = paymentMethod,
                paymentStatus = if (isPaid) PaymentStatus.PAID else PaymentStatus.UNPAID
            )
            orderDao.updateOrder(updatedOrderEntity)

            // 3. Hapus Item Lama & Masukkan Item Baru
            orderDao.deleteOrderItems(orderId)

            val newItems = cart.map { (product, quantity) ->
                OrderItemEntity(
                    orderId = orderId,
                    productId = product.id,
                    quantity = quantity,
                    priceAtSale = product.price
                )
            }
            orderDao.insertOrderItems(newItems)

            // 4. KURANGI STOK UNTUK ITEM BARU
            cart.forEach { (product, quantity) ->
                if (!product.isUnlimited) {
                    productDao.decreaseStock(product.id, quantity)
                }
            }

            Result.success(orderId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // --- GETTERS & PAY ---

    override fun getAllOrders(): Flow<List<Order>> {
        return orderDao.getAllOrdersWithCustomer().map { list ->
            list.map { mapToDomain(it) }
        }
    }

    override fun getOrderById(id: Long): Flow<Order?> {
        return orderDao.getOrderWithCustomerById(id).map { it?.let { mapToDomain(it) } }
    }

    // Hapus fungsi payOrder(id) lama yang hanya 1 parameter agar tidak konflik
    // override suspend fun payOrder(id: Long) { ... }

    override suspend fun payOrderDetail(id: Long, paymentMethod: String, amountPaid: Double) {
        orderDao.updatePaymentStatusToPaid(id, paymentMethod, amountPaid)
    }

    override suspend fun getOrderDetailsForReorder(orderId: Long): Map<Product, Int> {
        // 1. Ambil Order lengkap (Domain Model)
        val order = getOrderById(orderId).first()

        // 2. Jika ada, ekstrak item dan konversi ke Map<Product, Int>
        return order?.items?.associate { orderItem ->
            // OrderItem memiliki properti Product domain model dan quantity
            orderItem.product to orderItem.quantity
        } ?: emptyMap()
    }

    override fun getReportOrders(startDate: Date, endDate: Date, status: String): Flow<List<Order>> {
        val statusEnum = try { PaymentStatus.valueOf(status) } catch (e: Exception) { PaymentStatus.PAID }
        return orderDao.getOrdersByDateAndStatus(startDate.time, endDate.time, statusEnum).map { list ->
            list.map { mapToDomain(it) }
        }
    }

    override suspend fun getBestSellingProducts(startDate: Date, endDate: Date): List<Pair<Product, Int>> {
        val items = orderDao.getSoldItemsByDate(startDate.time, endDate.time)
        val groupedMap = items.groupBy { it.product.id }

        return groupedMap.map { (_, itemList) ->
            val productEntity = itemList.first().product
            val totalQty = itemList.sumOf { it.item.quantity }

            // Manual mapping dari Entity ke Domain Product
            val domainProduct = Product(
                id = productEntity.id,
                name = productEntity.name,
                price = productEntity.price,
                stock = productEntity.stock,
                isUnlimited = productEntity.isUnlimited,
                categoryId = productEntity.categoryId,
                imageUri = productEntity.imageUri
            )
            Pair(domainProduct, totalQty)
        }.sortedByDescending { it.second }
    }

    override suspend fun resetOrderSequence() {
        orderCounter.resetSequence()
    }

    // --- HELPERS ---

    private fun generateOrderNumber(date: Date): String {
        val dateFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())
        val sequence = orderCounter.getNextSequence()
        val sequenceStr = sequence.toString().padStart(4, '0')
        return "ORD/${dateFormat.format(date)}/$sequenceStr"
    }

    private suspend fun mapToDomain(relation: OrderWithCustomer): Order {
        // Ambil item produk
        val itemsEntities = orderDao.getOrderItemsWithProduct(relation.order.id)

        val domainItems = itemsEntities.map {
            OrderItem(
                product = Product(
                    id = it.product.id,
                    name = it.product.name,
                    price = it.product.price,
                    stock = it.product.stock,
                    isUnlimited = it.product.isUnlimited,
                    categoryId = it.product.categoryId,
                    imageUri = it.product.imageUri,

                ),
                quantity = it.item.quantity,
                priceAtSale = it.item.priceAtSale
            )
        }

        return Order(
            id = relation.order.id,
            orderNumber = relation.order.orderNumber,
            createdAt = relation.order.createdAt,
            totalPrice = relation.order.totalPrice,
            items = domainItems,
            customer = relation.customer?.let {
                Customer(it.id, it.name, it.phoneNumber, it.address)
            },
            note = relation.order.note,
            paymentMethod = relation.order.paymentMethod,
            discount = relation.order.discount,
            amountPaid = relation.order.amountPaid,
            paymentStatus = relation.order.paymentStatus.name,
            cancelReason = relation.order.cancelReason
        )
    }

    override suspend fun payOrder(id: Long) {
        // Kirim Enum PaymentStatus.PAID
        orderDao.updatePaymentStatus(id, PaymentStatus.PAID)
    }

}