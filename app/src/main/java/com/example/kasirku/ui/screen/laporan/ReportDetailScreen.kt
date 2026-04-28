package com.example.kasirku.ui.screen.laporan

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.kasirku.ui.screen.pesanan.formatCurrency
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    navController: NavController,
    viewModel: ReportDetailViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var isPickingStartDate by remember { mutableStateOf(true) }
    val datePickerState = rememberDatePickerState()

    val title = when(state.reportType) {
        "products" -> "Laporan Produk Terlaris"
        "cancelled" -> "Laporan Pesanan Batal"
        "cashless" -> "Laporan Pembayaran Non Tunai"
        else -> "Laporan Pesanan"
    }

    // LOGIKA TAMPILAN TANGGAL
    // Jika startDate 0, tampilkan "Awal Waktu" atau sejenisnya
    val startDateStr = if (state.startDate == 0L) "Awal" else SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(state.startDate))
    val endDateStr = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(state.endDate))

    // ... (Export Logic SAMA) ...
    var csvContentToExport by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream -> stream.write(csvContentToExport.toByteArray()) }
                Toast.makeText(context, "Berhasil disimpan!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    fun startExport() {
        val sb = StringBuilder()
        if (state.reportType == "products") {
            sb.append("No,Nama Produk,Harga,Terjual\n")
            state.bestSellers.forEachIndexed { i, (p, q) -> sb.append("${i+1},\"${p.name}\",${p.price},$q\n") }
        } else {
            sb.append("No,Tanggal,No Pesanan,Pelanggan,Status,Metode,Total\n")
            state.orders.forEachIndexed { i, o ->
                val status = when(o.paymentStatus) { "PAID" -> "Lunas"; "UNPAID" -> "Belum Bayar"; else -> "Batal" }
                val method = o.paymentMethod ?: "-"
                sb.append("${i+1},\"${SimpleDateFormat("dd/MM/yy HH:mm").format(o.createdAt)}\",${o.orderNumber},\"${o.customer?.name ?: "-"}\",$status,$method,${o.totalPrice.toLong()}\n")
            }
        }
        csvContentToExport = sb.toString()
        exportLauncher.launch("Laporan_${state.reportType}_${System.currentTimeMillis()}.csv")
    }

    // ... (Dialog DatePicker SAMA) ...
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        if (isPickingStartDate) viewModel.onDateRangeChanged(selectedDate, state.endDate)
                        else viewModel.onDateRangeChanged(state.startDate, getEndOfDayTimestamp(selectedDate))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val label = if(state.reportType == "products") "Total (${state.bestSellers.size} Item)" else "Total (${state.totalCount} Pesanan)"
                        val value = if(state.reportType == "products") "${state.totalCount} Pcs" else formatCurrency(state.totalAmount)
                        Text(label, style = MaterialTheme.typography.bodySmall)
                        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { startExport() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                        Text("Export Excel", color = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            // 1. FILTER TANGGAL
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { isPickingStartDate = true; datePickerState.selectedDateMillis = if(state.startDate == 0L) System.currentTimeMillis() else state.startDate; showDatePicker = true },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                    Text(startDateStr, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { isPickingStartDate = false; datePickerState.selectedDateMillis = state.endDate; showDatePicker = true },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                    Text(endDateStr, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- FILTER DROPDOWNS (Hanya jika reportType == "orders") ---
            if (state.reportType == "orders") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // ... (Filter Status & Metode SAMA SEPERTI SEBELUMNYA) ...
                    // (Copy paste kode filter dropdown dari jawaban sebelumnya)
                    // A. FILTER STATUS
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = state.selectedFilterStatus,
                                onValueChange = {}, readOnly = true, label = { Text("Status", fontSize = 12.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedBorderColor = MaterialTheme.colorScheme.primary),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Semua", "Dibayar", "Belum Dibayar", "Batal").forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { viewModel.onFilterStatusChanged(option); expanded = false })
                                }
                            }
                        }
                    }

                    // B. FILTER METODE
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = state.selectedPaymentMethodFilter,
                                onValueChange = {}, readOnly = true, label = { Text("Metode", fontSize = 12.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedBorderColor = MaterialTheme.colorScheme.primary),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                state.availablePaymentMethods.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { viewModel.onFilterPaymentMethodChanged(option); expanded = false })
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 3. TOMBOL CARI
            Button(
                onClick = { viewModel.loadData() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Cari / Refresh", color = Color.White) }

            Spacer(Modifier.height(16.dp))
            Divider()

            // 4. LIST DATA
            // ... (Kode List Data SAMA SEPERTI SEBELUMNYA) ...
            // ...
            if (state.reportType == "products") {
                if (state.bestSellers.isEmpty()) EmptyState() else {
                    LazyColumn {
                        items(state.bestSellers) { (p, q) ->
                            ListItem(headlineContent = { Text(p.name) }, trailingContent = { Text("$q pcs", fontWeight = FontWeight.Bold) })
                            Divider()
                        }
                    }
                }
            } else {
                if (state.orders.isEmpty()) EmptyState()
                else {
                    LazyColumn {
                        items(state.orders) { order ->
                            val date = SimpleDateFormat("dd MMM HH:mm", Locale("id")).format(order.createdAt)
                            val statusColor = when(order.paymentStatus) {
                                "PAID" -> Color(0xFF4CAF50); "CANCELLED" -> Color.Gray; else -> Color(0xFFE64A19)
                            }

                            ListItem(
                                headlineContent = {
                                    Text(order.orderNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                },
                                supportingContent = {
                                    Column {
                                        Text(order.customer?.name ?: "Tanpa Nama", fontSize = 12.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = when(order.paymentStatus) { "PAID" -> "Lunas"; "CANCELLED" -> "Batal"; else -> "Belum Bayar" },
                                                fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold
                                            )
                                            if (!order.paymentMethod.isNullOrBlank()) {
                                                Text("| ${order.paymentMethod}", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                },
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(date, fontSize = 10.sp, color = Color.Gray)
                                        Text(formatCurrency(order.totalPrice), fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
// ... helper function sama ...
fun getEndOfDayTimestamp(dateMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
    return calendar.timeInMillis
}
@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Description, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Text("Laporan Kosong", color = Color.Gray)
        }
    }
}