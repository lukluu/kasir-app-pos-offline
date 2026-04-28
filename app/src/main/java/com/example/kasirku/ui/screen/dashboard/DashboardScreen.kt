package com.example.kasirku.ui.screen.dashboard

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.kasirku.ui.navigation.Screen
import com.example.kasirku.ui.viewmodel.ChartFilter
import com.example.kasirku.ui.viewmodel.DashboardState
import com.example.kasirku.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    // Deteksi Orientasi Layar
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val lifecycleOwner = LocalLifecycleOwner.current
    var backPressedTime by remember { mutableStateOf(0L) }
    // Timer untuk reset back press
    BackHandler(enabled = true) {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            // Keluar aplikasi jika interval < 2 detik
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    (context as? Activity)?.finishAndRemoveTask()
                } else {
                    (context as? Activity)?.finishAffinity()
                }
            }
        } else {
            // Tampilkan toast
            android.widget.Toast.makeText(
                context,
                "Tekan sekali lagi untuk keluar",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        backPressedTime = System.currentTimeMillis()
    }
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        if (isLandscape) {
            DashboardContentLandscape(
                navController = navController,
                state = state,
                onFilterChange = viewModel::onChartFilterChanged
            )
        } else {
            DashboardContentPortrait(
                navController = navController,
                state = state,
                onFilterChange = viewModel::onChartFilterChanged
            )
        }
    }
}

