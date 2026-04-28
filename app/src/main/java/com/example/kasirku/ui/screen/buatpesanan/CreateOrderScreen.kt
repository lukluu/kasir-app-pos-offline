package com.example.kasirku.ui.screen.buatpesanan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kasirku.domain.model.Category
import com.example.kasirku.domain.model.Customer
import com.example.kasirku.domain.model.Product
import com.example.kasirku.ui.components.AppBottomBar
import com.example.kasirku.ui.components.CurrencyVisualTransformation
import com.example.kasirku.ui.navigation.Screen
import com.example.kasirku.ui.utils.isLandscapeMode
import java.text.NumberFormat
import java.util.*
import kotlin.math.ceil

// ==========================================
// SCREEN 1: PRODUCT SELECTION
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSelectionScreen(
    navController: NavController,
    viewModel: CreateOrderViewModel
) {
    val isLandscape = isLandscapeMode()

    if (isLandscape) {
        BuatPesananCombinedScreen(navController, viewModel)
    } else {
        // Tampilan portrait original
        val uiState by viewModel.uiState.collectAsState()
        LaunchedEffect(Unit) {
            if (uiState.isLoading && uiState.allProducts.isNotEmpty()) {
                viewModel.refreshData()
            }
        }

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            CenterAlignedTopAppBar(
                title = { Text("Tambah Pesanan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // SEARCH BAR - HEIGHT KECIL TAPI TEXT TERLIHAT
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                textStyle = TextStyle(
                                    fontSize = 14.sp, // Tetap terlihat
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Normal
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                visualTransformation = VisualTransformation.None,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (uiState.searchQuery.isEmpty()) {
                                            Text(
                                                "Cari nama produk",
                                                fontSize = 14.sp, // Sama dengan input text
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }

                    CategoryFilter(uiState.allCategories, uiState.selectedCategoryId, viewModel::onCategorySelected)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.isLoading) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.filteredProducts.isEmpty()) {
                        // TAMPILAN KOSONG - MENU TIDAK TERSEDIA
                        EmptyProductState(
                            modifier = Modifier.weight(1f),
                            onAddProductClick = { navController.navigate(Screen.AddEditProduct.createRoute(null)) }
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.filteredProducts, key = { it.id }) { product ->
                                ProductGridItem(
                                    product = product,
                                    quantityInCart = uiState.cart[product] ?: 0,
                                    onIncrease = { viewModel.onIncreaseQuantity(product) },
                                    onDecrease = { viewModel.onDecreaseQuantity(product) }
                                )
                            }
                        }
                    }
                }

                if (uiState.cart.isNotEmpty()) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
                        CartSummaryBar(
                            cartItemCount = uiState.cart.values.sum(),
                            total = uiState.total,
                            onContinueClick = { navController.navigate(Screen.OrderSummary.route) }
                        )
                    }
                }
            }
            AppBottomBar(navController = navController)
        }
    }
}

