package com.example.kasirku.data.repository

import com.example.kasirku.data.local.dao.CustomerDao
import com.example.kasirku.data.local.entity.CustomerEntity
import com.example.kasirku.domain.model.Customer
import com.example.kasirku.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val dao: CustomerDao
) : CustomerRepository {
    override fun getAllCustomers(): Flow<List<Customer>> {
        return dao.getAllCustomers().map { entities ->
            entities.map { it.toCustomer() }
        }
    }

    override suspend fun addOrUpdateCustomer(customer: Customer) {
        val entity = customer.toCustomerEntity()
        if (customer.id == 0) {
            dao.insert(entity)
        } else {
            dao.update(entity) // Panggil update khusus
        }
    }

    override suspend fun deleteCustomer(customer: Customer) {
        dao.delete(customer.toCustomerEntity())
    }
}

private fun CustomerEntity.toCustomer(): Customer {
    return Customer(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        address = address
    )
}

private fun Customer.toCustomerEntity(): CustomerEntity {
    return CustomerEntity(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        address = address
    )
}