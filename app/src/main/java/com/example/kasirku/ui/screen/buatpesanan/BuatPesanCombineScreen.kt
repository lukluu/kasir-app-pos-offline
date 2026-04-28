package com.example.kasirku.ui.screen.buatpesanan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.kasirku.ui.screen.pesanan.formatCurrency
import java.text.NumberFormat
import java.util.*
import kotlin.math.ceil

// BuatPesananCombinedScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuatPesananCombinedScreen(
    navController: NavController,
    viewModel: CreateOrderViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // State untuk bottomsheet
    var showPaymentSheet by remember { mutableStateOf(false) }
    var tempAmountPaidInput by remember { mutableStateOf(0.0) }
    val addCustomerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val paymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Buat Pesanan",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AppBottomBar(navController = navController)
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Kolom 1: Product Selection (60%)
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
            ) {
                ProductSelectionContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    isCombinedMode = true,
                    navController = navController // Tambahkan navController
                )
            }

            // Vertical Divider
            Divider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )

            // Kolom 2: Order Summary (40%)
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                OrderSummaryContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    navController = navController,
                    isCombinedMode = true,
                    onShowPaymentSheet = { showPaymentSheet = true }
                )
            }
        }
    }

    // --- BOTTOM SHEET: TAMBAH PELANGGAN BARU ---
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

    // --- SHEET PEMBAYARAN ---
    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            sheetState = paymentSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier
                .fillMaxHeight(0.95f)
                .statusBarsPadding()
                .imePadding()
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

    // --- DIALOG KONFIRMASI PEMBAYARAN ---
    if (uiState.showPaymentConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowPaymentConfirmation(false) },
            title = { Text("Konfirmasi Pembayaran") },
            text = {
                val msg = if(uiState.selectedPaymentMethod == "Tunai")
                    "Terima uang: ${formatCurrency(tempAmountPaidInput)}?"
                else
                    "Proses pembayaran ${uiState.selectedPaymentMethod}?"
                Text(msg)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onSaveOrder(
                            isPaid = true,
                            navController = navController,
                            context = context,
                            amountPaidInput = tempAmountPaidInput
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Ya, Bayar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowPaymentConfirmation(false) }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ProductSelectionContent(
    uiState: CreateOrderUiState,
    viewModel: CreateOrderViewModel,
    isCombinedMode: Boolean = false,
    navController: NavController // Tambahkan parameter navController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp)
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
                        fontSize = 14.sp,
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
                                    fontSize = 14.sp,
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

        // Category Filter
        CategoryFilter(
            categories = uiState.allCategories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategoryClick = viewModel::onCategorySelected
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Product Grid atau Empty State
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredProducts.isEmpty()) {
            // TAMPILAN KOSONG DENGAN TOMBOL BUAT MENU
            EmptyProductState(
                modifier = Modifier.weight(1f),
                navController = navController
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
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

        // Cart Summary Bar (hanya untuk portrait mode)
        if (!isCombinedMode && uiState.cart.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)) {
                CartSummaryBar(
                    cartItemCount = uiState.cart.values.sum(),
                    total = uiState.total,
                    onContinueClick = { /* Tidak digunakan di combined mode */ }
                )
            }
        }
    }
}

