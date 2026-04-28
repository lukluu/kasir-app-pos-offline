package com.example.kasirku.ui.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.kasirku.domain.model.Order
import com.example.kasirku.ui.screen.pesanan.OutletData
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

object ReceiptImageGenerator {

    // Konfigurasi lebar kertas dan font
    private const val RECEIPT_WIDTH = 600
    private const val TEXT_SIZE = 24f
    private const val LINE_HEIGHT = 35f
    private const val MARGIN = 30f

    fun createReceiptBitmap(
        order: Order,
        outletData: OutletData? = null
    ): Bitmap {
        val height = calculateDynamicHeight(order, outletData)
        val bitmap = Bitmap.createBitmap(RECEIPT_WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background Putih
        canvas.drawColor(Color.White.toArgb())

        // 2. Setup Paint (Font Monospace agar seperti mesin kasir)
        val paint = Paint().apply {
            color = Color.Black.toArgb()
            textSize = TEXT_SIZE
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.Black.toArgb()
            textSize = TEXT_SIZE
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        var y = 50f // Padding atas
        val pageWidth = RECEIPT_WIDTH.toFloat()

        // --- HELPER FUNCTIONS ---
        fun drawText(text: String, x: Float, yPos: Float, p: Paint) {
            canvas.drawText(text, x, yPos, p)
        }

        fun drawCentered(text: String, yPos: Float, p: Paint) {
            val width = p.measureText(text)
            val x = (pageWidth - width) / 2
            drawText(text, x, yPos, p)
        }

        fun drawLeft(text: String, yPos: Float, p: Paint) {
            drawText(text, MARGIN, yPos, p)
        }

        fun drawRight(text: String, yPos: Float, p: Paint) {
            val width = p.measureText(text)
            drawText(text, pageWidth - MARGIN - width, yPos, p)
        }

        // Fungsi menggambar garis putus-putus menggunakan teks (agar mirip struk fisik)
        fun drawDashedLine(yPos: Float) {
            val dashChar = "-"
            val dashWidth = paint.measureText(dashChar)
            val numDashes = ((pageWidth - (2 * MARGIN)) / dashWidth).toInt()
            val dashes = dashChar.repeat(numDashes)
            drawCentered(dashes, yPos, paint)
        }

        // --- MULAI MENGGAMBAR KONTEN ---

        // 1. HEADER TOKO
        val outletName = outletData?.name ?: "NAMA TOKO"
        // Gabungkan alamat dan no hp agar hemat tempat seperti contoh
        val outletInfo = "${outletData?.address ?: ""} - ${outletData?.phone ?: ""}".trim(' ', '-')

        drawCentered(outletName, y, boldPaint)
        y += LINE_HEIGHT

        if (outletInfo.isNotBlank()) {
            // Jika teks terlalu panjang, pecah (wrap) logic sederhana
            if (paint.measureText(outletInfo) > (pageWidth - 2 * MARGIN)) {
                // Sederhanakan untuk contoh ini: ambil substring atau biarkan terpotong sedikit
                // agar sesuai request "singkat"
                drawCentered(outletInfo.take(40), y, paint)
            } else {
                drawCentered(outletInfo, y, paint)
            }
            y += LINE_HEIGHT
        }

        // Nomor meja/ID Toko (Angka "17" di tengah pada contoh)
        drawCentered("17", y, paint)
        y += LINE_HEIGHT + 10f // Sedikit spasi sebelum garis

        // Garis Pemisah
        drawDashedLine(y)
        y += LINE_HEIGHT

        // 2. INFO PESANAN (Layout: Label di atas, Value di bawah)
        drawLeft("No. Pesanan", y, paint)
        y += LINE_HEIGHT
        drawLeft(order.orderNumber, y, paint) // Value di baris baru
        y += LINE_HEIGHT

        drawLeft("Waktu", y, paint)
        y += LINE_HEIGHT
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        drawLeft(dateFormat.format(order.createdAt), y, paint)
        y += LINE_HEIGHT

        drawDashedLine(y)
        y += LINE_HEIGHT

        // 3. PRODUK
        drawLeft("Produk", y, paint)
        y += LINE_HEIGHT

        order.items.forEach { item ->
            // Nama Produk
            drawLeft(item.product.name, y, paint)
            y += LINE_HEIGHT

            // Qty x Harga (Kiri) dan Total (Kanan)
            val qtyPrice = "${item.quantity} x ${formatCurrency(item.priceAtSale)}"
            val totalItemPrice = formatCurrency(item.quantity * item.priceAtSale)

            drawLeft(qtyPrice, y, paint)
            drawRight(totalItemPrice, y, paint)
            y += LINE_HEIGHT
        }

        drawDashedLine(y)
        y += LINE_HEIGHT

        // 4. TOTAL (Bold)
        drawLeft("Total", y, boldPaint)
        drawRight(formatCurrency(order.totalPrice), y, boldPaint)
        y += LINE_HEIGHT

        drawDashedLine(y)
        y += LINE_HEIGHT

        // 5. PEMBAYARAN
        drawLeft("Pembayaran", y, paint)
        y += LINE_HEIGHT

        val paymentMethod = if (order.paymentMethod.isNullOrBlank()) "Tunai" else order.paymentMethod
        drawLeft(paymentMethod, y, paint)
        // Di contoh gambar, jumlah pembayaran di sebelah kanan metode
        drawRight(formatCurrency(order.totalPrice), y, paint)

        // Selesai (Hapus footer "terima kasih" agar pendek sesuai request)

        return bitmap
    }

    private fun calculateDynamicHeight(
        order: Order,
        outletData: OutletData?
    ): Int {
        // Hitung estimasi tinggi baris
        var lines = 0

        // Header
        lines += 3 // Nama, Alamat, Angka Tengah
        lines += 1 // Garis

        // Info Pesanan
        lines += 4 // Label No, Value No, Label Waktu, Value Waktu
        lines += 1 // Garis

        // Produk
        lines += 1 // Header "Produk"
        lines += order.items.size * 2 // Nama produk + (Qty & Harga)
        lines += 1 // Garis

        // Total
        lines += 1 // Baris Total
        lines += 1 // Garis

        // Pembayaran
        lines += 2 // Label Pembayaran + (Metode & Harga)

        // Padding bawah
        lines += 2

        return (lines * LINE_HEIGHT + 100).toInt() // Buffer pixel
    }

    private fun formatCurrency(amount: Double): String {
        val kursIndonesia = DecimalFormat.getCurrencyInstance() as DecimalFormat
        val formatRp = DecimalFormatSymbols()

        formatRp.currencySymbol = "Rp "
        formatRp.monetaryDecimalSeparator = ','
        formatRp.groupingSeparator = '.'

        kursIndonesia.decimalFormatSymbols = formatRp
        kursIndonesia.maximumFractionDigits = 0 // Hilangkan desimal ,00

        return kursIndonesia.format(amount)
    }
}