package com.example.kasirku.ui.screen.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.domain.model.Category
import com.example.kasirku.domain.model.Product
import com.example.kasirku.domain.repository.CategoryRepository
import com.example.kasirku.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditProductUiState(
    val name: String = "",
    val price: String = "",
    val stock: String = "1",
    val isUnlimited: Boolean = true,
    val imageUri: String? = null,
    val selectedCategory: Category? = null,
    val allCategories: List<Category> = emptyList(),
    val isEditing: Boolean = false,
    val isSaveSuccess: Boolean = false,
    // State untuk Dialog Hapus
    val showDeleteDialog: Boolean = false
)

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditProductUiState())
    val uiState: StateFlow<AddEditProductUiState> = _uiState.asStateFlow()

    // Channel untuk mengirim pesan Snackbar (One-time event)
    private val _uiEvent = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    private val productId: Int = savedStateHandle.get<Int>("productId") ?: -1

    init {
        viewModelScope.launch {
            categoryRepository.getCategories().collect { categories ->
                _uiState.update { it.copy(allCategories = categories) }
                if (productId != -1) {
                    loadProduct(productId)
                }
            }
        }
    }

    private suspend fun loadProduct(id: Int) {
        productRepository.getProductById(id).collect { product ->
            product?.let { p ->
                _uiState.update { state ->
                    val foundCategory = state.allCategories.find { it.id == p.categoryId }
                    state.copy(
                        name = p.name,
                        price = p.price.toString().removeSuffix(".0"),
                        stock = p.stock.toString(),
                        imageUri = p.imageUri,
                        isUnlimited = p.isUnlimited,
                        selectedCategory = foundCategory,
                        isEditing = true
                    )
                }
            }
        }
    }

    fun onIsUnlimitedChange(value: Boolean) = _uiState.update { it.copy(isUnlimited = value) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onPriceChange(value: String) = _uiState.update { it.copy(price = value) }
    fun onStockChange(value: String) = _uiState.update { it.copy(stock = value) }
    fun onImageUriChange(value: String) = _uiState.update { it.copy(imageUri = value) }
    fun onCategorySelected(category: Category) = _uiState.update { it.copy(selectedCategory = category) }

    // --- FITUR HAPUS ---
    fun onDeleteClicked() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun onConfirmDelete() {
        viewModelScope.launch {
            try {
                // Kita butuh objek product untuk dihapus (terutama ID-nya)
                // Kita bisa recreate objectnya dari state saat ini karena ID-nya sama
                val productToDelete = Product(
                    id = productId,
                    name = _uiState.value.name,
                    price = 0.0, stock = 0, categoryId = 0 // Data lain tidak penting untuk delete by ID
                )

                productRepository.deleteProduct(productToDelete)

                _uiEvent.send("Produk berhasil dihapus")
                _uiState.update { it.copy(showDeleteDialog = false, isSaveSuccess = true) }
            } catch (e: Exception) {
                _uiEvent.send("Gagal menghapus: ${e.message}")
                _uiState.update { it.copy(showDeleteDialog = false) }
            }
        }
    }

    // --- FITUR SIMPAN (TAMBAH / EDIT) ---
    fun onSaveClicked() {
        val s = _uiState.value
        val price = s.price.toDoubleOrNull()

        if (price == null) {
            viewModelScope.launch { _uiEvent.send("Harga tidak valid") }
            return
        }

        val finalStock = if (s.isUnlimited) 0 else s.stock.toIntOrNull()
        if (finalStock == null) {
            viewModelScope.launch { _uiEvent.send("Stok tidak valid") }
            return
        }

        if (s.name.isBlank()) {
            viewModelScope.launch { _uiEvent.send("Nama produk wajib diisi") }
            return
        }

        if (s.selectedCategory == null) {
            viewModelScope.launch { _uiEvent.send("Pilih kategori terlebih dahulu") }
            return
        }

        viewModelScope.launch {
            try {
                val productToSave = Product(
                    id = if (s.isEditing) productId else 0,
                    name = s.name,
                    price = price,
                    stock = finalStock,
                    isUnlimited = s.isUnlimited,
                    categoryId = s.selectedCategory.id,
                    imageUri = s.imageUri
                )

                if (s.isEditing) {
                    productRepository.updateProduct(productToSave)
                    _uiEvent.send("Produk berhasil diperbarui") // ALERT EDIT
                } else {
                    productRepository.addProduct(productToSave)
                    _uiEvent.send("Produk berhasil ditambahkan") // ALERT TAMBAH
                }

                _uiState.update { it.copy(isSaveSuccess = true) }
            } catch (e: Exception) {
                _uiEvent.send("Gagal menyimpan: ${e.message}")
            }
        }
    }
}