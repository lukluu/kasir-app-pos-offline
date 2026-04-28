package com.example.kasirku.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// Enum untuk Pilihan Grafik
enum class ChartFilter { DAILY, MONTHLY }

data class DashboardState(
    val todayRevenue: String = "Rp 0",
    val todayOrders: Int = 0,
    val monthRevenue: String = "Rp 0",
    val monthOrders: Int = 0,
    val isLoading: Boolean = true,

    // Data Grafik
    val selectedChartFilter: ChartFilter = ChartFilter.DAILY,
    val chartData: List<Pair<String, Double>> = emptyList(), // Label (X), Value (Y)
    val chartMaxY: Double = 0.0 // Nilai tertinggi untuk skala Y
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    // Cache data mentah agar tidak query DB terus saat ganti tab
    private var allOrdersCache: List<com.example.kasirku.domain.model.Order> = emptyList()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val orders = orderRepository.getAllOrders().first()
                allOrdersCache = orders
                calculateStatistics(orders)
                updateChartData(ChartFilter.DAILY)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Hitung Statistik Kartu Atas
    private fun calculateStatistics(orders: List<com.example.kasirku.domain.model.Order>) {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        val currentMonthStr = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(today)
        val currentDayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(today)

        // Filter data yang valid (Paid) - Opsional, sesuaikan kebutuhan bisnis
        val validOrders = orders.filter { it.paymentStatus == "PAID" }

        // Harian
        val todayOrdersList = validOrders.filter {
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(it.createdAt) == currentDayStr
        }
        val todayRevenue = todayOrdersList.sumOf { it.totalPrice }

        // Bulanan
        val monthOrdersList = validOrders.filter {
            SimpleDateFormat("yyyyMM", Locale.getDefault()).format(it.createdAt) == currentMonthStr
        }
        val monthRevenue = monthOrdersList.sumOf { it.totalPrice }

        _uiState.update {
            it.copy(
                todayRevenue = formatCurrency(todayRevenue),
                todayOrders = todayOrdersList.size,
                monthRevenue = formatCurrency(monthRevenue),
                monthOrders = monthOrdersList.size,
                isLoading = false
            )
        }
    }

    // Fungsi Ganti Filter (Dipanggil dari UI)
    fun onChartFilterChanged(filter: ChartFilter) {
        updateChartData(filter)
    }

    // Logika Menyiapkan Data Grafik
    private fun updateChartData(filter: ChartFilter) {
        val data = if (filter == ChartFilter.DAILY) {
            // Ambil 7 Hari Terakhir
            (0..6).map { daysAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                val dateToCheck = cal.time
                val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(dateToCheck)

                // Label X: "12 Nov"
                val label = SimpleDateFormat("d MMM", Locale("id", "ID")).format(dateToCheck)

                // Total Y
                val total = allOrdersCache
                    .filter { it.paymentStatus == "PAID" } // Hanya yang lunas
                    .filter { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(it.createdAt) == dateStr }
                    .sumOf { it.totalPrice }

                Pair(label, total)
            }.reversed()
        } else {
            // Ambil 6 Bulan Terakhir
            (0..5).map { monthsAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -monthsAgo)
                val dateToCheck = cal.time
                val monthStr = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(dateToCheck)

                // Label X: "Nov"
                val label = SimpleDateFormat("MMM", Locale("id", "ID")).format(dateToCheck)

                val total = allOrdersCache
                    .filter { it.paymentStatus == "PAID" }
                    .filter { SimpleDateFormat("yyyyMM", Locale.getDefault()).format(it.createdAt) == monthStr }
                    .sumOf { it.totalPrice }

                Pair(label, total)
            }.reversed()
        }

        // Cari nilai tertinggi untuk skala Y (tambah buffer 20% agar grafik tidak mentok atas)
        val maxVal = (data.maxOfOrNull { it.second } ?: 0.0) * 1.2
        // Jika data 0 semua, set default max 1 juta agar grafik tetap tergambar
        val finalMaxY = if (maxVal == 0.0) 1000000.0 else maxVal

        _uiState.update {
            it.copy(
                selectedChartFilter = filter,
                chartData = data,
                chartMaxY = finalMaxY
            )
        }
    }

    private fun formatCurrency(amount: Double): String {
        val localeID = Locale("in", "ID")
        val numberFormat = NumberFormat.getCurrencyInstance(localeID)
        numberFormat.maximumFractionDigits = 0
        return numberFormat.format(amount)
    }
}