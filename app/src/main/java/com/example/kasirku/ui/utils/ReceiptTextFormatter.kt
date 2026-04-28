package com.example.kasirku.ui.utils

import com.example.kasirku.domain.model.Order
import com.example.kasirku.ui.screen.pesanan.OutletData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ReceiptTextFormatter {

    // FORMAT BARU YANG RAPI SEPERTI GAMBAR
    fun formatCompactReceiptText(order: Order, outlet: OutletData? = null): String {
        val outletName = outlet?.name ?: "MADECCA"
        // HEADER: Alamat dan No HP jadi satu baris dipisah "-"
        val outletInfo = "${outlet?.address ?: "Perdos UHO blok L"} - ${outlet?.phone ?: "0852200481"}"

        val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(order.createdAt)

        val sb = StringBuilder()

        // --- HEADER RATA TENGAH ---
        sb.append(centerText(outletName))
        sb.append(centerText(outletInfo))
        sb.append(centerText("17")) // Nomor meja seperti contoh gambar
        sb.append("\n")

        // GARIS PEMISAH
        sb.append(centerText("-------------------------------"))
        sb.append("\n")

        // --- INFO PESANAN ---
        sb.append("No. Pesanan\n")
        sb.append("${order.orderNumber}\n")
        sb.append("Waktu\n")
        sb.append("$date\n")
        sb.append("\n")

        // GARIS PEMISAH
        sb.append(centerText("-------------------------------"))
        sb.append("\n")

        // --- PRODUK ---
        sb.append("Produk\n")
        sb.append(centerText("-------------------------------"))
        sb.append("\n")

        // ITEM PRODUK - Format seperti gambar
        order.items.forEach { item ->
            // Nama produk
            sb.append("${item.product.name}\n")

            // Qty x Harga dan Total (format rapat)
            val qtyPrice = "${item.quantity} x ${formatCurrency(item.priceAtSale)}"
            val total = formatCurrency(item.quantity * item.priceAtSale)

            // Alignment: qtyPrice di kiri, total di kanan
            sb.append("$qtyPrice${getSpacing(qtyPrice, total)}$total\n")
        }

        // GARIS PEMISAH
        sb.append(centerText("-------------------------------"))
        sb.append("\n")

        // --- TOTAL ---
        val totalText = "Total"
        val totalAmount = formatCurrency(order.totalPrice)
        sb.append("$totalText${getSpacing(totalText, totalAmount)}$totalAmount\n")

        // GARIS PEMISAH
        sb.append(centerText("-------------------------------"))
        sb.append("\n")

        // --- PEMBAYARAN ---
        sb.append("Pembayaran\n")

        val paymentMethod = if (order.paymentMethod.isNullOrBlank()) "Tunai" else order.paymentMethod
        val paymentAmount = formatCurrency(order.totalPrice)

        // HILANGKAN "Diterima" dan "Kembalian" sesuai permintaan
        sb.append("$paymentMethod${getSpacing(paymentMethod, paymentAmount)}$paymentAmount\n")

        // JARAK RAPAT - tidak perlu footer
        sb.append("\n") // Sedikit spasi saja di akhir

        return sb.toString()
    }

    // FORMAT SIMPLE (backward compatibility)
    fun formatSimpleReceiptText(order: Order, outlet: OutletData? = null): String {
        return formatCompactReceiptText(order, outlet)
    }

    // FORMAT LAMA (untuk kompatibilitas)
    fun formatReceiptText(order: Order, outlet: OutletData? = null): String {
        return formatCompactReceiptText(order, outlet)
    }

    // --- HELPER FUNCTIONS ---

    private fun centerText(text: String): String {
        val totalWidth = 32 // Lebar thermal printer typical
        if (text.length >= totalWidth) return "$text\n"

        val padding = (totalWidth - text.length) / 2
        return " ".repeat(padding.coerceAtLeast(0)) + text + "\n"
    }

    private fun getSpacing(left: String, right: String): String {
        val totalWidth = 32
        val spacingNeeded = totalWidth - left.length - right.length
        return " ".repeat(spacingNeeded.coerceAtLeast(1))
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount)
                .replace(",00", "")
                .replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp ${amount.toInt()}"
        }
    }

    // Fungsi lama untuk kompatibilitas (tidak digunakan di format baru)
    private fun appendRightAligned(sb: StringBuilder, label: String, value: String) {
        val totalWidth = 32
        val spaces = " ".repeat(totalWidth - label.length - value.length)
        sb.append("$label$spaces$value\n")
    }
}