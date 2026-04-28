package com.example.kasirku.ui.screen.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kasirku.domain.model.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    navController: NavController,
    viewModel: CustomerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 1. State Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // 2. Listener Event Snackbar
    LaunchedEffect(true) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState.activeSheet) {
        if (uiState.activeSheet != ActiveSheet.None) sheetState.show() else sheetState.hide()
    }

    Scaffold(
        // 3. Pasang Snackbar Host
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ... (Isi Column sama persis seperti kode Anda) ...
            CustomTopAppBarWithSearch(
                navController = navController,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange
            )

            if (uiState.filteredCustomers.isEmpty() && uiState.searchQuery.isBlank()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 8.dp)
                ) {
                    items(uiState.filteredCustomers, key = { it.id }) { customer ->
                        CustomerListItem(
                            customer = customer,
                            onClick = { viewModel.onCustomerClicked(customer) }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            Button(
                onClick = { viewModel.onAddClicked() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Tambah Pelanggan")
            }
        }
    }

    // ... (Kode BottomSheet dan Dialog sama persis) ...
    if (uiState.activeSheet != ActiveSheet.None) {
        ModalBottomSheet(
            onDismissRequest = viewModel::onSheetDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            // Tambahkan imePadding agar input tidak tertutup keyboard
            modifier = Modifier.imePadding()
        ) {
            when (uiState.activeSheet) {
                ActiveSheet.Options -> {
                    CustomerOptionsSheetContent(
                        customerName = uiState.selectedCustomer?.name ?: "",
                        onEditClick = viewModel::onEditClicked,
                        onDeleteClick = viewModel::onDeleteClicked
                    )
                }
                ActiveSheet.AddEdit -> {
                    AddEditCustomerSheetContent(
                        isEditing = uiState.selectedCustomer != null,
                        name = uiState.nameInput,
                        phone = uiState.phoneInput,
                        address = uiState.addressInput,
                        onNameChange = viewModel::onNameChange,
                        onPhoneChange = viewModel::onPhoneChange,
                        onAddressChange = viewModel::onAddressChange,
                        onSaveClick = viewModel::onConfirmAddEdit
                    )
                }
                else -> {}
            }
        }
    }

    if (uiState.showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            customerName = uiState.selectedCustomer?.name ?: "",
            onConfirm = viewModel::onConfirmDelete,
            onDismiss = viewModel::onDismissDeleteDialog
        )
    }
}

@Composable
private fun CustomTopAppBarWithSearch(
    navController: NavController,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                text = "Pelanggan",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Cari nama atau no. telpon") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
    }
}

// =======================================================
// PERBAIKAN UTAMA DI SINI
// =======================================================
@Composable
private fun CustomerListItem(customer: Customer, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp), // Padding di dalam Row
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            // Perbaiki warna ikon agar netral
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(customer.name, style = MaterialTheme.typography.bodyLarge)
            // Tampilkan nomor telepon jika ada
            if (!customer.phoneNumber.isNullOrBlank()) {
                Text(
                    text = customer.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddEditCustomerSheetContent(
    isEditing: Boolean,
    name: String, phone: String, address: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    // 1. Create FocusRequesters for each input field
    val (nameFocus, phoneFocus, addressFocus) = remember { FocusRequester.createRefs() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (isEditing) "Ubah Pelanggan" else "Tambah Pelanggan Baru",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Name Input
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nama Pelanggan (Wajib)") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocus), // Apply requester
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), // Set action to "Next"
            keyboardActions = KeyboardActions(onNext = { phoneFocus.requestFocus() }) // On "Next", move to phone input
        )

        // Phone Input
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("No. Telpon (Opsional)") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(phoneFocus), // Apply requester
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next // Set action to "Next"
            ),
            keyboardActions = KeyboardActions(onNext = { addressFocus.requestFocus() }) // On "Next", move to address input
        )

        // Address Input
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Alamat (Opsional)") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(addressFocus), // Apply requester
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), // Set action to "Done"
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus() // Hide the keyboard
                if (name.isNotBlank()) {
                    onSaveClick() // Trigger the save action
                }
            })
        )

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
private fun CustomerOptionsSheetContent(
    customerName: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(customerName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        Divider()
        ListItem(
            headlineContent = { Text("Ubah") },
            leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onEditClick)
        )
        ListItem(
            headlineContent = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
            leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            modifier = Modifier.clickable(onClick = onDeleteClick)
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text("Belum ada pelanggan.", textAlign = TextAlign.Center)
    }
}

@Composable
private fun DeleteConfirmationDialog(customerName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Pelanggan?") },
        text = { Text("Yakin ingin menghapus \"$customerName\"?") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Hapus") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}