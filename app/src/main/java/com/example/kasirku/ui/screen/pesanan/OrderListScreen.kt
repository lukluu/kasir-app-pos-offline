package com.example.kasirku.ui.screen.pesanan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.kasirku.domain.model.Order
import com.example.kasirku.ui.components.AppBottomBar
import com.example.kasirku.ui.navigation.Screen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    navController: NavController,
    viewModel: OrderListViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val filters = listOf("Semua", "Dibayar", "Belum Dibayar", "Dibatalkan")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pesanan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = { AppBottomBar(navController) }
    ) { padding ->
        // PERBAIKAN: Gunakan LazyColumn sebagai parent container utama
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            // contentPadding untuk memberikan ruang di atas dan bawah list
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // --- BAGIAN HEADER (Search, Filter, Total) ---
            // Kita masukkan sebagai 'item' tunggal agar ikut ter-scroll
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp) // Padding atas tambahan
                ) {
                    // 1. SEARCH BAR
// 1. SEARCH BAR - CUSTOM DENGAN THEME COMPLETE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(38.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp)
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = state.searchQuery,
                                    onValueChange = viewModel::onSearchChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    visualTransformation = VisualTransformation.None,
                                    interactionSource = interactionSource,
                                    decorationBox = { innerTextField ->
                                        // Placeholder
                                        if (state.searchQuery.isEmpty()) {
                                            Text(
                                                "Cari nomor pesanan...",
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        // Text input
                                        innerTextField()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 2. FILTER BUTTONS
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filters) { filter ->
                            FilterButton(
                                text = filter,
                                isSelected = state.selectedFilter == filter,
                                onClick = { viewModel.onFilterChange(filter) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 3. TOTAL SUMMARY
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(formatCurrency(state.totalRevenue), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }

            // --- BAGIAN LIST ITEMS ---
            items(state.orders) { order ->
                // Bungkus OrderCard dengan padding agar rapi
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OrderCard(order) {
                        navController.navigate(Screen.OrderDetail.createRoute(order.id))
                    }
                }
            }
        }
    }
}

// ... (Komponen OrderCard dan FilterButton TETAP SAMA, tidak perlu diubah) ...

@Composable
fun OrderCard(order: Order, onClick: () -> Unit) {
    // ... (Kode OrderCard SAMA) ...
    // --- LOGIKA WARNA STATUS ---
    val isPaid = order.paymentStatus == "PAID"
    val isCancelled = order.paymentStatus == "CANCELLED"
    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(order.createdAt)
    val badgeColor = when {
        isPaid -> Color(0xFF4CAF50); isCancelled -> Color.Gray; else -> Color(0xFFE64A19)
    }
    val statusText = when {
        isPaid -> "Dibayar"; isCancelled -> "Dibatalkan"; else -> "Belum Dibayar"
    }
    val cardAlpha = if (isCancelled) 0.8f else 1f

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha)),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.orderNumber, color = if (isCancelled) Color.Gray else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = if(isCancelled) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge)
                Text(date, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.customer?.name ?: "-", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!order.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(end = 8.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Catatan: ${order.note}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatCurrency(order.totalPrice), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isCancelled) Color.Gray else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.background(badgeColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(text = statusText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
    val border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    Surface(onClick = onClick, modifier = modifier.height(40.dp), shape = RoundedCornerShape(8.dp), color = containerColor, border = border, shadowElevation = if (isSelected) 2.dp else 0.dp) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text = text, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}