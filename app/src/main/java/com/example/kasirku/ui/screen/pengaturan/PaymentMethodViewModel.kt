package com.example.kasirku.ui.screen.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.data.local.entity.PaymentMethodEntity
import com.example.kasirku.domain.repository.PaymentMethodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Tipe Sheet yang sedang aktif
sealed interface PaymentSheetType {
    object None : PaymentSheetType     // Tidak ada sheet
    object Options : PaymentSheetType  // Sheet Pilihan (Ubah/Hapus)
    object Form : PaymentSheetType     // Sheet Input (Tambah/Edit)
}

data class PaymentMethodUiState(
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val activeSheet: PaymentSheetType = PaymentSheetType.None, // Ganti showDialog
    val isEditing: Boolean = false,
    val selectedMethod: PaymentMethodEntity? = null,
    val nameInput: String = ""
)

@HiltViewModel
class PaymentMethodViewModel @Inject constructor(
    private val repository: PaymentMethodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentMethodUiState())
    val uiState: StateFlow<PaymentMethodUiState> = _uiState.asStateFlow()

    // Channel untuk Snackbar (One-time event)
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.initDefaultPaymentMethods()
            repository.getAllPaymentMethods().collect { list ->
                _uiState.update { it.copy(paymentMethods = list) }
            }
        }
    }

    // 1. Klik Tombol Tambah -> Buka Form Kosong
    fun onAddClick() {
        _uiState.update {
            it.copy(activeSheet = PaymentSheetType.Form, isEditing = false, nameInput = "", selectedMethod = null)
        }
    }

    // 2. Klik Item List -> Buka Opsi (Ubah/Hapus)
    fun onItemClick(method: PaymentMethodEntity) {
        _uiState.update {
            it.copy(activeSheet = PaymentSheetType.Options, selectedMethod = method)
        }
    }

    // 3. Klik Ubah di Sheet Opsi -> Pindah ke Sheet Form
    fun onEditClick() {
        val currentMethod = _uiState.value.selectedMethod ?: return
        _uiState.update {
            it.copy(activeSheet = PaymentSheetType.Form, isEditing = true, nameInput = currentMethod.name)
        }
    }

    // 4. Klik Hapus -> Hapus & Tutup Sheet
    fun onDeleteClick() {
        val currentMethod = _uiState.value.selectedMethod ?: return
        viewModelScope.launch {
            repository.deletePaymentMethod(currentMethod)
            _uiEvent.send("Metode berhasil dihapus")
            onDismissSheet()
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(nameInput = value) }
    }

    fun onDismissSheet() {
        _uiState.update { it.copy(activeSheet = PaymentSheetType.None) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.nameInput.isBlank()) return

        viewModelScope.launch {
            if (state.isEditing && state.selectedMethod != null) {
                repository.updatePaymentMethod(state.selectedMethod.copy(name = state.nameInput))
                _uiEvent.send("Metode berhasil diperbarui")
            } else {
                repository.addPaymentMethod(state.nameInput)
                _uiEvent.send("Metode berhasil ditambahkan")
            }
            onDismissSheet()
        }
    }
}