package com.example.kasirku.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavItem(Screen.Dashboard.route, "Dashboard", Icons.Outlined.Dashboard)
    object Pesanan : BottomNavItem(Screen.Pesanan.route, "Pesanan", Icons.Outlined.ReceiptLong)
    object Laporan : BottomNavItem(Screen.Laporan.route, "Laporan", Icons.Outlined.Assessment)
    object Pengaturan : BottomNavItem(Screen.Pengaturan.route, "Pengaturan", Icons.Outlined.Settings)

    object BuatPesanan : BottomNavItem(Screen.ProductSelection.route, "Buat Pesanan", Icons.Default.Add)
}