// ==========================================
// TAMPILAN PORTRAIT (LAYOUT LAMA YANG DIOPTIMALKAN)
// ==========================================
@Composable
private fun DashboardContentPortrait(
    navController: NavController,
    state: DashboardState,
    onFilterChange: (ChartFilter) -> Unit
) {
    val scrollState = rememberScrollState()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Judul
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = primaryColor)
        Spacer(modifier = Modifier.height(20.dp))

        // Statistik
        StatisticSection(state, surfaceColor)
        Spacer(modifier = Modifier.height(24.dp))

        // Menu Kelola
        MenuSection(navController)
        Spacer(modifier = Modifier.height(32.dp))

        // Grafik Header & Filter
        ChartHeaderSection(state, onFilterChange)
        Spacer(modifier = Modifier.height(16.dp))

        // GRAFIK INTERAKTIF (Zoomable)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            InteractiveZoomChart(
                data = state.chartData,
                maxY = state.chartMaxY,
                lineColor = primaryColor,
                fillColor = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent)
                )
            )
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// TAMPILAN LANDSCAPE (LAYOUT BARU)
// ==========================================
@Composable
private fun DashboardContentLandscape(
    navController: NavController,
    state: DashboardState,
    onFilterChange: (ChartFilter) -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- BAGIAN KIRI: STATISTIK & MENU (Scrollable) ---
        Column(
            modifier = Modifier
                .weight(0.4f) // Ambil 40% layar
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Text("Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = primaryColor)
            Spacer(modifier = Modifier.height(16.dp))

            StatisticSection(state, surfaceColor)
            Spacer(modifier = Modifier.height(16.dp))

            MenuSection(navController)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- BAGIAN KANAN: GRAFIK FULL SCREEN (Fixed) ---
        Column(
            modifier = Modifier
                .weight(0.6f) // Ambil 60% layar
                .fillMaxHeight()
                .padding(bottom = 16.dp)
        ) {
            // Header Grafik
            ChartHeaderSection(state, onFilterChange)
            Spacer(modifier = Modifier.height(8.dp))

            // Card Grafik Full Height
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Isi sisa tinggi
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                InteractiveZoomChart(
                    data = state.chartData,
                    maxY = state.chartMaxY,
                    lineColor = primaryColor,
                    fillColor = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS (Agar Kode Rapi)
// ==========================================

@Composable
fun StatisticSection(state: DashboardState, surfaceColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItemBig(title = "Pendapatan Hari Ini", value = state.todayRevenue, isPrimary = true)
                StatItemSmall(title = "Pesanan", value = state.todayOrders.toString())
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItemBig(title = "Pendapatan Bulan Ini", value = state.monthRevenue, isPrimary = false)
                StatItemSmall(title = "Pesanan", value = state.monthOrders.toString())
            }
        }
    }
}

@Composable
fun MenuSection(navController: NavController) {
    Column {
        Text("Kelola", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ManageMenuBox("Kategori", Icons.Outlined.Category, Modifier.weight(1f)) { navController.navigate(Screen.Category.route) }
            ManageMenuBox("Produk", Icons.Outlined.Inventory2, Modifier.weight(1f)) { navController.navigate(Screen.Product.route) }
            ManageMenuBox("Pelanggan", Icons.Outlined.People, Modifier.weight(1f)) { navController.navigate(Screen.Customer.route) }
        }
    }
}

@Composable
fun ChartHeaderSection(state: DashboardState, onFilterChange: (ChartFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Grafik Pesanan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Row(
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartFilterButton("Harian", state.selectedChartFilter == ChartFilter.DAILY) { onFilterChange(ChartFilter.DAILY) }
            ChartFilterButton("Bulanan", state.selectedChartFilter == ChartFilter.MONTHLY) { onFilterChange(ChartFilter.MONTHLY) }
        }
    }
}
// ==========================================
// CORE: INTERACTIVE ZOOM CHART - PERBAIKAN
// ==========================================
@Composable
fun InteractiveZoomChart(
    data: List<Pair<String, Double>>,
    maxY: Double,
    lineColor: Color,
    fillColor: Brush
) {
    // Ambil color scheme di level Composable
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    if (data.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada data", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    // State Zoom & Pan - SET DEFAULT SCALE LEBIH KECIL
    var scale by remember { mutableStateOf(0.8f) } // DEFAULT ZOOM 0.8x (lebih kecil)
    var offsetX by remember { mutableStateOf(0f) }

    val gridColor = outlineVariant.copy(alpha = 0.3f)
    val textColor = onSurfaceVariant.toArgb()
    val pointColor = surfaceColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            // PUSATKAN GRAFIK DI DALAM CARD
            .padding(16.dp)
            // DETEKSI GESTURE (ZOOM & PAN)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Update Scale (Zoom) dengan batas 0.5x sampai 5x
                    scale = (scale * zoom).coerceIn(0.5f, 5f)

                    // Update Offset (Geser/Pan)
                    offsetX += pan.x
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Gunakan clipRect agar gambar tidak keluar dari kotak Chart saat digeser
            clipRect {
                val graphHeight = size.height * 0.7f // Kurangi tinggi grafik untuk memberi ruang label
                val graphTopPadding = size.height * 0.1f // Padding atas untuk label nilai
                val graphBottomPadding = size.height * 0.2f // Padding bawah untuk label tanggal

                // PERKECIL JARAK DEFAULT ANTAR TITIK
                val baseSpacing = 50.dp.toPx() // DIPERKECIL dari 80.dp ke 50.dp
                val spacing = baseSpacing * scale // Jarak setelah di-zoom

                // Hitung Total Lebar Grafik Berdasarkan Zoom
                val totalChartWidth = spacing * (data.size - 1)

                // POSISIKAN GRAFIK AGAR TITIK TERAKHIR (HARI INI) DI TENGAH
                val startPaddingX = if (totalChartWidth < size.width) {
                    // Jika grafik lebih kecil dari layar, center semua titik
                    (size.width - totalChartWidth) / 2
                } else {
                    // Jika grafik lebih besar, pastikan titik terakhir di tengah
                    size.width / 2 - (spacing * (data.size - 1))
                }

                // Batasi Offset (Agar tidak bisa geser lewat batas kiri/kanan)
                val maxOffset = 30f // DIPERKECIL padding awal
                val minOffset = -(totalChartWidth - size.width)

                // Apply clamp logic jika konten lebih besar dari layar
                if (totalChartWidth > size.width) {
                    offsetX = offsetX.coerceIn(minOffset, maxOffset)
                } else {
                    // Jika grafik muat dalam layar, gunakan offset untuk posisi tengah
                    offsetX = startPaddingX
                }

                // --- 1. GAMBAR GRID HORIZONTAL & LABEL Y (STATIC) ---
                val gridLines = 5
                val stepHeight = graphHeight / gridLines
                val stepValue = maxY / gridLines

                // PERKECIL FONT LABEL Y
                val textPaintY = android.graphics.Paint().apply {
                    color = textColor
                    textSize = 20f // SESUAIKAN UKURAN
                    textAlign = android.graphics.Paint.Align.LEFT
                }

                for (i in 0..gridLines) {
                    val y = graphTopPadding + graphHeight - (i * stepHeight)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Label Harga
                    val labelValue = (stepValue * i).toLong()
                    val labelText = if (labelValue >= 1000) "${labelValue / 1000}k" else labelValue.toString()
                    if (i > 0) {
                        drawContext.canvas.nativeCanvas.drawText(labelText, 5f, y - 5f, textPaintY)
                    }
                }

                // --- 2. HITUNG KOORDINAT TITIK ---
                val points = mutableListOf<Offset>()

                data.forEachIndexed { index, pair ->
                    // Rumus X: (index * jarak_per_titik) + posisi_geser
                    val x = (index * spacing) + offsetX
                    val y = graphTopPadding + graphHeight - ((pair.second / maxY) * graphHeight).toFloat()
                    points.add(Offset(x, y))
                }

                // --- 3. GAMBAR KURVA (PATH) ---
                if (points.isNotEmpty()) {
                    val strokePath = Path()
                    val fillPath = Path()

                    strokePath.moveTo(points.first().x, points.first().y)
                    fillPath.moveTo(points.first().x, graphTopPadding + graphHeight) // Tutup bawah
                    fillPath.lineTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        // Bezier Control Points
                        val controlPoint1 = Offset((p1.x + p2.x) / 2f, p1.y)
                        val controlPoint2 = Offset((p1.x + p2.x) / 2f, p2.y)

                        strokePath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                        fillPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                    }

                    fillPath.lineTo(points.last().x, graphTopPadding + graphHeight)
                    fillPath.close()

                    drawPath(path = fillPath, brush = fillColor)
                    drawPath(path = strokePath, color = lineColor, style = Stroke(width = 3.dp.toPx()))
                }

                // --- 4. GAMBAR TITIK & LABEL ---
                // PERKECIL FONT LABEL X
                val textPaintX = android.graphics.Paint().apply {
                    color = textColor
                    textSize = 20f // DIPERKECIL dari 25f
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // PERKECIL FONT LABEL NILAI (Revenue) - GUNAKAN WARNA TEMA
                val textPaintValue = android.graphics.Paint().apply {
                    color = primaryColor.toArgb() // GUNAKAN WARNA PRIMARY DARI TEMA
                    textSize = 25f // DIPERKECIL dari 25f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }

                points.forEachIndexed { index, point ->
                    // Hanya gambar jika titik berada dalam layar (Optimasi Rendering)
                    if (point.x >= -50f && point.x <= size.width + 50f) {

                        // Titik - beri highlight khusus untuk titik terakhir (hari ini)
                        val isLatestPoint = index == data.size - 1
                        val pointRadius = if (isLatestPoint) 5.dp.toPx() else 3.dp.toPx()

                        drawCircle(color = pointColor, radius = pointRadius, center = point)
                        drawCircle(
                            color = if (isLatestPoint) primaryColor else lineColor,
                            radius = pointRadius,
                            center = point,
                            style = Stroke(width = if (isLatestPoint) 1.5.dp.toPx() else 1.dp.toPx())
                        )

                        // Label Tanggal (Di bawah grafik)
                        // Beri highlight untuk label tanggal hari ini
                        val dateTextPaint = android.graphics.Paint().apply {
                            color = if (isLatestPoint) primaryColor.toArgb() else textColor
                            textSize = if (isLatestPoint) 21f else 20f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = isLatestPoint
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            data[index].first,
                            point.x,
                            graphTopPadding + graphHeight + graphBottomPadding - 10f,
                            dateTextPaint
                        )

                        // Label Nilai Pendapatan (Di atas titik) - SELALU DITAMPILKAN
                        val revenueValue = data[index].second
                        if (revenueValue > 0) {
                            val revenueText = formatCurrencyShort(revenueValue)

                            // Background untuk label nilai
                            val textWidth = textPaintValue.measureText(revenueText)
                            val rectWidth = textWidth + 6f // DIPERKECIL
                            val rectHeight = 14f
                            val rectLeft = point.x - rectWidth / 2
                            val rectTop = point.y - 18f // SESUAIKAN POSISI

                            // Gambar background rounded rectangle
                            drawRoundRect(
                                color = surfaceColor,
                                topLeft = Offset(rectLeft, rectTop),
                                size = Size(rectWidth, rectHeight),
                                cornerRadius = CornerRadius(3f, 3f)
                            )

                            // Border untuk background - gunakan warna primary untuk titik terakhir
                            val borderColor = if (isLatestPoint) primaryColor.copy(alpha = 0.8f) else lineColor.copy(alpha = 0.5f)
                            drawRoundRect(
                                color = borderColor,
                                topLeft = Offset(rectLeft, rectTop),
                                size = Size(rectWidth, rectHeight),
                                cornerRadius = CornerRadius(3f, 3f),
                                style = Stroke(width = if (isLatestPoint) 0.8f else 0.4f)
                            )

                            // Text nilai - gunakan warna yang sesuai tema
                            drawContext.canvas.nativeCanvas.drawText(
                                revenueText,
                                point.x,
                                point.y - 8f, // SESUAIKAN POSISI
                                textPaintValue
                            )
                        }
                    }
                }

                // --- 5. GARIS VERTIKAL UNTUK TITIK TERAKHIR (HARI INI) ---
                val latestPoint = points.lastOrNull()
                latestPoint?.let { point ->
                    drawLine(
                        color = primaryColor.copy(alpha = 0.3f),
                        start = Offset(point.x, graphTopPadding),
                        end = Offset(point.x, graphTopPadding + graphHeight),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f) // DIPERKECIL
                    )
                }
            } // End clipRect
        }
    }
}

// Fungsi helper untuk format currency singkat
fun formatCurrencyShort(amount: Double): String {
    return when {
        amount >= 1_000_000 -> "Rp${(amount / 1_000_000).toInt()}jt"
        amount >= 1_000 -> "Rp${(amount / 1_000).toInt()}Rb"
        else -> "Rp${amount.toInt()}"
    }
}
// --- HELPER (Tetap Sama) ---
@Composable
fun StatItemBig(title: String, value: String, isPrimary: Boolean) {
    Column {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatItemSmall(title: String, value: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ChartFilterButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ManageMenuBox(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

fun formatK(value: Double): String {
    return if (value >= 1000) {
        "${(value / 1000).toInt()}k"
    } else {
        value.toInt().toString()
    }
}