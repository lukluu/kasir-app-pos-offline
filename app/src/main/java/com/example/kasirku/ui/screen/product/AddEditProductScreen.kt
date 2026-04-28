package com.example.kasirku.ui.screen.product

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.kasirku.ui.components.CurrencyVisualTransformation
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    navController: NavController,
    viewModel: AddEditProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 1. Snackbar Host State
    val snackbarHostState = remember { SnackbarHostState() }

    // 2. Listener Event (Alert Tambah/Edit/Hapus)
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { message ->
            // Jika pesan mengandung "berhasil", pakai Toast (karena layar akan ditutup)
            if (message.contains("berhasil", ignoreCase = true)) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } else {
                // Jika Gagal/Validasi, pakai Snackbar (tetap di layar)
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    // 3. Navigasi Kembali saat Sukses (Simpan/Hapus)
    LaunchedEffect(uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            navController.popBackStack()
        }
    }

    // 4. Dialog Konfirmasi Hapus
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissDeleteDialog() },
            title = { Text("Hapus Produk") },
            text = { Text("Apakah Anda yakin ingin menghapus produk ini? Produk yang sudah ada di riwayat pesanan tidak akan hilang dari laporan.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.onConfirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissDeleteDialog() }) {
                    Text("Batal")
                }
            }
        )
    }

    // ... (Launchers Image Picker Tetap Sama) ...
    val imageCropperLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { viewModel.onImageUriChange(it.toString()) }
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val cropOptions = CropImageContractOptions(
                uri = it,
                cropImageOptions = CropImageOptions(aspectRatioX = 1, aspectRatioY = 1, fixAspectRatio = true)
            )
            imageCropperLauncher.launch(cropOptions)
        }
    }

    val (nameFocus, priceFocus, stockFocus) = remember { FocusRequester.createRefs() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (uiState.isEditing) "Ubah Produk" else "Tambah Produk") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Tampilkan Tombol Hapus HANYA jika sedang Edit
                    if (uiState.isEditing) {
                        IconButton(onClick = { viewModel.onDeleteClicked() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus Produk",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ... (Image Picker UI Tetap Sama) ...
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.imageUri.isNullOrBlank()) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Tambah Gambar", modifier = Modifier.size(40.dp))
                    } else {
                        AsyncImage(
                            model = uiState.imageUri,
                            contentDescription = "Gambar Produk",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // ... (Inputs Tetap Sama) ...
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Nama") },
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(
                        FocusDirection.Down) })
                )

                CategoryDropdown(
                    categories = uiState.allCategories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = {
                        viewModel.onCategorySelected(it)
                        priceFocus.requestFocus()
                    }
                )

                OutlinedTextField(
                    value = uiState.price,
                    onValueChange = { newValue -> viewModel.onPriceChange(newValue.filter { it.isDigit() }) },
                    label = { Text("Harga Jual") },
                    modifier = Modifier.fillMaxWidth().focusRequester(priceFocus),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    visualTransformation = CurrencyVisualTransformation(),
                    keyboardActions = KeyboardActions(onNext = {
                        if (!uiState.isUnlimited) {
                            stockFocus.requestFocus()
                        } else {
                            focusManager.clearFocus()
                            viewModel.onSaveClicked()
                        }
                    })
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gunakan Stok", modifier = Modifier.weight(1f))
                    Switch(
                        checked = !uiState.isUnlimited,
                        onCheckedChange = { isChecked -> viewModel.onIsUnlimitedChange(!isChecked) },
                        colors = SwitchDefaults.colors(
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                if (!uiState.isUnlimited) {
                    OutlinedTextField(
                        value = uiState.stock,
                        onValueChange = viewModel::onStockChange,
                        label = { Text("Stok") },
                        modifier = Modifier.fillMaxWidth().focusRequester(stockFocus),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            viewModel.onSaveClicked()
                        })
                    )
                }
            }

            // Save Button
            Button(
                onClick = { viewModel.onSaveClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(contentColor = Color.White)
            ) {
                Text("Simpan")
            }
        }
    }
}

// ... (CategoryDropdown tetap sama) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<com.example.kasirku.domain.model.Category>,
    selectedCategory: com.example.kasirku.domain.model.Category?,
    onCategorySelected: (com.example.kasirku.domain.model.Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "Pilih Kategori",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}