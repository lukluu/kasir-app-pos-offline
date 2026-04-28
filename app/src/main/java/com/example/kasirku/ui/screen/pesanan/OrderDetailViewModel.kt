package com.example.kasirku.ui.screen.pesanan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.domain.model.Order
import com.example.kasirku.domain.repository.OrderRepository
import com.example.kasirku.domain.repository.PaymentMethodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val repository: OrderRepository,
    private val paymentMethodRepository: PaymentMethodRepository, // 1. Inject Repository Ini
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: Long = checkNotNull(savedStateHandle["orderId"])

    val order: StateFlow<Order?> = repository.getOrderById(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 2. State untuk Payment Method
    private val _availablePaymentMethods = MutableStateFlow<List<String>>(listOf("Tunai"))
    val availablePaymentMethods = _availablePaymentMethods.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow("Tunai")
    val selectedPaymentMethod = _selectedPaymentMethod.asStateFlow()

    private val _cancelReason = MutableStateFlow("")
    val cancelReason: StateFlow<String> = _cancelReason.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadPaymentMethods()
    }

    // 3. Load Metode dari DB
    private fun loadPaymentMethods() {
        viewModelScope.launch {
            paymentMethodRepository.getAllPaymentMethods().collect { methods ->
                val names = methods.map { it.name }
                // Gabungkan "Tunai" dengan data dari DB
                _availablePaymentMethods.value = listOf("Tunai") + names.filter { it != "Tunai" }
            }
        }
    }



    fun onPaymentMethodSelected(method: String) {
        _selectedPaymentMethod.value = method
    }

    fun onReasonChange(newReason: String) {
        _cancelReason.value = newReason
    }

    // 4. Update payOrder untuk mengirim metode bayar
    fun payOrder(amountPaidInput: Double) {
        val currentOrder = order.value ?: return

        viewModelScope.launch {
            try {
                val method = _selectedPaymentMethod.value

                // --- LOGIKA PENENTUAN AMOUNT PAID ---
                val finalAmountPaid = if (method == "Tunai") {
                    amountPaidInput // Jika Tunai, pakai input user
                } else {
                    currentOrder.totalPrice // Jika Non-Tunai, otomatis uang pas (total tagihan)
                }

                // Panggil Repository (updatePaymentStatusToPaid harus diupdate di Repo/DAO agar terima amountPaid juga)
                // ATAU gunakan fungsi updateOrder jika repository payOrder belum support amountPaid

                // CARA TERBAIK: Tambahkan fungsi updatePaymentInfo di Repository
                repository.payOrderDetail(orderId, method, finalAmountPaid)

                _uiEvent.send(UiEvent.ShowSnackbar("Pembayaran Berhasil ($method)"))
            } catch (e: Exception) {
                e.printStackTrace()
                _uiEvent.send(UiEvent.ShowSnackbar("Gagal: ${e.message}"))
            }
        }
    }

    fun cancelOrder() {
        val reason = _cancelReason.value
        if (reason.isBlank()) {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar("Alasan pembatalan wajib diisi!")) }
            return
        }

        viewModelScope.launch {
            try {
                repository.cancelOrder(orderId, reason)
                _uiEvent.send(UiEvent.ShowSnackbar("Pesanan Dibatalkan"))
                _uiEvent.send(UiEvent.NavigateBack)
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar("Gagal membatalkan: ${e.message}"))
            }
        }
    }


    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object NavigateBack : UiEvent()
    }
}