// COMPONENT BARU: TAMPILAN KOSONG
@Composable
fun EmptyProductState(
    modifier: Modifier = Modifier,
    onAddProductClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon besar di tengah
        Icon(
            imageVector = Icons.Default.Fastfood,
            contentDescription = "Tidak ada menu",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Teks judul
        Text(
            text = "Belum Ada Menu",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tombol untuk menambah produk
        Button(
            onClick = onAddProductClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah produk",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Tambah Menu",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}


// ==========================================
// SCREEN 2: ORDER SUMMARY (MODIFIED)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSummaryScreen(
    navController: NavController,
    viewModel: CreateOrderViewModel
) {
    val isLandscape = isLandscapeMode()

    if (isLandscape) {
        // Jika landscape, arahkan kembali ke combined screen
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        // Tampilkan loading atau kosong
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val uiState by viewModel.uiState.collectAsState()
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }

        var showPaymentSheet by remember { mutableStateOf(false) }
        var tempAmountPaidInput by remember { mutableStateOf(0.0) }
        val addCustomerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val paymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // ... (LaunchedEffect logic tetap sama) ...
        LaunchedEffect(uiState.orderSavedSuccessfully) {
            uiState.orderSavedSuccessfully?.let { orderId ->
                navController.navigate(Screen.OrderDetail.createRoute(orderId)) {
                    popUpTo(Screen.CreateOrderGraph.route) { inclusive = true }
                }
            }
        }

        LaunchedEffect(uiState.showSuccessSnackbar) {
            if (uiState.showSuccessSnackbar) {
                snackbarHostState.showSnackbar("Pembayaran Berhasil!")
                viewModel.onHideSnackbar()
            }
        }

        // --- DIALOG KONFIRMASI PEMBAYARAN (Tetap Sama) ---
        if (uiState.showPaymentConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.onShowPaymentConfirmation(false) },
                title = { Text("Konfirmasi Pembayaran") },
                text = {
                    val msg = if(uiState.selectedPaymentMethod == "Tunai") "Terima uang: ${formatCurrency(tempAmountPaidInput)}?" else "Proses pembayaran ${uiState.selectedPaymentMethod}?"
                    Text(msg)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.onSaveOrder(isPaid = true, navController = navController, context = context, amountPaidInput = tempAmountPaidInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Ya, Bayar") }
                },
                dismissButton = { TextButton(onClick = { viewModel.onShowPaymentConfirmation(false) }) { Text("Batal") } }
            )
        }


        // --- BOTTOM SHEET 2: TAMBAH PELANGGAN BARU (LANGSUNG INI) ---
        if (uiState.showAddCustomerSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onShowAddCustomerSheet(false) },
                sheetState = addCustomerSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                AddCustomerSheetContent(
                    name = uiState.newCustomerName,
                    phone = uiState.newCustomerPhone,
                    address = uiState.newCustomerAddress,
                    onNameChange = viewModel::onNewCustomerNameChange,
                    onPhoneChange = viewModel::onNewCustomerPhoneChange,
                    onAddressChange = viewModel::onNewCustomerAddressChange,
                    onSave = { viewModel.onSaveNewCustomer() }
                )
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = { Text("Ringkasan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                SummaryActions(
                    subtotal = uiState.subtotal,
                    discount = uiState.totalDiscount,
                    total = uiState.total,
                    onPayLater = { viewModel.onSaveOrder(isPaid = false, navController = navController, context = context, amountPaidInput = 0.0) },
                    onPayNow = { showPaymentSheet = true }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // ... (Item Cart tetap sama) ...
                    items(uiState.cart.entries.toList(), key = { (product, _) -> product.id }) { (product, qty) ->
                        SummaryCartItem(
                            product = product,
                            quantity = qty,
                            onIncrease = { viewModel.onIncreaseQuantity(product) },
                            onDecrease = { viewModel.onDecreaseQuantity(product) },
                            onRemove = { viewModel.onRemoveFromCart(product) }
                        )
                    }

                    item { Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) }

                    item {
                        SummaryForm(
                            selectedCustomer = uiState.selectedCustomer,
                            note = uiState.note,
                            useDiscount = uiState.useDiscount,
                            discountAmount = uiState.discountAmount,
                            discountType = uiState.discountType,
                            onDiscountTypeChange = viewModel::onDiscountTypeChange,
                            onCustomerSelected = viewModel::onCustomerSelected,
                            onNoteChange = viewModel::onNoteChange,
                            onDiscountSwitchChange = viewModel::onDiscountSwitchChange,
                            onDiscountAmountChange = viewModel::onDiscountAmountChange,
                            onCustomerFieldClick = { viewModel.onShowAddCustomerSheet(true) }
                        )
                    }
                }
            }
        }

        // --- SHEET PEMBAYARAN (Tetap Sama) ---
        if (showPaymentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPaymentSheet = false },
                sheetState = paymentSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                modifier = Modifier.fillMaxHeight(0.95f).statusBarsPadding().imePadding()
            ) {
                PaymentSheetContent(
                    total = uiState.total,
                    paymentMethods = uiState.availablePaymentMethods,
                    selectedPaymentMethod = uiState.selectedPaymentMethod,
                    onPaymentMethodChange = viewModel::onPaymentMethodSelected,
                    onConfirmPayment = { amount ->
                        tempAmountPaidInput = amount
                        viewModel.onShowPaymentConfirmation(true)
                        showPaymentSheet = false
                    }
                )
            }
        }
    }
}

