package com.example.kasirku.ui.screen.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.domain.model.Category
import com.example.kasirku.domain.model.Product
import com.example.kasirku.domain.repository.CategoryRepository
import com.example.kasirku.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductUiState(
    val allProducts: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val activeSheet: ActiveSheet = ActiveSheet.None,
    val selectedProduct: Product? = null,
    val showDeleteConfirmDialog: Boolean = false
)

sealed interface ActiveSheet {
    object None : ActiveSheet
    object Options : ActiveSheet
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    init {
        // Gabungkan semua flow yang dibutuhkan oleh layar ini
        combine(
            productRepository.getAllProducts(),
            categoryRepository.getCategories(),
            _uiState.map { it.searchQuery }.distinctUntilChanged()
        ) { products, categories, query ->
            val filtered = if (query.isBlank()) {
                products
            } else {
                products.filter { it.name.contains(query, ignoreCase = true) }
            }
            _uiState.update {
                it.copy(
                    allProducts = products,
                    filteredProducts = filtered,
                    allCategories = categories,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onProductClicked(product: Product) {
        _uiState.update { it.copy(selectedProduct = product, activeSheet = ActiveSheet.Options) }
    }

    fun onSheetDismiss() {
        _uiState.update { it.copy(activeSheet = ActiveSheet.None) }
    }

    fun onDeleteClicked() {
        _uiState.update { it.copy(activeSheet = ActiveSheet.None, showDeleteConfirmDialog = true) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun onConfirmDelete() {
        _uiState.value.selectedProduct?.let { product ->
            viewModelScope.launch {
                productRepository.deleteProduct(product)
                _uiState.update { it.copy(showDeleteConfirmDialog = false, selectedProduct = null) }
            }
        }
    }
}