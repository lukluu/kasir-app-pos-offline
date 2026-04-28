package com.example.kasirku.ui.screen.buatpesanan

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.domain.model.*
import com.example.kasirku.domain.repository.*
import com.example.kasirku.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

enum class DiscountType { NOMINAL, PERCENTAGE }
enum class OrderLoadMode { NEW, EDIT, REORDER }
data class CreateOrderUiState(
    val allProducts: List<Product> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val allCustomers: List<Customer> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: Int? = null,
    val cart: Map<Product, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val selectedCustomer: Customer? = null,
    val note: String = "",
    val useDiscount: Boolean = false,
    val discountAmount: String = "0",
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val subtotal: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val total: Double = 0.0,
    val orderSavedSuccessfully: Long? = null,

    // --- STATE PELANGGAN (SHEET) ---
    val showCustomerSheet: Boolean = false,      // Sheet Pencarian
    val showAddCustomerSheet: Boolean = false,   // Sheet Tambah Baru

    val customerSearchQuery: String = "",
    val filteredCustomers: List<Customer> = emptyList(),

    // Input Form Tambah Pelanggan
    val newCustomerName: String = "",
    val newCustomerPhone: String = "",
    val newCustomerAddress: String = "",

    val showPaymentConfirmation: Boolean = false,
    val showSuccessSnackbar: Boolean = false,
    val editingOrderId: Long? = null,
    val currentMode: OrderLoadMode = OrderLoadMode.NEW,

    // Payment Method
    val availablePaymentMethods: List<String> = listOf("Tunai"),
    val selectedPaymentMethod: String = "Tunai"
)

