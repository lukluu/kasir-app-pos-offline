package com.example.kasirku.ui.components // <--- PASTIKAN INI SAMA

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.kasirku.ui.navigation.BottomNavItem
import com.example.kasirku.ui.navigation.Screen

@Composable
fun AppBottomBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Pesanan,
        BottomNavItem.BuatPesanan,
        BottomNavItem.Laporan,
        BottomNavItem.Pengaturan
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            // Ganti BottomAppBar dengan Surface dan atur height secara explicit
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
                modifier = Modifier.height(56.dp) // Atur tinggi sesuai kebutuhan
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item menu
                    items.forEachIndexed { index, item ->
                        if (index == 2) {
                            // Spacer untuk tombol tengah
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            BottomAppBarItem(
                                item = item,
                                selected = currentRoute == item.route,
                                onClick = { navController.navigate(item.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Tombol tengah
        Box(
            modifier = Modifier
                .offset(y = (-18).dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                .clickable { navController.navigate(Screen.CreateOrderGraph.route) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = BottomNavItem.BuatPesanan.icon,
                contentDescription = "Buat Pesanan",
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun BottomAppBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = item.title,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp
        )
    }
}