package com.example.kasirku.ui.screen.pesanan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.domain.model.Order
import com.example.kasirku.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val repository: OrderRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow("Semua")

    val uiState = combine(
        repository.getAllOrders(),
        _searchQuery,
        _filterType
    ) { orders, query, filter ->
        var result = orders

        // 1. Filter Tab
        when (filter) {
            "Dibayar" -> result = result.filter { it.paymentStatus == "PAID" }
            "Belum Dibayar" -> result = result.filter { it.paymentStatus == "UNPAID" }
            "Dibatalkan" -> result = result.filter { it.paymentStatus == "CANCELLED" }
        }

        // 2. Filter Search
        if (query.isNotBlank()) {
            result = result.filter {
                it.orderNumber.contains(query, ignoreCase = true) ||
                        (it.customer?.name?.contains(query, ignoreCase = true) == true)
            }
        }

        // 3. Hitung Total (PERBAIKAN DI SINI)
        // Hanya hitung jika status BUKAN 'CANCELLED'
        val calculatedTotal = result
            .filter { it.paymentStatus != "CANCELLED" }
            .sumOf { it.totalPrice }

        OrderListUiState(
            orders = result,
            totalRevenue = calculatedTotal, // Gunakan total yang sudah difilter
            searchQuery = query,
            selectedFilter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrderListUiState())

    fun onSearchChange(query: String) { _searchQuery.value = query }
    fun onFilterChange(filter: String) { _filterType.value = filter }
}

data class OrderListUiState(
    val orders: List<Order> = emptyList(),
    val totalRevenue: Double = 0.0,
    val searchQuery: String = "",
    val selectedFilter: String = "Semua"
)