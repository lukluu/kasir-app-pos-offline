package com.example.kasirku.ui.screen.category

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kasirku.domain.model.Category
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    navController: androidx.navigation.NavController,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 1. State Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // 2. Listener Event
    LaunchedEffect(true) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // --- (Kode Status Bar tetap sama) ---
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = surfaceColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    LaunchedEffect(uiState.activeSheet) {
        if (uiState.activeSheet != ActiveSheet.None) sheetState.show() else sheetState.hide()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar( // Biar judul di tengah
                title = { Text("Kategori") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        // 3. Pasang Snackbar Host
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.categories.isEmpty()) {
                EmptyState(Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(uiState.categories, key = { it.id }) { category ->
                        CategoryListItem(
                            category = category,
                            onClick = { viewModel.onCategoryClicked(category) }
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }

            Button(
                onClick = { viewModel.onAddClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Tambah Kategori", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // ... (Kode Sheet & Dialog tetap sama) ...
    if (uiState.activeSheet != ActiveSheet.None) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onSheetDismiss() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.imePadding() // Agar tidak tertutup keyboard
        ) {
            when (uiState.activeSheet) {
                ActiveSheet.Options -> {
                    CategoryOptionsSheetContent(
                        categoryName = uiState.selectedCategory?.name ?: "",
                        onEditClick = { viewModel.onEditClicked() },
                        onDeleteClick = { viewModel.onDeleteClicked() }
                    )
                }
                ActiveSheet.AddEdit -> {
                    AddEditCategorySheetContent(
                        isEditing = uiState.selectedCategory != null,
                        name = uiState.categoryNameInput,
                        onNameChange = { viewModel.onCategoryNameChange(it) },
                        onSaveClick = { viewModel.onConfirmAddEdit() }
                    )
                }
                ActiveSheet.None -> {}
            }
        }
    }

    if(uiState.showDeleteConfirmDialog){
        DeleteConfirmationDialog(
            categoryName = uiState.selectedCategory?.name ?: "",
            onConfirm = { viewModel.onConfirmDelete() },
            onDismiss = { viewModel.onDismissDeleteDialog() }
        )
    }
}
@Composable
private fun CategoryListItem(category: Category, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CategoryOptionsSheetContent(
    categoryName: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        )
        Divider()
        ListItem(
            headlineContent = { Text("Edit") },
            leadingContent = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
            modifier = Modifier.clickable(onClick = onEditClick)
        )
        ListItem(
            headlineContent = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
            leadingContent = { Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error) },
            modifier = Modifier.clickable(onClick = onDeleteClick)
        )
    }
}

@Composable
private fun AddEditCategorySheetContent(
    isEditing: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isEditing) "Edit Kategori" else "Tambah Kategori Baru",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nama Kategori") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSaveClick,
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simpan")
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Belum ada kategori.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    categoryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Kategori?") },
        text = { Text("Apakah Anda yakin ingin menghapus kategori \"$categoryName\"?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Hapus") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}