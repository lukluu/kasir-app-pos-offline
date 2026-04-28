package com.example.kasirku.ui.screen.pesanan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.data.repository.AuthRepository
import com.example.kasirku.domain.model.Order
import com.example.kasirku.domain.repository.OrderRepository
import com.example.kasirku.ui.utils.PrinterHelper
import com.example.kasirku.ui.utils.ReceiptImageGenerator
import com.example.kasirku.ui.utils.ReceiptTextFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val repository: OrderRepository,
    private val authRepository: AuthRepository,
    private val printerHelper: PrinterHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: Long = checkNotNull(savedStateHandle["orderId"]).toString().toLong()

    val order: StateFlow<Order?> = repository.getOrderById(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val outletData: StateFlow<OutletData?> = authRepository.getCurrentUserFlow()
        .map { user ->
            user?.let {
                OutletData(
                    name = it.name,
                    address = it.address ?: "Alamat belum diatur",
                    phone = it.phoneNumber ?: "Telepon belum diatur",
                    footerNote = it.footerNote ?: "Terima Kasih"
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // PERBAIKI: Cek koneksi dengan auto-reconnect
    fun isPrinterConnected(): Boolean {
        val isConnected = printerHelper.isPrinterConnected()
        println("DEBUG: Printer connected check: $isConnected")

        if (!isConnected) {
            // Try to reconnect automatically
            viewModelScope.launch {
                val reconnected = printerHelper.reconnectToActivePrinter()
                println("DEBUG: Auto-reconnect result: $reconnected")
            }
        }

        return isConnected
    }

    // PERBAIKI: Fungsi print dengan reconnection logic
// Di ReceiptViewModel - ganti fungsi printReceipt
    suspend fun printReceipt(order: Order): Boolean {
        return try {
            val outlet = outletData.value
            // GUNAKAN FORMAT BARU YANG RAPI
            val receiptText = ReceiptTextFormatter.formatCompactReceiptText(order, outlet)

            println("DEBUG: Attempting to print receipt with compact format...")

            // Cek dan reconnect jika perlu
            if (!printerHelper.isPrinterConnected()) {
                println("DEBUG: Printer not connected, attempting reconnect...")
                val reconnected = printerHelper.reconnectToActivePrinter()
                if (!reconnected) {
                    println("DEBUG: Reconnect failed")
                    return false
                }
            }

            println("DEBUG: Sending print commands...")
            val success = printerHelper.printTextReceipt(receiptText)
            println("DEBUG: Print result: $success")

            success
        } catch (e: Exception) {
            println("DEBUG: Print error: ${e.message}")
            e.printStackTrace()
            false
        }
    }

//    suspend fun printReceipt(order: Order): Boolean {
//        // Coba bitmap dulu, jika gagal fallback ke text
//        val bitmapSuccess = printBitmapReceipt(order)
//
//        if (!bitmapSuccess) {
//            println("DEBUG: Bitmap failed, falling back to text printing")
//            return printTextReceipt(order)
//        }
//
//        return bitmapSuccess
//    }




    suspend fun printBitmapReceipt(order: Order): Boolean {
        return try {
            val outlet = outletData.value

            // GENERATE BITMAP
            val receiptBitmap = ReceiptImageGenerator.createReceiptBitmap(order, outlet)
            println("DEBUG: Bitmap created - ${receiptBitmap.width}x${receiptBitmap.height}")

            // CEK KONEKSI dengan timeout
            if (!printerHelper.isPrinterConnected()) {
                println("DEBUG: Printer not connected, reconnecting...")
                val reconnected = withTimeout(5000) { // 5 second timeout
                    printerHelper.reconnectToActivePrinter()
                }
                if (!reconnected) {
                    println("DEBUG: Reconnect failed")
                    return false
                }
            }

            // PRINT dengan timeout
            println("DEBUG: Starting bitmap print...")
            val success = withTimeout(10000) { // 10 second timeout
                printerHelper.printBitmapReceipt(receiptBitmap)
            }

            println("DEBUG: Bitmap print completed: $success")
            return success

        } catch (e: Exception) {
            println("DEBUG: Bitmap print error: ${e.message}")
            e.printStackTrace()
            false
        }
    }


    private suspend fun printTextReceipt(order: Order): Boolean {
        val outlet = outletData.value
        val receiptText = ReceiptTextFormatter.formatReceiptText(order, outlet)
        return printerHelper.printTextReceipt(receiptText)
    }
//    private suspend fun printTextReceipt(order: Order): Boolean {
//        return try {
//            val outlet = outletData.value
//            val receiptText = ReceiptTextFormatter.formatSimpleReceiptText(order, outlet)
//
//            if (!printerHelper.isPrinterConnected()) {
//                printerHelper.reconnectToActivePrinter()
//            }
//
//            printerHelper.printTextReceipt(receiptText)
//        } catch (e: Exception) {
//            println("DEBUG: Text print error: ${e.message}")
//            false
//        }
//    }

    fun getActivePrinterMac(): String? = printerHelper.getActivePrinterMac()

    // FUNGSI BARU: Force reconnect
    suspend fun forceReconnectPrinter(): Boolean {
        return printerHelper.reconnectToActivePrinter()
    }
}

// Data class untuk menyimpan data outlet
data class OutletData(
    val name: String,
    val address: String,
    val phone: String,
    val footerNote: String
)