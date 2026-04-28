package com.example.kasirku.ui.screen.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.domain.model.Customer
import com.example.kasirku.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerUiState(
    val customers: List<Customer> = emptyList(),
    val filteredCustomers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val activeSheet: ActiveSheet = ActiveSheet.None,
    val selectedCustomer: Customer? = null,
    val showDeleteConfirmDialog: Boolean = false,
    // State untuk input di bottom sheet
    val nameInput: String = "",
    val phoneInput: String = "",
    val addressInput: String = ""
)

sealed interface ActiveSheet {
    object None : ActiveSheet
    object Options : ActiveSheet
    object AddEdit : ActiveSheet
}

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()
    // Channel untuk Snackbar
    private val _uiEvent = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()
    init {
        combine(
            repository.getAllCustomers(),
            _uiState.map { it.searchQuery }.distinctUntilChanged()
        ) { customers, query ->
            val filtered = if (query.isBlank()) {
                customers
            } else {
                customers.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.phoneNumber?.contains(query) == true
                }
            }
            _uiState.update { it.copy(customers = customers, filteredCustomers = filtered) }
        }.launchIn(viewModelScope)
    }

    // Event Handlers
    fun onSearchQueryChange(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun onNameChange(name: String) = _uiState.update { it.copy(nameInput = name) }
    fun onPhoneChange(phone: String) = _uiState.update { it.copy(phoneInput = phone) }
    fun onAddressChange(address: String) = _uiState.update { it.copy(addressInput = address) }
    fun onSheetDismiss() = _uiState.update { it.copy(activeSheet = ActiveSheet.None) }

    fun onAddClicked() {
        _uiState.update {
            it.copy(
                activeSheet = ActiveSheet.AddEdit,
                selectedCustomer = null,
                nameInput = "", phoneInput = "", addressInput = ""
            )
        }
    }

    fun onCustomerClicked(customer: Customer) {
        _uiState.update { it.copy(selectedCustomer = customer, activeSheet = ActiveSheet.Options) }
    }

    fun onEditClicked() {
        _uiState.update {
            it.copy(
                activeSheet = ActiveSheet.AddEdit,
                nameInput = it.selectedCustomer?.name ?: "",
                phoneInput = it.selectedCustomer?.phoneNumber ?: "",
                addressInput = it.selectedCustomer?.address ?: ""
            )
        }
    }

    fun onDeleteClicked() {
        _uiState.update { it.copy(activeSheet = ActiveSheet.None, showDeleteConfirmDialog = true) }
    }

    fun onDismissDeleteDialog() = _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    fun onConfirmDelete() {
        _uiState.value.selectedCustomer?.let { customer ->
            viewModelScope.launch {
                try {
                    repository.deleteCustomer(customer)
                    _uiEvent.send("Pelanggan berhasil dihapus") // Kirim pesan
                    _uiState.update { it.copy(showDeleteConfirmDialog = false, selectedCustomer = null) }
                } catch (e: Exception) {
                    _uiEvent.send("Gagal menghapus: ${e.message}")
                }
            }
        }
    }

    fun onConfirmAddEdit() {
        val state = _uiState.value
        if (state.nameInput.isBlank()) return

        viewModelScope.launch {
            try {
                val isEdit = state.selectedCustomer != null
                val customer = Customer(
                    id = state.selectedCustomer?.id ?: 0,
                    name = state.nameInput.trim(),
                    phoneNumber = state.phoneInput.trim().ifBlank { null },
                    address = state.addressInput.trim().ifBlank { null }
                )
                repository.addOrUpdateCustomer(customer)

                val msg = if (isEdit) "Pelanggan diperbarui" else "Pelanggan ditambahkan"
                _uiEvent.send(msg) // Kirim pesan

                onSheetDismiss()
            } catch (e: Exception) {
                _uiEvent.send("Gagal menyimpan: ${e.message}")
            }
        }
    }
}