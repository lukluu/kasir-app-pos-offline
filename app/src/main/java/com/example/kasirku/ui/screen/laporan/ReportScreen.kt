package com.example.kasirku.ui.screen.laporan


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kasirku.ui.components.AppBottomBar
import com.example.kasirku.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Laporan", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = { AppBottomBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)
        ) {
            ReportMenuItem("Laporan Pesanan", Icons.Outlined.ReceiptLong) {
                navController.navigate(Screen.ReportDetail.createRoute("orders"))
            }
            ReportMenuItem("Laporan Produk Terlaris", Icons.Outlined.ShoppingBag) {
                navController.navigate(Screen.ReportDetail.createRoute("products"))
            }
            ReportMenuItem("Laporan Pembayaran Non Tunai", Icons.Outlined.CreditCard) {
                navController.navigate(Screen.ReportDetail.createRoute("cashless")) // Bisa reuse logic orders
            }
            ReportMenuItem("Laporan Pesanan Batal", Icons.Outlined.Cancel) {
                navController.navigate(Screen.ReportDetail.createRoute("cancelled"))
            }
        }
    }
}

@Composable
fun ReportMenuItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Column {
        ListItem(
            headlineContent = { Text(title) },
            leadingContent = { Icon(icon, null) },
            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
            modifier = Modifier.clickable(onClick = onClick)
        )
        Divider()
    }
}