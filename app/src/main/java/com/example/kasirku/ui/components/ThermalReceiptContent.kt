package com.example.kasirku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kasirku.domain.model.Order
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ThermalReceiptContent(
    order: Order,
    modifier: Modifier = Modifier
) {
    val receiptFont = FontFamily.Monospace
    val date = SimpleDateFormat("dd/MM/yy HH:mm", Locale("id", "ID")).format(order.createdAt)

    Surface(
        color = Color.White,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER - Center aligned
            Text(
                "TOKO KASIRKU",
                fontWeight = FontWeight.Bold,
                fontFamily = receiptFont,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Jl. Contoh No. 123",
                fontFamily = receiptFont,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "08123456789",
                fontFamily = receiptFont,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            // INFO ORDER - Left aligned
            ReceiptRowThermal("No.Pesanan", order.orderNumber, receiptFont)
            ReceiptRowThermal("Tanggal", date, receiptFont)

            val customerName = order.customer?.name?.ifBlank { "-" } ?: "-"
            val customerPhone = order.customer?.phoneNumber?.ifBlank { "-" } ?: "-"

            ReceiptRowThermal("Pelanggan", customerName, receiptFont)
            ReceiptRowThermal("No.HP", customerPhone, receiptFont)

            if (!order.note.isNullOrBlank()) {
                ReceiptRowThermal("Catatan", order.note, receiptFont)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            // HEADER PRODUK
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("PRODUK", fontFamily = receiptFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("SUBTTL", fontFamily = receiptFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Divider(color = Color.Black, thickness = 0.5.dp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))

            // ITEM PRODUK
            order.items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    // Nama produk
                    Text(
                        item.product.name,
                        fontFamily = receiptFont,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Qty x Harga = Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${item.quantity} x ${formatCurrency(item.priceAtSale)}",
                            fontFamily = receiptFont,
                            fontSize = 10.sp
                        )
                        Text(
                            formatCurrency(item.quantity * item.priceAtSale),
                            fontFamily = receiptFont,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            // TOTAL
            val subtotal = order.totalPrice + order.discount
            ReceiptRowThermal("Subtotal", formatCurrency(subtotal), receiptFont)

            if (order.discount > 0) {
                ReceiptRowThermal("Diskon", "-${formatCurrency(order.discount)}", receiptFont)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TOTAL", fontFamily = receiptFont, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    formatCurrency(order.totalPrice),
                    fontFamily = receiptFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            // PEMBAYARAN
            if (order.paymentStatus == "PAID") {
                val paymentMethod = if (order.paymentMethod.isNullOrBlank()) "Tunai" else order.paymentMethod
                ReceiptRowThermal("Metode", paymentMethod, receiptFont)

                if (order.amountPaid > 0) {
                    val change = (order.amountPaid - order.totalPrice).coerceAtLeast(0.0)
                    ReceiptRowThermal("Diterima", formatCurrency(order.amountPaid), receiptFont)
                    ReceiptRowThermal("Kembalian", formatCurrency(change), receiptFont)
                }
            }

            // STATUS
            val statusText = when(order.paymentStatus) {
                "PAID" -> "LUNAS"
                "CANCELLED" -> "DIBATALKAN"
                else -> "BELUM BAYAR"
            }
            ReceiptRowThermal("Status", statusText, receiptFont)

            if (order.paymentStatus == "CANCELLED" && !order.cancelReason.isNullOrBlank()) {
                Text(
                    "Alasan: ${order.cancelReason}",
                    fontFamily = receiptFont,
                    fontSize = 10.sp,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            // FOOTER - Center aligned
            Text(
                "Terima Kasih",
                fontFamily = receiptFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Barang sudah dibeli",
                fontFamily = receiptFont,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "tidak dapat ditukar/dikembalikan",
                fontFamily = receiptFont,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ReceiptRowThermal(label: String, value: String, font: FontFamily) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = font, fontSize = 11.sp)
        Text(value, fontFamily = font, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount).replace(",00", "")
}