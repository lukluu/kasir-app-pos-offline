package com.example.kasirku.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.kasirku.domain.model.Order
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PrintHelper {

    fun printOrderReceipt(context: Context, order: Order): Boolean {
        return try {
            // TODO: Implementasi koneksi printer Bluetooth ESC/POS
            // Contoh pseudocode:
            // 1. Connect ke printer Bluetooth
            // 2. Format teks untuk ESC/POS
            // 3. Kirim perintah print
            // 4. Close connection

            // Untuk sementara, tampilkan toast
            Toast.makeText(context, "Mencetak nota: ${order.orderNumber}", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun shareReceiptAsText(context: Context, order: Order): Boolean {
        return try {
            val receiptText = buildReceiptText(order)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, receiptText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Nota"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun shareReceiptAsImage(context: Context, order: Order, bitmap: Bitmap): Boolean {
        return try {
            val imagesFolder = File(context.cacheDir, "receipts")
            imagesFolder.mkdirs()

            val file = File(imagesFolder, "receipt_${order.orderNumber}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Nota sebagai Gambar"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isPrinterConnected(context: Context): Boolean {
        // TODO: Implementasi check koneksi printer
        return false
    }

    private fun buildReceiptText(order: Order): String {
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(order.createdAt)
        val sb = StringBuilder()

        sb.appendLine("TOKO KASIRKU")
        sb.appendLine("Jl. Contoh No. 123")
        sb.appendLine("Telp: 0812-3456-7890")
        sb.appendLine("=".repeat(32))
        sb.appendLine("No. Pesanan: ${order.orderNumber}")
        sb.appendLine("Tanggal: $date")
        sb.appendLine("Pelanggan: ${order.customer?.name ?: "-"}")
        sb.appendLine("No. HP: ${order.customer?.phoneNumber ?: "-"}")

        if (!order.note.isNullOrBlank()) {
            sb.appendLine("Catatan: ${order.note}")
        }

        sb.appendLine("-".repeat(32))
        sb.appendLine("PRODUK")

        order.items.forEach { item ->
            sb.appendLine("${item.product.name}")
            sb.appendLine("${item.quantity} x ${formatCurrency(item.priceAtSale)} = ${formatCurrency(item.priceAtSale * item.quantity)}")
        }

        sb.appendLine("-".repeat(32))
        val subtotal = order.totalPrice + order.discount
        sb.appendLine("Subtotal: ${formatCurrency(subtotal)}")

        if (order.discount > 0) {
            sb.appendLine("Diskon: -${formatCurrency(order.discount)}")
        }

        sb.appendLine("TOTAL: ${formatCurrency(order.totalPrice)}")
        sb.appendLine("-".repeat(32))
        sb.appendLine("PEMBAYARAN")
        sb.appendLine("Metode: ${order.paymentMethod ?: "Tunai"}")

        if (order.paymentStatus == "PAID" && order.amountPaid > 0) {
            val change = (order.amountPaid - order.totalPrice).coerceAtLeast(0.0)
            sb.appendLine("Dibayar: ${formatCurrency(order.amountPaid)}")
            sb.appendLine("Kembali: ${formatCurrency(change)}")
        }

        sb.appendLine("Status: ${when(order.paymentStatus) {
            "PAID" -> "LUNAS"
            "CANCELLED" -> "DIBATALKAN"
            else -> "BELUM BAYAR"
        }}")

        if (order.paymentStatus == "CANCELLED" && !order.cancelReason.isNullOrBlank()) {
            sb.appendLine("Alasan: ${order.cancelReason}")
        }

        sb.appendLine("=".repeat(32))
        sb.appendLine("Terima Kasih")
        sb.appendLine("Barang yang sudah dibeli")
        sb.appendLine("tidak dapat ditukar/dikembalikan")

        return sb.toString()
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount).replace(",00", "")
    }
}