@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val customerRepository: CustomerRepository,
    private val orderRepository: OrderRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()
    private val navOrderId: Long = savedStateHandle.get<Long>("orderId") ?: -1L
    private val navModeString: String = savedStateHandle.get<String>("mode") ?: OrderLoadMode.NEW.name
    private val initialMode = try {
        OrderLoadMode.valueOf(navModeString.uppercase())
    } catch (e: Exception) {
        OrderLoadMode.NEW
    }
    init {
        // Load Data Utama
        combine(
            productRepository.getAllProducts(),
            categoryRepository.getCategories(),
            customerRepository.getAllCustomers()
        ) { products, categories, customers ->
            Triple(products, categories, customers)
        }.onEach { (products, categories, customers) ->
            _uiState.update {
                it.copy(
                    allProducts = products,
                    allCategories = categories,
                    allCustomers = customers,
                    filteredCustomers = customers,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)

        // Load Payment Methods
        viewModelScope.launch {
            paymentMethodRepository.getAllPaymentMethods().collect { methods ->
                val methodNames = methods.map { it.name }
                val combinedMethods = listOf("Tunai") + methodNames.filter { it != "Tunai" }
                _uiState.update { it.copy(availablePaymentMethods = combinedMethods) }
            }
        }

        // Filter Pelanggan Live Search
        _uiState.map { it.customerSearchQuery to it.allCustomers }
            .distinctUntilChanged()
            .onEach { (query, all) ->
                val filtered = if (query.isBlank()) all else all.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            (it.phoneNumber?.contains(query) == true)
                }
                _uiState.update { it.copy(filteredCustomers = filtered) }
            }.launchIn(viewModelScope)

        // Perhitungan Total
        _uiState.map { state ->
            val subtotal = state.cart.entries.sumOf { (product, qty) -> product.price * qty }
            val discountValue = state.discountAmount.toDoubleOrNull() ?: 0.0
            val finalDiscount = if (state.useDiscount) {
                if (state.discountType == DiscountType.PERCENTAGE) {
                    subtotal * (discountValue / 100)
                } else {
                    discountValue
                }
            } else 0.0
            val total = (subtotal - finalDiscount).coerceAtLeast(0.0)
            Triple(subtotal, finalDiscount, total)
        }.distinctUntilChanged().onEach { (sub, disc, tot) ->
            _uiState.update { it.copy(subtotal = sub, totalDiscount = disc, total = tot) }
        }.launchIn(viewModelScope)

        // Filter Produk
        _uiState.map { state ->
            val byCat = if (state.selectedCategoryId == null) state.allProducts else state.allProducts.filter { it.categoryId == state.selectedCategoryId }
            if (state.searchQuery.isBlank()) byCat else byCat.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
        }.distinctUntilChanged().onEach { filtered ->
            _uiState.update { it.copy(filteredProducts = filtered) }
        }.launchIn(viewModelScope)


        if (navOrderId > 0L) {
            loadOrderData(navOrderId, initialMode)
        } else {
            _uiState.update { it.copy(currentMode = OrderLoadMode.NEW) }
        }
    }

    fun refreshData() { _uiState.update { it.copy(isLoading = false) } }

    // --- Actions Produk ---
    fun onSearchQueryChange(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun onCategorySelected(categoryId: Int?) = _uiState.update { it.copy(selectedCategoryId = categoryId) }

    fun onAddToCart(product: Product) {
        val newCart = _uiState.value.cart.toMutableMap()
        newCart[product] = (newCart[product] ?: 0) + 1
        _uiState.update { it.copy(cart = newCart) }
    }
    fun onIncreaseQuantity(product: Product) = onAddToCart(product)
    fun onDecreaseQuantity(product: Product) {
        val newCart = _uiState.value.cart.toMutableMap()
        val currentQty = newCart[product] ?: 0
        if (currentQty > 1) newCart[product] = currentQty - 1 else newCart.remove(product)
        _uiState.update { it.copy(cart = newCart) }
    }
    fun onRemoveFromCart(product: Product) {
        val newCart = _uiState.value.cart.toMutableMap()
        newCart.remove(product)
        _uiState.update { it.copy(cart = newCart) }
    }

    fun onNoteChange(note: String) = _uiState.update { it.copy(note = note) }
    // Di dalam class CreateOrderViewModel

    fun onDiscountTypeChange(newType: DiscountType) {
        _uiState.update { currentState ->
            val subtotal = currentState.subtotal

            // Ambil nilai diskon yang sedang aktif (sebagai Nominal)
            val currentInputAmount = currentState.discountAmount.toDoubleOrNull() ?: 0.0

            // 1. Tentukan Nilai Nominal Aktif berdasarkan Tipe Lama
            val currentNominalValue = if (currentState.discountType == DiscountType.PERCENTAGE) {
                (currentInputAmount / 100.0) * subtotal
            } else {
                currentInputAmount
            }

            // 2. Lakukan Konversi ke Tipe Baru
            val newAmountString = when (newType) {
                DiscountType.NOMINAL -> {
                    // Konversi ke NOMINAL: Pertahankan nilai Nominal
                    currentNominalValue.toLong().toString()
                }
                DiscountType.PERCENTAGE -> {
                    if (subtotal > 0) {
                        val percentage = (currentNominalValue / subtotal) * 100.0

                        // --- PERBAIKAN: Gunakan Locale.US untuk memastikan titik desimal (DOT) ---
                        "%.2f".format(Locale.US, percentage.coerceAtMost(100.0))
                    } else {
                        "0"
                    }
                }
            }

            // 3. Update State
            currentState.copy(
                discountType = newType,
                discountAmount = newAmountString
            )
        }
    }
    fun onDiscountSwitchChange(useDiscount: Boolean) = _uiState.update { it.copy(useDiscount = useDiscount) }
    fun onDiscountAmountChange(amount: String) = _uiState.update { it.copy(discountAmount = amount) }
    fun onPaymentMethodSelected(method: String) = _uiState.update { it.copy(selectedPaymentMethod = method) }

    fun onCustomerSelected(customer: Customer?) {
        _uiState.update { it.copy(selectedCustomer = customer, showCustomerSheet = false) }
    }

    // 2. Sheet Tambah Pelanggan
    fun onShowAddCustomerSheet(show: Boolean) = _uiState.update { it.copy(showAddCustomerSheet = show, newCustomerName = "", newCustomerPhone = "") }
    fun onNewCustomerNameChange(name: String) = _uiState.update { it.copy(newCustomerName = name) }
    fun onNewCustomerPhoneChange(phone: String) = _uiState.update { it.copy(newCustomerPhone = phone) }
    fun onNewCustomerAddressChange(address: String) = _uiState.update { it.copy(newCustomerAddress = address) }

    // Simpan Pelanggan Baru
    fun onSaveNewCustomer() {
        val state = _uiState.value
        if (state.newCustomerName.isBlank()) return

        viewModelScope.launch {
            val newCustomer = Customer(0, state.newCustomerName, state.newCustomerPhone.ifBlank { null }, address = state.newCustomerAddress.ifBlank { null })
            customerRepository.addOrUpdateCustomer(newCustomer)

            // Tutup sheet dan pilih customer baru
            _uiState.update { it.copy(showAddCustomerSheet = false) }

            val updatedCustomers = customerRepository.getAllCustomers().first()
            val addedCustomer = updatedCustomers.find { it.name == state.newCustomerName }

            if (addedCustomer != null) {
                _uiState.update { it.copy(selectedCustomer = addedCustomer) }
            }
        }
    }

    // --- ORDER MANAGEMENT ---
    fun onShowPaymentConfirmation(show: Boolean) = _uiState.update { it.copy(showPaymentConfirmation = show) }
    fun onHideSnackbar() { _uiState.update { it.copy(showSuccessSnackbar = false) } }

    fun loadOrderForEditing(orderId: Long) {
        viewModelScope.launch {
            val order = orderRepository.getOrderById(orderId).first()
            if (order != null) {
                val cartMap = order.items.associate { it.product to it.quantity }
                _uiState.update {
                    it.copy(
                        editingOrderId = order.id,
                        cart = cartMap,
                        selectedCustomer = order.customer,
                        note = order.note ?: "",
                        useDiscount = order.discount > 0,
                        discountAmount = if (order.discount > 0) order.discount.toInt().toString() else "0",
                        selectedPaymentMethod = "Tunai"
                    )
                }
            }
        }
    }

    fun loadOrderData(oldOrderId: Long, mode: OrderLoadMode) {
        viewModelScope.launch {
            if (oldOrderId <= 0L) return@launch

            val oldOrder = orderRepository.getOrderById(oldOrderId).first()
            val cartMap = orderRepository.getOrderDetailsForReorder(oldOrderId)

            if (oldOrder == null) return@launch

            // 1. Tentukan ID Akhir (Kunci untuk INSERT vs UPDATE)
            // EDIT: Pertahankan ID (ID lama > 0). REORDER: ID diset null.
            val finalEditingId = if (mode == OrderLoadMode.EDIT) oldOrderId else null

            // 2. Tentukan Diskon (Reset jika Reorder, Salin jika Edit)
            val (finalUseDiscount, finalDiscountAmount) = if (mode == OrderLoadMode.REORDER) {
                // REORDER: Reset diskon ke 0
                Pair(false, "0")
            } else {
                // EDIT: Salin nilai diskon lama
                Pair(oldOrder.discount > 0, if (oldOrder.discount > 0) oldOrder.discount.toInt().toString() else "0")
            }

            _uiState.update {
                it.copy(
                    editingOrderId = finalEditingId, // null untuk NEW/REORDER
                    currentMode = mode, // Simpan mode

                    cart = cartMap,
                    selectedCustomer = oldOrder.customer,
                    note = oldOrder.note ?: "",

                    // Set Diskon yang sudah direset/disalin
                    useDiscount = finalUseDiscount,
                    discountAmount = finalDiscountAmount,
                    discountType = DiscountType.NOMINAL,

                    selectedPaymentMethod = "Tunai"
                )
            }
        }
    }



    fun resetState() {
        _uiState.update { CreateOrderUiState(availablePaymentMethods = it.availablePaymentMethods) }
    }

    fun onSaveOrder(
        isPaid: Boolean,
        navController: androidx.navigation.NavController,
        context: Context,
        amountPaidInput: Double
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val currentOrderId = state.editingOrderId
            val paymentMethod = state.selectedPaymentMethod
            val totalTagihan = state.total

            val finalAmountPaid = if (isPaid) {
                if (paymentMethod == "Tunai") amountPaidInput else totalTagihan
            } else {
                0.0
            }

            val result = if (currentOrderId != null) {
                orderRepository.updateOrder(
                    orderId = currentOrderId,
                    cart = state.cart,
                    customer = state.selectedCustomer,
                    note = state.note,
                    discount = state.totalDiscount,
                    isPaid = isPaid,
                    paymentMethod = paymentMethod,
                    amountPaid = finalAmountPaid
                )
            } else {
                orderRepository.saveOrder(
                    cart = state.cart,
                    customer = state.selectedCustomer,
                    note = state.note,
                    discount = state.totalDiscount,
                    isPaid = isPaid,
                    paymentMethod = paymentMethod,
                    amountPaid = finalAmountPaid
                )
            }

            result.onSuccess { orderId ->
                _uiState.update { it.copy(orderSavedSuccessfully = orderId, showPaymentConfirmation = false, editingOrderId = null) }

                val msg = if(currentOrderId != null) "Pesanan Berhasil Diubah" else "Pesanan Berhasil Disimpan"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

                navController.navigate(Screen.OrderDetail.createRoute(orderId)) {
                    popUpTo(Screen.CreateOrderGraph.route) { inclusive = true }
                }
                resetState()
            }

            result.onFailure { e ->
                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}