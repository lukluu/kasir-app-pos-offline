package com.example.kasirku.ui.screen.laporan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.domain.model.Order
import com.example.kasirku.domain.model.Product
import com.example.kasirku.domain.repository.OrderRepository
import com.example.kasirku.domain.repository.PaymentMethodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ReportUiState(
    val reportType: String = "",
    // PERBAIKAN: Set startDate ke 0 (1970) agar mencakup semua waktu awal
    val startDate: Long = 0L,
    val endDate: Long = getEndOfDay(),
    val orders: List<Order> = emptyList(),
    val bestSellers: List<Pair<Product, Int>> = emptyList(),
    val totalAmount: Double = 0.0,
    val totalCount: Int = 0,
    val selectedFilterStatus: String = "Semua",
    val selectedPaymentMethodFilter: String = "Semua",
    val availablePaymentMethods: List<String> = listOf("Semua", "Tunai", "Non-Tunai")
)

// Helper Date
fun getEndOfDay(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val repository: OrderRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val type = savedStateHandle.get<String>("type") ?: "orders"
    private val _uiState = MutableStateFlow(ReportUiState(reportType = type))
    val uiState = _uiState.asStateFlow()

    init {
        loadPaymentMethods()
        loadData() // Ini akan meload data dari 1970 - Sekarang (Semua Waktu)
    }

    // ... (loadPaymentMethods, onDateRangeChanged, onFilterStatusChanged, dll SAMA SEPERTI SEBELUMNYA) ...
    private fun loadPaymentMethods() {
        viewModelScope.launch {
            paymentMethodRepository.getAllPaymentMethods().collect { methods ->
                val names = listOf("Semua", "Tunai", "Non-Tunai") +  methods.map { it.name }.filter { it != "Tunai" }
                _uiState.update { it.copy(availablePaymentMethods = names.distinct()) }
            }
        }
    }

    fun onDateRangeChanged(start: Long, end: Long) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
        loadData()
    }

    fun onFilterStatusChanged(status: String) {
        _uiState.update { it.copy(selectedFilterStatus = status) }
        loadData()
    }

    fun onFilterPaymentMethodChanged(method: String) {
        _uiState.update { it.copy(selectedPaymentMethodFilter = method) }
        loadData()
    }

    fun loadData() {
        val start = Date(_uiState.value.startDate)
        val end = Date(_uiState.value.endDate)
        val type = _uiState.value.reportType

        viewModelScope.launch {
            if (type == "products") {
                val items = repository.getBestSellingProducts(start, end)
                _uiState.update {
                    it.copy(
                        bestSellers = items,
                        orders = emptyList(),
                        totalCount = items.sumOf { pair -> pair.second },
                        totalAmount = 0.0
                    )
                }
            } else {
                repository.getAllOrders().collect { allOrders ->
                    val filtered = allOrders.filter { order ->
                        val isDateMatch = order.createdAt.time in start.time..end.time

                        when (type) {
                            "cancelled" -> isDateMatch && order.paymentStatus == "CANCELLED"
                            "cashless" -> {
                                val method = order.paymentMethod ?: "Tunai"
                                isDateMatch && order.paymentStatus == "PAID" && !method.equals("Tunai", ignoreCase = true)
                            }
                            else -> {
                                val currentStatusFilter = _uiState.value.selectedFilterStatus
                                val currentPaymentFilter = _uiState.value.selectedPaymentMethodFilter

                                val isStatusMatch = when (currentStatusFilter) {
                                    "Dibayar" -> order.paymentStatus == "PAID"
                                    "Belum Dibayar" -> order.paymentStatus == "UNPAID"
                                    "Batal" -> order.paymentStatus == "CANCELLED"
                                    else -> true
                                }
                                val orderMethod = order.paymentMethod ?: "Tunai"
                                val isMethodMatch = when (currentPaymentFilter) {
                                    "Semua" -> true
                                    "Tunai" -> orderMethod.equals("Tunai", ignoreCase = true)
                                    "Non-Tunai" -> !orderMethod.equals("Tunai", ignoreCase = true)
                                    else -> orderMethod.equals(currentPaymentFilter, ignoreCase = true)
                                }
                                isDateMatch && isStatusMatch && isMethodMatch
                            }
                        }
                    }

                    _uiState.update {
                        it.copy(
                            orders = filtered,
                            bestSellers = emptyList(),
                            totalAmount = filtered.filter { it.paymentStatus == "PAID" }.sumOf { o -> o.totalPrice },
                            totalCount = filtered.size
                        )
                    }
                }
            }
        }
    }
}