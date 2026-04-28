package com.example.kasirku.ui.screen.pesanan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kasirku.ui.components.CurrencyVisualTransformation
import com.example.kasirku.ui.navigation.Screen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    navController: NavController,
    viewModel: OrderDetailViewModel
) {
    val order by viewModel.order.collectAsState()
    val cancelReason by viewModel.cancelReason.collectAsState()

    // 1. AMBIL DATA PAYMENT METHOD DARI VIEWMODEL
    val paymentMethods by viewModel.availablePaymentMethods.collectAsState()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // State Dialog & BottomSheet
    var showPayConfirmDialog by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) } // State Sheet Bayar
    var showCancelSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Listener Event
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when(event) {
                is OrderDetailViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short, withDismissAction = true)
                }
                is OrderDetailViewModel.UiEvent.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }
    var tempAmountPaidInput by remember { mutableStateOf(0.0) }
    // --- 1. DIALOG KONFIRMASI PEMBAYARAN AKHIR ---
    if (showPayConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPayConfirmDialog = false },
            title = { Text("Konfirmasi Pembayaran") },
            text = {
                val msg = if(selectedPaymentMethod == "Tunai")
                    "Terima uang: ${formatCurrency(tempAmountPaidInput)}?"
                else "Proses pembayaran $selectedPaymentMethod?"
                Text(msg)
            },
            confirmButton = {
                Button(
                    onClick = {
                        // PERBAIKAN: Kirim nilai tempAmountPaidInput ke ViewModel
                        viewModel.payOrder(tempAmountPaidInput)
                        showPayConfirmDialog = false
                        showPaymentSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Ya, Bayar") }
            },
            dismissButton = {
                TextButton(onClick = { showPayConfirmDialog = false }) { Text("Batal") }
            }
        )
    }

    // --- 2. BOTTOM SHEET PEMBAYARAN (BARU) ---
    if (showPaymentSheet && order != null) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight(0.95f).imePadding().statusBarsPadding()
        ) {
            // Panggil PaymentSheetContent
            PaymentSheetContent(
                total = order!!.totalPrice,
                paymentMethods = paymentMethods,
                selectedPaymentMethod = selectedPaymentMethod,
                onPaymentMethodChange = viewModel::onPaymentMethodSelected,
                onConfirmPayment = { amountInput ->
                    // 1. Simpan amountInput ke state sementara (agar bisa dipakai di dialog konfirmasi)
                    // Anda perlu menambahkan state ini di atas
                    tempAmountPaidInput = amountInput

                    // 2. Buka dialog konfirmasi
                    showPayConfirmDialog = true
                    showPaymentSheet = false // Tutup sheet
                }
            )
        }
    }

    // --- 3. BOTTOM SHEET BATALKAN PESANAN ---
    if (showCancelSheet) {
        val cancelScrollState = rememberScrollState() // State scroll

        ModalBottomSheet(
            onDismissRequest = { showCancelSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // PENTING: Aktifkan scroll vertikal
                    .verticalScroll(cancelScrollState)
                    .padding(horizontal = 16.dp)
                    // Tambahkan padding bawah agar tidak mepet edge layar/keyboard
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Batalkan Pesanan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Text(
                    text = "Masukkan alasan pembatalan pesanan ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = cancelReason,
                    onValueChange = viewModel::onReasonChange,
                    label = { Text("Alasan Pembatalan") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5, // Batasi tinggi maksimal input text
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                Button(
                    onClick = {
                        viewModel.cancelOrder()
                        showCancelSheet = false
                    },
                    enabled = cancelReason.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Batalkan Pesanan", fontWeight = FontWeight.Bold)
                }

                // Spacer tambahan untuk keamanan scroll
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detail Pesanan", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (order != null) {
                val item = order!!
                val isPaid = item.paymentStatus == "PAID"
                val isCancelled = item.paymentStatus == "CANCELLED"
                val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(item.createdAt)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 32.dp)
                ) {
                    LazyColumn(modifier = Modifier.weight(1f).padding(bottom = 32.dp)) {
                        // INFO PESANAN
                        item {
                            Section("Info Pesanan")
                            RowInfo("Waktu Pesanan", date)
                            RowInfo("No. Pesanan", item.orderNumber)
                            if (isCancelled && !item.cancelReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Dibatalkan karena:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Text(item.cancelReason!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                            Divider(Modifier.padding(vertical = 16.dp))

                            Section("Info Pelanggan")
                            RowInfo("Nama", item.customer?.name ?: "-")
                            RowInfo("No.HP", item.customer?.phoneNumber ?: "-")
                            if (!item.note.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Catatan", color = Color.Gray)
                                    Text(item.note, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.padding(start = 16.dp))
                                }
                            }
                            Divider(Modifier.padding(vertical = 16.dp))
                            Section("Produk")
                        }

                        // ITEM PRODUK
                        items(item.items) { orderItem ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(6.dp))

                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (orderItem.product.imageUri.isNullOrBlank()) {

                                        Icon(
                                            imageVector = Icons.Default.Fastfood,
                                            contentDescription = null,
                                            modifier = Modifier.size(25.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        AsyncImage(
                                            model = orderItem.product.imageUri, contentDescription = null,
                                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }


                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(orderItem.product.name, fontWeight = FontWeight.Bold)
                                    Text("${orderItem.quantity} x ${formatCurrency(orderItem.priceAtSale)}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text(formatCurrency(orderItem.priceAtSale * orderItem.quantity), fontWeight = FontWeight.Bold)
                            }
                        }
                        item {
                            Divider(Modifier.padding(vertical = 16.dp))
                            Section("Info Pembayaran")
                            val subtotal = item.totalPrice + item.discount
                            RowInfo("Subtotal", formatCurrency(subtotal))
                            if (item.discount > 0) RowInfo("Diskon", "-${formatCurrency(item.discount)}")
                            Spacer(Modifier.height(8.dp))

                            // --- TAMBAHAN: METODE PEMBAYARAN ---
                            if (isPaid) {
                                // 1. Metode Bayar
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Metode Bayar", color = Color.Gray)
                                    Text(
                                        text = if (item.paymentMethod.isNullOrBlank()) "Tunai" else item.paymentMethod,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // 2. Jumlah Bayar (Uang Diterima)
                                if (item.amountPaid > 0) { // Hanya tampil jika ada data amountPaid
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Uang Diterima", color = Color.Gray)
                                        Text(formatCurrency(item.amountPaid), fontWeight = FontWeight.Medium)
                                    }

                                    // 3. Kembalian (Hitung: Uang Diterima - Total)
                                    val change = (item.amountPaid - item.totalPrice).coerceAtLeast(0.0)
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Kembalian", color = Color.Gray)
                                        Text(formatCurrency(change), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            // -----------------------------------

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Pesanan", color = Color.Gray)
                                Text(formatCurrency(item.totalPrice), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Status Pembayaran", color = Color.Gray)

                                val badgeColor = when {
                                    isPaid -> Color(0xFF4CAF50)
                                    isCancelled -> Color.Gray
                                    else -> Color(0xFFE64A19)
                                }
                                val statusText = when {
                                    isPaid -> "Dibayar"
                                    isCancelled -> "Dibatalkan"
                                    else -> "Belum Dibayar"
                                }

                                Box(modifier = Modifier.background(badgeColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(statusText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
//
//                    Spacer(modifier = Modifier.height(24.dp))

                    // === TOMBOL AKSI ===
                    if (!isPaid && !isCancelled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 1. Tombol Bayar (Buka Sheet)
                            Button(
                                onClick = { showPaymentSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Bayar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            // 2. Batal & Ubah
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.onReasonChange(""); showCancelSheet = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Batal")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        navController.navigate(
                                            Screen.CreateOrderGraph.createRoute(
                                                orderId = order!!.id,
                                                mode = "EDIT" // <-- MODE EDIT
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Ubah")
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }


                    // Lihat Nota
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.Receipt.createRoute(order!!.id)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Outlined.Receipt, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Lihat Nota", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (isPaid) {
                        Button(
                            onClick = {
                                navController.navigate(
                                    Screen.CreateOrderGraph.createRoute(
                                        orderId = item.id,
                                        mode = "REORDER" // <-- MODE REORDER
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C8A06)) // Warna Kuning/Aksen
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            ); Spacer(Modifier.width(8.dp)); Text(
                            "Pesan Ulang", fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Snackbar
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) { data ->
                Snackbar(snackbarData = data, containerColor = Color(0xFF323232), contentColor = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentSheetContent(
    total: Double,
    paymentMethods: List<String>,
    selectedPaymentMethod: String,
    onPaymentMethodChange: (String) -> Unit,
    onConfirmPayment: (Double) -> Unit
) {
    val isCash = selectedPaymentMethod == "Tunai"
    var amountPaidInput by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPaymentMethod, total) {
        if (!isCash) amountPaidInput = total.toLong().toString()
        else if (amountPaidInput == total.toLong().toString()) amountPaidInput = ""
    }

    val amountPaidValue = amountPaidInput.toDoubleOrNull() ?: 0.0
    val change = (amountPaidValue - total).coerceAtLeast(0.0)
    val isPayEnabled = !isCash || (amountPaidValue >= total)
    val btnColor = if (isPayEnabled) MaterialTheme.colorScheme.primary else Color(0xFFFF8A8A)

    val quickAmounts = remember(total) {
        val suggestions = mutableListOf<Double>()
        suggestions.add(total)
        val next5k = ceil(total / 5000) * 5000
        val next10k = ceil(total / 10000) * 10000
        val next20k = ceil(total / 20000) * 20000
        val next50k = ceil(total / 50000) * 50000
        val next100k = ceil(total / 100000) * 100000
        listOf(next5k, next10k, next20k, next50k, next100k)
            .filter { it >= total && it != 0.0 }
            .distinct()
            .forEach { suggestions.add(it) }
        suggestions.sorted().distinct().take(6)
    }

    // GUNAKAN COLUMN DENGAN VERTICAL SCROLL
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // <--- INI KUNCINYA
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Total
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Total", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(formatCurrency(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        // Metode Pembayaran
        Column {
            Text("Metode Pembayaran", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = selectedPaymentMethod, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface))
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    paymentMethods.forEach { method -> DropdownMenuItem(text = { Text(method) }, onClick = { onPaymentMethodChange(method); expanded = false }) }
                }
            }
        }

        // Input Jumlah
        Column {
            Text("Jumlah yang Dibayarkan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = amountPaidInput,
                onValueChange = { if (isCash) amountPaidInput = it.filter { c -> c.isDigit() } },
                readOnly = !isCash,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.End),
                visualTransformation = CurrencyVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isCash) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    unfocusedTextColor = if (isCash) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = if (!isCash) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent
                )
            )
        }

        // Grid Uang Cepat (Manual Layout agar tidak crash scroll)
        // Kita bagi menjadi baris-baris (chunked 2 item per baris)
        if (quickAmounts.isNotEmpty()) {
            quickAmounts.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { amount ->
                        val isUangPas = amount == total
                        val label = if (isUangPas) "Uang Pas" else formatCurrency(amount).replace("Rp", "").trim()
                        val borderColor = if (isCash) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
                        val textColor = if (isCash) MaterialTheme.colorScheme.primary else Color.Gray

                        OutlinedButton(
                            onClick = { if (isCash) amountPaidInput = amount.toInt().toString() },
                            enabled = isCash,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.weight(1f), // Bagi rata lebar
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor, disabledContentColor = Color.Gray)
                        ) {
                            Text(label)
                        }
                    }
                    // Jika ganjil, tambahkan spacer agar item kiri tidak melar sendiri
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Kembalian
        Column {
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kembalian", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Text(
                    if (isCash) formatCurrency(change) else "Rp 0",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCash && change < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Tombol Bayar
        Button(
            onClick = { onConfirmPayment(amountPaidValue) },
            enabled = isPayEnabled,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = btnColor, contentColor = Color.White, disabledContainerColor = Color(0xFFFF8A8A).copy(alpha = 0.7f))
        ) {
            Text("Bayar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

//        // Tambahan Spacer bawah agar tidak terlalu mepet saat discroll mentok
//        Spacer(Modifier.height(24.dp))
    }
}

// Helper
@Composable
fun Section(title: String) { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp)) }

@Composable
fun RowInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray); Text(value, fontWeight = FontWeight.Medium)
    }
}

fun formatCurrency(amount: Double) = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount).replace(",00", "")