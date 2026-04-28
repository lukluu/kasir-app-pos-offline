package com.example.kasirku.ui.screen.pengaturan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(
    navController: NavController,
    viewModel: PaymentMethodViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // State untuk Snackbar & BottomSheet
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    // Listener untuk menampilkan Snackbar dari ViewModel
    LaunchedEffect(true) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Metode Pembayaran", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Button(
                onClick = { viewModel.onAddClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Tambah Metode Pembayaran", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            items(state.paymentMethods) { method ->
                ListItem(
                    headlineContent = {
                        Text(method.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    },
                    modifier = Modifier.clickable {
                        // KLIK ITEM -> BUKA SHEET OPSI
                        viewModel.onItemClick(method)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }

    // --- BOTTOM SHEET ---
    if (state.activeSheet != PaymentSheetType.None) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissSheet() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            // Penting: Agar input tidak tertutup keyboard
            modifier = Modifier.imePadding()
        ) {
            // Konten Sheet berubah tergantung tipe (Options / Form)
            when (state.activeSheet) {
                PaymentSheetType.Options -> {
                    Column(modifier = Modifier.padding(bottom = 32.dp)) {
                        Text(
                            text = state.selectedMethod?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        Divider()

                        // Menu Ubah
                        ListItem(
                            headlineContent = { Text("Ubah Nama") },
                            leadingContent = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { viewModel.onEditClick() }
                        )

                        // Menu Hapus
                        ListItem(
                            headlineContent = { Text("Hapus Metode", color = MaterialTheme.colorScheme.error) },
                            leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable { viewModel.onDeleteClick() }
                        )
                    }
                }

                PaymentSheetType.Form -> {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (state.isEditing) "Ubah Metode" else "Tambah Metode Baru",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = state.nameInput,
                            onValueChange = { viewModel.onNameChange(it) },
                            label = { Text("Nama Metode") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                viewModel.onSave()
                            })
                        )

                        Button(
                            onClick = { viewModel.onSave() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            enabled = state.nameInput.isNotBlank()
                        ) {
                            Text("Simpan")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                else -> {}
            }
        }
    }
}