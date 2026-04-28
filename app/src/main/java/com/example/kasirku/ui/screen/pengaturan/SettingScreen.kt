package com.example.kasirku.ui.screen.pengaturan

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kasirku.ui.components.AppBottomBar
import com.example.kasirku.ui.navigation.Screen
import com.example.kasirku.ui.utils.PrinterHelper
import com.example.kasirku.ui.viewmodel.AuthState
import com.example.kasirku.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun SettingScreen(
    navController: NavController,
    viewModel: SettingViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onLogoSelected(context, uri)
        }
    }
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Unauthenticated -> {
                Log.d("SettingScreen", "User logged out, navigating to Login")
                navController.navigate(Screen.Login.route) {
                    // Hapus semua screen dari back stack
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Authenticated -> {
                // User masih login, tidak perlu melakukan apa-apa
                Log.d("SettingScreen", "User still authenticated")
            }
            is AuthState.Loading -> {
                // Loading state
            }
        }
    }
    LaunchedEffect(Unit) { viewModel.loadUserProfile() }

    // Helper Bluetooth
    val printerHelper = remember { PrinterHelper(context) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) { showPrinterDialog = true }
        else { Toast.makeText(context, "Izin Bluetooth diperlukan", Toast.LENGTH_SHORT).show() }
    }

    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) {
            // Panggil logout dari AuthViewModel
            authViewModel.logout()
        }
    }

    Scaffold(
        bottomBar = { AppBottomBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text("Pengaturan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(2.dp, MaterialTheme.colorScheme.error, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.outletLogo != null) {
                        AsyncImage(
                            model = state.outletLogo,
                            contentDescription = "Logo Toko",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text(state.outletName.ifEmpty { "Nama Toko" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text(state.outletPhone.ifEmpty { "No HP" }, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.outletAddress.ifEmpty { "Alamat belum diatur" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Tombol Edit (Buka Sheet)
                IconButton(onClick = { viewModel.onEditProfileClicked() }) {
                    Icon(Icons.Default.Edit, null, tint = Color(0xFF4CAF50))
                }
            }

            Divider(Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Text("Menu", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))

            LazyColumn {
                item {
                    SettingItem("Printers", Icons.Outlined.Print) {
                        // Langsung Navigasi ke PrinterScreen
                        navController.navigate(Screen.SettingPrinters.route)
                    }
                }
                item { SettingItem("Reset No. Pesanan", Icons.Outlined.RestartAlt) { viewModel.showResetDialog(true) } }
                item { SettingItem("Back Up", Icons.Outlined.Backup) { viewModel.backupData(context) } }
                item { SettingItem("Restore", Icons.Outlined.Restore) { viewModel.restoreData(context) } }
                item { SettingItem("Metode Pembayaran", Icons.Outlined.Payments) { navController.navigate(Screen.SettingPaymentMethods.route) } }
                item {
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.showLogoutDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Keluar / Logout", fontWeight = FontWeight.Bold, color= Color.White) }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }

    // --- BOTTOM SHEET EDIT PROFIL ---
    if (state.showEditProfileSheet) {
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        // 1. State untuk scroll
        val scrollState = rememberScrollState()

        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissEditProfileSheet() },
            sheetState = editSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            // 2. Pastikan imePadding ada di sini
            modifier = Modifier.imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 3. Aktifkan Vertical Scroll
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp), // Padding bawah agar tidak mepet
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Ubah Data Outlet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Text("Logo", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))

                // --- BOX PILIH GAMBAR ---
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.editLogo != null) {
                        AsyncImage(
                            model = state.editLogo,
                            contentDescription = "Logo Baru",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --- FORM INPUT ---
                // Gunakan ImeAction Next agar otomatis pindah ke bawah
                OutlinedTextField(
                    value = state.editName, onValueChange = { viewModel.onEditNameChange(it) },
                    label = { Text("Nama") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.editPhone, onValueChange = { viewModel.onEditPhoneChange(it) },
                    label = { Text("No. Handphone") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.editAddress, onValueChange = { viewModel.onEditAddressChange(it) },
                    label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.editFooter, onValueChange = { viewModel.onEditFooterChange(it) },
                    label = { Text("Footer Nota (Opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.saveProfile(context) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }

                // Tambahan spacer agar scroll bottom nyaman
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // --- DIALOGS (SAMA SEPERTI SEBELUMNYA) ---
    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLogoutDialog(false) },
            title = { Text("Konfirmasi Logout") },
            text = { Text("Apakah Anda yakin ingin keluar dari akun ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout() // Ini akan trigger state.isLoggedOut = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showLogoutDialog(false) }) {
                    Text("Batal")
                }
            }
        )
    }

    if (state.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showResetDialog(false) },
            title = { Text("Reset Nomor Pesanan") },
            text = { Text("Nomor antrian pesanan akan kembali ke 0001.\nData laporan TIDAK akan dihapus.\n\nLanjutkan?") },
            confirmButton = { Button(onClick = { viewModel.resetOrderNumber(context) }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { viewModel.showResetDialog(false) }) { Text("Batal") } }
        )
    }

    if (showPrinterDialog) {
        val devices = printerHelper.getPairedDevices()
        AlertDialog(
            onDismissRequest = { showPrinterDialog = false },
            title = { Text("Pilih Printer") },
            text = {
                if (!printerHelper.isBluetoothEnabled()) {
                    Column {
                        Text("Bluetooth mati.", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { printerHelper.enableBluetooth() }) { Text("Aktifkan") }
                    }
                } else if (devices.isEmpty()) {
                    Text("Tidak ada printer terhubung.")
                } else {

                    LazyColumn {
                        items(devices) { device ->
                            ListItem(
                                headlineContent = { Text(device.name ?: "Unknown") },
                                supportingContent = { Text(device.address) },
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "Dipilih: ${device.name}", Toast.LENGTH_SHORT).show()
                                    showPrinterDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPrinterDialog = false }) { Text("Tutup") } }
        )
    }
}

@Composable
fun SettingItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Column {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground) },
            leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onClick)
        )
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
}