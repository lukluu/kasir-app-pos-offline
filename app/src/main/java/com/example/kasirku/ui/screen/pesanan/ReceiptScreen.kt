package com.example.kasirku.ui.screen.pesanan

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.kasirku.domain.model.Order
import com.example.kasirku.ui.utils.ReceiptImageGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    navController: NavController,
    viewModel: ReceiptViewModel
) {
    val order by viewModel.order.collectAsState()
    val outletData by viewModel.outletData.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State untuk print dan share
    var isPrinting by remember { mutableStateOf(false) }
    var isSharingImage by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // FONT KHUSUS STRUK (Monospace)
    val receiptFont = FontFamily.Monospace

    // WARNA ADAPTIF TEMA
    val receiptBackground = MaterialTheme.colorScheme.surface
    val receiptTextPrimary = MaterialTheme.colorScheme.onSurface
    val receiptTextSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val receiptDivider = MaterialTheme.colorScheme.outlineVariant
    var showReconnectDialog by remember { mutableStateOf(false) }
    var isReconnecting by remember { mutableStateOf(false) }
    // --- FUNGSI PRINT RECEIPT ---
    fun printReceipt() {
        if (order == null) return

        println("DEBUG: Print button clicked")

        val isConnected = viewModel.isPrinterConnected()
        println("DEBUG: Initial connection check: $isConnected")

        if (!isConnected) {
            scope.launch {
                // Tampilkan dialog reconnect option
                showReconnectDialog = true
            }
            return
        }

        isPrinting = true
        scope.launch {
            try {
                println("DEBUG: Starting print process...")
                val success = viewModel.printReceipt(order!!)
                println("DEBUG: Print process completed: $success")

                if (success) {
                    snackbarHostState.showSnackbar("Nota berhasil dicetak!")
                } else {
                    snackbarHostState.showSnackbar("Gagal mencetak. Coba sambung ulang printer.")
                }
            } catch (e: Exception) {
                println("DEBUG: Print exception: ${e.message}")
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isPrinting = false
            }
        }
    }
    fun manualReconnectPrinter() {
        isReconnecting = true
        scope.launch {
            val success = viewModel.forceReconnectPrinter()
            if (success) {
                snackbarHostState.showSnackbar("Printer berhasil tersambung ulang!")
            } else {
                snackbarHostState.showSnackbar("Gagal menyambung ulang printer")
            }
            isReconnecting = false
            showReconnectDialog = false
        }
    }
    // DIALOG RECONNECT
    if (showReconnectDialog) {
        AlertDialog(
            onDismissRequest = { showReconnectDialog = false },
            title = { Text("Printer Tidak Terhubung") },
            text = {
                Text("Printer aktif tidak terhubung. Sambung ulang ke printer?")
            },
            confirmButton = {
                Button(
                    onClick = { manualReconnectPrinter() },
                    enabled = !isReconnecting
                ) {
                    if (isReconnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Menyambung...")
                    } else {
                        Text("Sambung Ulang")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showReconnectDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
    val activePrinterMac = remember { viewModel.getActivePrinterMac() }

    // GANTI bagian ini:
    if (activePrinterMac != null) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isConnected = viewModel.isPrinterConnected()
                Icon(
                    Icons.Default.Print,
                    null,
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Status Printer: ${if (isConnected) "Terhubung" else "Terputus"}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "MAC: ${activePrinterMac.take(12)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isConnected) {
                    TextButton(onClick = { showReconnectDialog = true }) {
                        Text("Sambung")
                    }
                }
            }
        }
    }

    fun shareReceiptAsImage(item: Order) {
        isSharingImage = true

        scope.launch {
            try {
                println("DEBUG: Creating receipt bitmap with improved generator...")

                // GUNAKAN IMAGE GENERATOR DENGAN DATA OUTLET
                val bitmap = ReceiptImageGenerator.createReceiptBitmap(item, outletData)

                println("DEBUG: Improved bitmap created, size: ${bitmap.width}x${bitmap.height}")

                // Simpan dengan kualitas lebih baik
                val fileName = "receipt_${System.currentTimeMillis()}.png"
                val file = File(context.cacheDir, fileName)

                println("DEBUG: Saving to file: ${file.absolutePath}")

                // Pastikan directory exists
                file.parentFile?.mkdirs()

                FileOutputStream(file).use { stream ->
                    val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    println("DEBUG: Bitmap compress result: $success, file size: ${file.length()} bytes")
                }

                // Validasi file
                if (file.exists() && file.length() > 1000) {
                    println("DEBUG: File created successfully, size: ${file.length()} bytes")

                    // Dapatkan URI
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )

                    println("DEBUG: URI created: $uri")

                    // Share dengan intent yang lebih spesifik
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "image/png"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_SUBJECT, "Nota Pesanan ${item.orderNumber}")
                        putExtra(Intent.EXTRA_TEXT, "Nota Pesanan ${item.orderNumber} dari ${outletData?.name ?: "Toko KasirKu"}\nTanggal: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(item.createdAt)}")
                    }

                    val shareImageIntent = Intent.createChooser(shareIntent, "Bagikan Nota sebagai Gambar")
                    shareImageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(shareImageIntent)

                    snackbarHostState.showSnackbar("Gambar nota berhasil dibuat dan siap dibagikan!")
                } else {
                    println("DEBUG: File creation failed or too small")
                    snackbarHostState.showSnackbar("Gagal membuat gambar nota (file terlalu kecil)")
                }

            } catch (e: Exception) {
                println("DEBUG: Error in shareReceiptAsImage: ${e.message}")
                e.printStackTrace()
                snackbarHostState.showSnackbar("Error membuat gambar: ${e.localizedMessage}")
            } finally {
                isSharingImage = false
                println("DEBUG: shareReceiptAsImage completed")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Nota Pesanan", color = receiptTextPrimary, fontWeight = FontWeight.Bold, fontFamily = receiptFont)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = receiptTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = receiptBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = receiptBackground
    ) { padding ->
        if (order == null) return@Scaffold
        val item = order!!
        val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(item.createdAt)

        // Ambil data pelanggan dengan default '-'
        val customerName = item.customer?.name.orEmpty().ifBlank { "-" }
        val customerPhone = item.customer?.phoneNumber.orEmpty().ifBlank { "-" }
        val noteContent = item.note.orEmpty().ifBlank { "-" }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // --- HEADER TOKO ---
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            outletData?.name ?: "Toko KasirKu",
                            color = receiptTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = receiptFont
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${outletData?.address ?: "Jl. Contoh No. 123"} - ${outletData?.phone ?: "08123456789"}",
                            color = receiptTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = receiptFont
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Divider(color = receiptDivider, thickness = 1.dp, modifier = Modifier.fillMaxWidth())
                }

                // --- INFO ORDER ---
                item {
                    ReceiptRow("No. Pesanan", item.orderNumber, receiptTextSecondary, receiptTextPrimary, receiptFont)
                    ReceiptRow("Waktu", date, receiptTextSecondary, receiptTextPrimary, receiptFont)

                    Spacer(Modifier.height(4.dp))

                    // Selalu tampilkan Nama & HP
                    ReceiptRow("Pelanggan", customerName, receiptTextSecondary, receiptTextPrimary, receiptFont)
                    ReceiptRow("No. HP", customerPhone, receiptTextSecondary, receiptTextPrimary, receiptFont)

                    // Selalu tampilkan Catatan
                    if (noteContent != "-") {
                        Spacer(Modifier.height(4.dp))
                        Text("Catatan:", color = receiptTextSecondary, fontSize = 12.sp, fontFamily = receiptFont)
                        Text(noteContent,
                            color = receiptTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = receiptFont,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Divider(color = receiptDivider, thickness = 1.dp, modifier = Modifier.fillMaxWidth())
                }

                // --- PRODUK ---
                item {
                    Text("Produk",
                        color = receiptTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = receiptFont,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(item.items) { orderItem ->
                    Column(Modifier.padding(vertical = 2.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                orderItem.product.name,
                                color = receiptTextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontFamily = receiptFont,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                formatCurrency(orderItem.priceAtSale * orderItem.quantity),
                                color = receiptTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = receiptFont
                            )
                        }
                        Text(
                            "${orderItem.quantity} x ${formatCurrency(orderItem.priceAtSale)}",
                            color = receiptTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = receiptFont
                        )
                    }
                }

                // --- TOTAL & PEMBAYARAN ---
                item {
                    Spacer(Modifier.height(4.dp))
                    Divider(color = receiptDivider, thickness = 1.dp, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))

                    // Subtotal & Diskon
                    ReceiptRow("Subtotal", formatCurrency(item.totalPrice + item.discount), receiptTextSecondary, receiptTextPrimary, receiptFont)
                    if (item.discount > 0) {
                        ReceiptRow("Diskon", "-${formatCurrency(item.discount)}", receiptTextSecondary, MaterialTheme.colorScheme.error, receiptFont)
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", color = receiptTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = receiptFont)
                        Text(formatCurrency(item.totalPrice), color = receiptTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = receiptFont)
                    }

                    Spacer(Modifier.height(8.dp))

                    Column(Modifier.fillMaxWidth()) {
                        Text("Pembayaran", color = receiptTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = receiptFont)
                        Spacer(Modifier.height(2.dp))

                        val paymentMethod = if (item.paymentStatus == "PAID") {
                            if (item.paymentMethod.isNullOrBlank()) "Tunai" else item.paymentMethod
                        } else {
                            "-"
                        }

                        // Metode Bayar
                        ReceiptRow("Metode", paymentMethod, receiptTextPrimary, receiptTextPrimary, receiptFont)

                        // Uang Diterima & Kembalian (Hanya tampil jika LUNAS dan ada amountPaid > 0)
                        if (item.paymentStatus == "PAID" && item.amountPaid > 0) {
                            val change = (item.amountPaid - item.totalPrice).coerceAtLeast(0.0)
                            ReceiptRow("Diterima", formatCurrency(item.amountPaid), receiptTextSecondary, receiptTextPrimary, receiptFont)
                            ReceiptRow("Kembalian", formatCurrency(change), receiptTextSecondary, receiptTextPrimary, receiptFont)
                        }

                        Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            val statusText = when(item.paymentStatus) {
                                "PAID" -> "Lunas"
                                "CANCELLED" -> "Dibatalkan"
                                else -> "Belum Dibayar"
                            }
                            val statusColor = when(item.paymentStatus) {
                                "PAID" -> MaterialTheme.colorScheme.primary
                                "CANCELLED" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.error
                            }

                            Text("Status", color = receiptTextPrimary, fontFamily = receiptFont, fontSize = 14.sp)
                            Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontFamily = receiptFont, fontSize = 14.sp)
                        }
                    }

                    if (item.paymentStatus == "CANCELLED" && !item.cancelReason.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Ket: ${item.cancelReason}",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontFamily = receiptFont,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    CenterText(outletData?.footerNote ?: "Terima Kasih Telah Memesan !😊", receiptTextSecondary, receiptFont)
                    CenterText("Silahkan datang kembali", receiptTextSecondary, receiptFont)

                }
            }

            // --- BOTTOM BUTTONS ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Tombol Bagikan Gambar (Kecil, Icon Share)
                OutlinedButton(
                    onClick = { shareReceiptAsImage(item) },
                    modifier = Modifier.size(50.dp), // Ukuran Kecil 50x50
                    enabled = !isSharingImage,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(0.dp), // Hilangkan padding internal
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = receiptTextPrimary
                    )
                ) {
                    if (isSharingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        // Icon Share
                        Icon(Icons.Default.Share, contentDescription = "Bagikan Gambar", modifier = Modifier.size(24.dp))
                    }
                }

                // 2. Tombol Cetak (Mengambil sisa lebar)
                Button(
                    onClick = { printReceipt() },
                    modifier = Modifier
                        .weight(1f) // Mengambil sisa lebar
                        .height(50.dp),
                    enabled = !isPrinting,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Mencetak...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Print, contentDescription = "Cetak", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cetak Nota", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String, labelColor: Color, valueColor: Color, font: FontFamily) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = labelColor, fontSize = 14.sp, fontFamily = font)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, fontFamily = font)
    }
}

@Composable
fun CenterText(text: String, color: Color, font: FontFamily) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontFamily = font,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