// ==========================================
// COMPONENTS
// ==========================================

// --- 2. ADD CUSTOMER SHEET CONTENT ---
@Composable
fun AddCustomerSheetContent(
    name: String,
    phone: String,
    address: String, // <-- TAMBAHKAN
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit, // <-- TAMBAHKAN
    onSave: () -> Unit
) {
    // 1. Tambahkan State Scroll
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 2. Aktifkan Scroll Vertikal
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp), // Padding bawah
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tambah Pelanggan",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nama") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("No. Handphone (Opsional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onSave,
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F), // Merah
                contentColor = Color.White
            )
        ) {
            Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
// --- 3. SUMMARY FORM (UPDATED - HAPUS onAddCustomerClick) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryForm(
    selectedCustomer: Customer?, note: String, useDiscount: Boolean, discountAmount: String, discountType: DiscountType,
    onDiscountTypeChange: (DiscountType) -> Unit, onCustomerSelected: (Customer?) -> Unit, onNoteChange: (String) -> Unit, onDiscountSwitchChange: (Boolean) -> Unit, onDiscountAmountChange: (String) -> Unit,
    onCustomerFieldClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // CUSTOMER FIELD
        Column {
            Text("Pelanggan (Opsional)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "", onValueChange = {}, readOnly = true,
                    trailingIcon = {
                        if (selectedCustomer != null) IconButton(onClick = { onCustomerSelected(null) }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.secondary) }
                        else Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                Box(modifier = Modifier.matchParentSize().padding(end = if (selectedCustomer != null) 48.dp else 0.dp).clickable { onCustomerFieldClick() })
            }
        }
        // NOTE
        Column {
            Text("Catatan/No. Meja (Opsional)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(value = note, onValueChange = onNoteChange, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedTextColor = MaterialTheme.colorScheme.onSurface))
        }
        // DISCOUNT
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Diskon",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Switch(
                checked = useDiscount,
                onCheckedChange = onDiscountSwitchChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0), // Abu-abu terang
                    uncheckedThumbColor = Color(0xFF9E9E9E), // Abu-abu sedang
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline // Outline gelap
                )
            )
        }
        if (useDiscount) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onDiscountTypeChange(DiscountType.NOMINAL) }) {
                    RadioButton(selected = discountType == DiscountType.NOMINAL, onClick = { onDiscountTypeChange(DiscountType.NOMINAL) }, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
                    Text("Nominal (Rp)", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onDiscountTypeChange(DiscountType.PERCENTAGE) }) {
                    RadioButton(selected = discountType == DiscountType.PERCENTAGE, onClick = { onDiscountTypeChange(DiscountType.PERCENTAGE) }, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
                    Text("Persen (%)", style = MaterialTheme.typography.bodyMedium)
                }
            }
            OutlinedTextField(
                value = discountAmount, onValueChange = { onDiscountAmountChange(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text(if (discountType == DiscountType.NOMINAL) "Jumlah Diskon (Rp)" else "Persentase Diskon (%)", color = MaterialTheme.colorScheme.secondary) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (discountType == DiscountType.NOMINAL) CurrencyVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                suffix = { if (discountType == DiscountType.PERCENTAGE) { Text("%") } }
            )
        }
    }
}

// ... (Sisa Komponen Helper: CategoryFilter, ProductGridItem, CartSummaryBar, SummaryCartItem, SummaryActions, PaymentSheetContent TETAP SAMA) ...
// ... (Silakan paste dari kode sebelumnya jika perlu, saya potong agar jawaban muat) ...

@Composable
fun CategoryFilter(categories: List<Category>, selectedCategoryId: Int?, onCategoryClick: (Int?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { CategoryChip(selected = selectedCategoryId == null, text = "Semua Kategori", onClick = { onCategoryClick(null) }) }
        items(categories) { category -> CategoryChip(selected = selectedCategoryId == category.id, text = category.name, onClick = { onCategoryClick(category.id) }) }
    }
}

