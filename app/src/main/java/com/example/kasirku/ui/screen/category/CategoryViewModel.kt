package com.example.kasirku.ui.screen.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.domain.model.Category
import com.example.kasirku.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val activeSheet: ActiveSheet = ActiveSheet.None,
    val selectedCategory: Category? = null,
    val categoryNameInput: String = "",
    val showDeleteConfirmDialog: Boolean = false
)

sealed interface ActiveSheet {
    object None : ActiveSheet
    object AddEdit : ActiveSheet
    object Options : ActiveSheet
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            // 1. Inisialisasi data default (Makanan, Minuman) jika kosong
            repository.initDefaultCategory()

            // 2. Setelah inisialisasi, mulai observe data
            repository.getCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onAddClicked() {
        _uiState.update {
            it.copy(activeSheet = ActiveSheet.AddEdit, selectedCategory = null, categoryNameInput = "")
        }
    }

    fun onCategoryClicked(category: Category) {
        _uiState.update {
            it.copy(activeSheet = ActiveSheet.Options, selectedCategory = category)
        }
    }

    fun onEditClicked() {
        _uiState.update {
            it.copy(activeSheet = ActiveSheet.AddEdit, categoryNameInput = it.selectedCategory?.name ?: "")
        }
    }

    fun onDeleteClicked() {
        _uiState.update {
            it.copy(activeSheet = ActiveSheet.None, showDeleteConfirmDialog = true)
        }
    }

    fun onConfirmDelete() {
        _uiState.value.selectedCategory?.let { category ->
            viewModelScope.launch {
                try {
                    repository.deleteCategory(category)
                    _uiEvent.send("Kategori berhasil dihapus") // Kirim event
                    _uiState.update { it.copy(showDeleteConfirmDialog = false, selectedCategory = null) }
                } catch (e: Exception) {
                    _uiEvent.send("Gagal menghapus: ${e.message}")
                }
            }
        }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun onSheetDismiss() {
        _uiState.update { it.copy(activeSheet = ActiveSheet.None) }
    }

    fun onCategoryNameChange(name: String) {
        _uiState.update { it.copy(categoryNameInput = name) }
    }

    fun onConfirmAddEdit() {
        val currentState = _uiState.value
        val name = currentState.categoryNameInput.trim()

        if (name.isNotBlank()) {
            viewModelScope.launch {
                try {
                    val categoryToSave = currentState.selectedCategory?.copy(name = name)
                        ?: Category(id = 0, name = name)

                    if (currentState.selectedCategory == null) {
                        repository.addCategory(categoryToSave)
                        _uiEvent.send("Kategori berhasil ditambahkan") // Kirim event
                    } else {
                        repository.updateCategory(categoryToSave)
                        _uiEvent.send("Kategori berhasil diperbarui") // Kirim event
                    }
                    onSheetDismiss()
                } catch (e: Exception) {
                    _uiEvent.send("Gagal menyimpan: ${e.message}")
                }
            }
        }
    }
}