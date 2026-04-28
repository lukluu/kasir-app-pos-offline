package com.example.kasirku.domain.repository

import com.example.kasirku.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun getAllCustomers(): Flow<List<Customer>>
    suspend fun addOrUpdateCustomer(customer: Customer)
    suspend fun deleteCustomer(customer: Customer)
}