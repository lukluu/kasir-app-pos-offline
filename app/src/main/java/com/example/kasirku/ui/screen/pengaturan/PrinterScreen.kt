package com.example.kasirku.ui.screen.pengaturan

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kasirku.ui.utils.PrinterHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreen(navController: NavController) {
    val context = LocalContext.current
    val printerHelper = remember { PrinterHelper(context) }
    val scope = rememberCoroutineScope()

    // State
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var activePrinterMac by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectingMac by remember { mutableStateOf<String?>(null) }

    // State Refresh
    var isRefreshing by remember { mutableStateOf(false) }

    // Fungsi Refresh
    fun refreshDevices() {
        scope.launch {
            isRefreshing = true
            // Simulasi delay sebentar agar indikator terlihat
            delay(1000)
            if (printerHelper.isBluetoothEnabled()) {
                pairedDevices = printerHelper.getPairedDevices()
            }
            activePrinterMac = printerHelper.getActivePrinterMac()
            isRefreshing = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            refreshDevices()
        } else {
            Toast.makeText(context, "Izin Bluetooth diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    // Load Awal
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        } else {
            refreshDevices()
        }
    }

    // DI PrinterScreen - UPDATE fungsi connectToPrinter
    fun connectToPrinter(device: BluetoothDevice) {
        scope.launch {
            isConnecting = true
            connectingMac = device.address

            // TEST dulu koneksi basic
            val testSuccess = printerHelper.connectAndTest(device)

            if (testSuccess) {
                // KALAU TEST BERHASIL, CONNECT UNTUK PRINT
                val connectSuccess = printerHelper.connectToPrinter(device.address)

                if (connectSuccess) {
                    printerHelper.saveActivePrinter(device.address)
                    activePrinterMac = device.address
                    Toast.makeText(context, "Berhasil terhubung ke ${device.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal terhubung ke printer", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Gagal terhubung. Pastikan printer nyala.", Toast.LENGTH_SHORT).show()
            }

            isConnecting = false
            connectingMac = null
        }
    }

    fun disconnectPrinter() {
        printerHelper.clearActivePrinter()
        activePrinterMac = null
        Toast.makeText(context, "Printer diputuskan", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Printers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Tambah Printer (Buka Pengaturan)", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    ) { padding ->

        // --- PULL TO REFRESH BOX ---
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshDevices() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!printerHelper.isBluetoothEnabled()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Bluetooth, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            Text("Bluetooth mati. Silakan aktifkan.", color = Color.Gray)
                            Button(onClick = { printerHelper.enableBluetooth() }) { Text("Aktifkan") }
                        }
                    }
                } else {
                    // Status Info (Printer Aktif)
                    if (activePrinterMac != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Print, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Printer Aktif", style = MaterialTheme.typography.labelMedium)
                                    val activeName = pairedDevices.find { it.address == activePrinterMac }?.name ?: "Device Tersimpan"
                                    Text(activeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }


                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pairedDevices) { device ->
                            val isActive = device.address == activePrinterMac
                            val isThisConnecting = isConnecting && connectingMac == device.address

                            ListItem(
                                headlineContent = { Text(device.name ?: "Unknown Device", fontWeight = FontWeight.Medium) },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        null,
                                        tint = if(isActive) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                },
                                trailingContent = {
                                    if (isThisConnecting) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        if (isActive) {
                                            OutlinedButton(
                                                onClick = { disconnectPrinter() },
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Text("Putuskan")
                                            }
                                        } else {
                                            Button(
                                                onClick = { connectToPrinter(device) },
                                                enabled = !isConnecting
                                            ) {
                                                Text("Hubungkan")
                                            }
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }

                        // Empty State jika tidak ada device
                        if (pairedDevices.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("Tidak ada perangkat. Pastikan printer sudah di-pairing di pengaturan Bluetooth HP.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}