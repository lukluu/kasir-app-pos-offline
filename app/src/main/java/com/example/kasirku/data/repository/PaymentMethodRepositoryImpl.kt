package com.example.kasirku.data.repository

import com.example.kasirku.data.local.dao.PaymentMethodDao
import com.example.kasirku.data.local.entity.PaymentMethodEntity
import com.example.kasirku.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentMethodRepositoryImpl @Inject constructor(
    private val dao: PaymentMethodDao
) : PaymentMethodRepository {

    override fun getAllPaymentMethods(): Flow<List<PaymentMethodEntity>> {
        return dao.getAllPaymentMethods()
    }

    override suspend fun addPaymentMethod(name: String) {
        dao.insertPaymentMethod(PaymentMethodEntity(name = name))
    }

    override suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity) {
        dao.updatePaymentMethod(paymentMethod)
    }

    override suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity) {
        dao.deletePaymentMethod(paymentMethod)
    }

    override suspend fun initDefaultPaymentMethods() {
        if (dao.getCount() == 0) {
            val defaults = listOf(
                "Tunai", "GoPay", "Dana", "ShopeePay", "QRIS", "LinkAja", "OVO", "Transfer"
            ).map { PaymentMethodEntity(name = it) }
            dao.insertAll(defaults)
        }
    }
}