@Composable
private fun CategoryChip(selected: Boolean, text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.height(36.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) { Text(text = text, color = if (selected) Color.White else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium) }
    }
}

@Composable
fun ProductGridItem(
    product: Product,
    quantityInCart: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    val isOutOfStock = !product.isUnlimited && product.stock <= 0
    val canAddMore = product.isUnlimited || product.stock > quantityInCart

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = !isOutOfStock) { if (canAddMore) onIncrease() }
        ) {
            if (product.imageUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fastfood,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp), // Diperbesar (sebelumnya 32.dp)
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                    )
                }
            } else {
                AsyncImage(
                    model = product.imageUri,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = rememberVectorPainter(image = Icons.Default.Image),
                    error = rememberVectorPainter(image = Icons.Default.BrokenImage)
                )
            }

            // --- Overlay Stok & Tombol (Tidak Berubah) ---
            if (isOutOfStock) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(color = Color(0xFFB00020), shape = RoundedCornerShape(4.dp)) {
                        Text(text = "Stok Kosong", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            } else {
                if (quantityInCart > 0) {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), shadowElevation = 4.dp, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(32.dp)) {
                            IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Remove, "Kurang", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                            Text(text = quantityInCart.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = onIncrease, enabled = canAddMore, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, "Tambah", tint = if (canAddMore) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(16.dp)) }
                        }
                    }
                } else {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 8.dp), modifier = Modifier.align(Alignment.BottomEnd).size(40.dp).clickable { onIncrease() }) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Tambah", tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        val textColor = if (isOutOfStock) Color.Gray else MaterialTheme.colorScheme.onSurface
        Text(text = product.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, color = textColor)
        Text(text = formatCurrency(product.price), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
fun CartSummaryBar(cartItemCount: Int, total: Double, onContinueClick: () -> Unit) {
    Surface(shadowElevation = 16.dp, color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary); Text(formatCurrency(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
            Button(onClick = onContinueClick, enabled = cartItemCount > 0, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), modifier = Modifier.width(150.dp)) { Text("Lanjut", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SummaryCartItem(product: Product, quantity: Int, onIncrease: () -> Unit, onDecrease: () -> Unit, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                // Background sama seperti di grid (bukan abu-abu standar)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (product.imageUri.isNullOrBlank()) {
                // Tampilan Icon Default (Fastfood)
                Icon(
                    imageVector = Icons.Default.Fastfood,
                    contentDescription = null,
                    // Ukuran icon disesuaikan agar pas di kotak 60dp
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                )
            } else {
                // Tampilan Gambar Produk
                AsyncImage(
                    model = product.imageUri,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = rememberVectorPainter(image = Icons.Default.Image),
                    error = rememberVectorPainter(image = Icons.Default.BrokenImage)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) { Text(product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface); Text(formatCurrency(product.price), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRemove, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary), contentPadding = PaddingValues(0.dp), modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Delete, "Hapus", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
            Surface(shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), color = Color.Transparent) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(32.dp).clickable { onDecrease() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, "Kurang", tint = MaterialTheme.colorScheme.onSurface) }; Text(text = quantity.toString(), modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Box(modifier = Modifier.size(32.dp).clickable { onIncrease() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Tambah", tint = MaterialTheme.colorScheme.primary) } } }
        }
    }
}

@Composable
private fun SummaryActions(subtotal: Double, discount: Double, total: Double, onPayLater: () -> Unit, onPayNow: () -> Unit) {
    Surface(shadowElevation = 16.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row { Text("Subtotal", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary); Text(formatCurrency(subtotal), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.height(4.dp))
            Row { Text("Diskon", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary); Text("-${formatCurrency(discount)}", color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(12.dp))
            Row { Text("Total", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface); Text(formatCurrency(total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onPayLater, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) { Text("Bayar Nanti", color = MaterialTheme.colorScheme.primary) }
                Button(onClick = onPayNow, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)) { Text("Bayar Sekarang") }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheetContent(
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

        // Tambahan Spacer bawah agar tidak terlalu mepet saat discroll mentok
        Spacer(Modifier.height(24.dp))
    }
}
fun formatCurrency(amount: Double): String { return NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount).replace(",00", "") }