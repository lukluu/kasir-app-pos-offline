package com.example.kasirku.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.kasirku.data.local.AppDatabase
import com.example.kasirku.data.repository.AuthRepository
import com.example.kasirku.data.repository.CategoryRepositoryImpl
import com.example.kasirku.data.repository.CustomerRepositoryImpl
import com.example.kasirku.data.repository.OrderRepositoryImpl
import com.example.kasirku.data.repository.PaymentMethodRepositoryImpl
import com.example.kasirku.data.repository.ProductRepositoryImpl
import com.example.kasirku.domain.repository.CategoryRepository
import com.example.kasirku.domain.repository.CustomerRepository
import com.example.kasirku.domain.repository.OrderRepository
import com.example.kasirku.domain.repository.PaymentMethodRepository
import com.example.kasirku.domain.repository.ProductRepository
import com.example.kasirku.ui.utils.OrderCounter
import com.example.kasirku.ui.utils.PrinterHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_session", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kasirku_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(db: AppDatabase, sharedPreferences: SharedPreferences): AuthRepository {
        return AuthRepository(db.userDao(), sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(db: AppDatabase): CategoryRepository {
        return CategoryRepositoryImpl(db.categoryDao())
    }

    @Provides
    @Singleton
    fun provideProductRepository(db: AppDatabase): ProductRepository {
        return ProductRepositoryImpl(db.productDao())
    }

    @Provides
    @Singleton
    fun provideCustomerRepository(db: AppDatabase): CustomerRepository {
        return CustomerRepositoryImpl(db.customerDao())
    }

    // === PERBAIKAN DI SINI ===
    @Provides
    @Singleton
    fun provideOrderRepository(
        db: AppDatabase,
        orderCounter: OrderCounter
    ): OrderRepository {
        return OrderRepositoryImpl(
            orderDao = db.orderDao(),
            productDao = db.productDao(),
            orderCounter = orderCounter
        )
    }

    @Provides
    @Singleton
    fun provideOrderCounter(@ApplicationContext context: Context): OrderCounter {
        return OrderCounter(context)
    }
    @Provides
    @Singleton
    fun providePaymentMethodRepository(db: AppDatabase): PaymentMethodRepository {
        return PaymentMethodRepositoryImpl(db.paymentMethodDao())
    }


    @Provides
    @Singleton
    fun providePrinterHelper(@ApplicationContext context: Context): PrinterHelper {
        return PrinterHelper(context)
    }

}