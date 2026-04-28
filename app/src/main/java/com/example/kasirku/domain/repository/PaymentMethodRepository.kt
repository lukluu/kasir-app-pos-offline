package com.example.kasirku.domain.repository

import com.example.kasirku.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    fun getAllPaymentMethods(): Flow<List<PaymentMethodEntity>>
    suspend fun addPaymentMethod(name: String)
    suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity)
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity)
    suspend fun initDefaultPaymentMethods()
}