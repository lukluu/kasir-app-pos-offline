package com.example.kasirku.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.kasirku.data.local.converters.DateConverter
import com.example.kasirku.data.local.dao.CategoryDao
import com.example.kasirku.data.local.dao.CustomerDao
import com.example.kasirku.data.local.dao.OrderDao
import com.example.kasirku.data.local.dao.PaymentMethodDao
import com.example.kasirku.data.local.dao.ProductDao
import com.example.kasirku.data.local.dao.UserDao
import com.example.kasirku.data.local.entity.CategoryEntity
import com.example.kasirku.data.local.entity.CustomerEntity
import com.example.kasirku.data.local.entity.OrderEntity
import com.example.kasirku.data.local.entity.OrderItemEntity
import com.example.kasirku.data.local.entity.PaymentMethodEntity
import com.example.kasirku.data.local.entity.ProductEntity
import com.example.kasirku.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        CustomerEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentMethodEntity::class
    ],
    version = 17,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentMethodDao(): PaymentMethodDao
}