// COMPOSABLE BARU: Empty Product State
@Composable
private fun EmptyProductState(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon besar
        Icon(
            imageVector = Icons.Default.RestaurantMenu,
            contentDescription = "Tidak ada produk",
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Judul
        Text(
            text = "Belum Ada Menu",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Deskripsi
        Text(
            text = "Anda belum memiliki produk atau menu yang tersedia. Tambahkan produk terlebih dahulu untuk mulai membuat pesanan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Buat Menu
        Button(
            onClick = {
                // Navigasi ke halaman tambah produk
                navController.navigate(Screen.AddEditProduct.createRoute(null))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Produk",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Buat Menu Pertama",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tombol Kelola Produk (secondary)
        OutlinedButton(
            onClick = {
                // Navigasi ke halaman daftar produk
                navController.navigate(Screen.Product.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = "Kelola Produk",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Kelola Semua Produk",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OrderSummaryContent(
    uiState: CreateOrderUiState,
    viewModel: CreateOrderViewModel,
    navController: NavController,
    isCombinedMode: Boolean = false,
    onShowPaymentSheet: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp, 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Ringkasan Pesanan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (uiState.cart.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Item Pesanan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                items(uiState.cart.entries.toList(), key = { (product, _) -> product.id }) { (product, qty) ->
                    SummaryCartItemCompact(
                        product = product,
                        quantity = qty,
                        onRemove = { viewModel.onRemoveFromCart(product) }
                    )
                }

                item {
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                item {
                    SummaryFormCompact(
                        selectedCustomer = uiState.selectedCustomer,
                        note = uiState.note,
                        useDiscount = uiState.useDiscount,
                        discountAmount = uiState.discountAmount,
                        discountType = uiState.discountType,
                        subtotal = uiState.subtotal,
                        discount = uiState.totalDiscount,
                        total = uiState.total,
                        onDiscountTypeChange = viewModel::onDiscountTypeChange,
                        onCustomerSelected = viewModel::onCustomerSelected,
                        onNoteChange = viewModel::onNoteChange,
                        onDiscountSwitchChange = viewModel::onDiscountSwitchChange,
                        onDiscountAmountChange = viewModel::onDiscountAmountChange,
                        onCustomerFieldClick = { viewModel.onShowAddCustomerSheet(true) },
                        onPayNow = onShowPaymentSheet,
                        onPayLater = {
                            viewModel.onSaveOrder(
                                isPaid = false,
                                navController = navController,
                                context = context,
                                amountPaidInput = 0.0
                            )
                        }
                    )
                }

            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Keranjang kosong",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Belum ada item di keranjang",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Pilih produk dari menu sebelah kiri",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ... (kode SummaryCartItemCompact, SummaryFormCompact, dan fungsi lainnya tetap sama)
@Composable
private fun SummaryCartItemCompact(
    product: Product,
    quantity: Int,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (product.imageUri.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.Fastfood,
                    contentDescription = null,
                    modifier = Modifier.size(25.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                )
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
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    formatCurrency(product.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text("•", color = MaterialTheme.colorScheme.outline)

                Text(
                    "Qty: $quantity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Text("•", color = MaterialTheme.colorScheme.outline)
                Text(
                    formatCurrency(product.price * quantity),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        OutlinedButton(
            onClick = onRemove,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Outlined.Delete,
                "Hapus",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SummaryFormCompact(
    selectedCustomer: Customer?,
    note: String,
    useDiscount: Boolean,
    discountAmount: String,
    discountType: DiscountType,
    subtotal: Double,
    discount: Double,
    total: Double,
    onDiscountTypeChange: (DiscountType) -> Unit,
    onCustomerSelected: (Customer?) -> Unit,
    onNoteChange: (String) -> Unit,
    onDiscountSwitchChange: (Boolean) -> Unit,
    onDiscountAmountChange: (String) -> Unit,
    onCustomerFieldClick: () -> Unit,
    onPayNow: () -> Unit,
    onPayLater: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(formatCurrency(subtotal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Diskon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text("-${formatCurrency(discount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    formatCurrency(total),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column {
            Text("Pelanggan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        if (selectedCustomer != null) {
                            IconButton(onClick = { onCustomerSelected(null) }) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(end = if (selectedCustomer != null) 48.dp else 0.dp)
                        .clickable { onCustomerFieldClick() }
                )
            }
        }

        Column {
            Text("Catatan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                placeholder = { Text("Catatan pesanan") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Diskon",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = useDiscount,
                onCheckedChange = onDiscountSwitchChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0),
                    uncheckedThumbColor = Color(0xFF9E9E9E)
                )
            )
        }

        if (useDiscount) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDiscountTypeChange(DiscountType.NOMINAL) }
                ) {
                    RadioButton(
                        selected = discountType == DiscountType.NOMINAL,
                        onClick = { onDiscountTypeChange(DiscountType.NOMINAL) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("Nominal", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDiscountTypeChange(DiscountType.PERCENTAGE) }
                ) {
                    RadioButton(
                        selected = discountType == DiscountType.PERCENTAGE,
                        onClick = { onDiscountTypeChange(DiscountType.PERCENTAGE) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("Persen", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedTextField(
                value = discountAmount,
                onValueChange = { onDiscountAmountChange(it.filter { c -> c.isDigit() || c == '.' }) },
                placeholder = {
                    Text(
                        if (discountType == DiscountType.NOMINAL) "Jumlah diskon" else "Persentase diskon"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (discountType == DiscountType.NOMINAL) CurrencyVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                suffix = { if (discountType == DiscountType.PERCENTAGE) { Text("%") } }
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPayLater,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Bayar Nanti", style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onPayNow,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Bayar", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color= Color.White)
            }
        }
    }
}