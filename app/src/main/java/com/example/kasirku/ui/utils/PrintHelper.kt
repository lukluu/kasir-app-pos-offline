package com.example.kasirku.ui.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.UUID

class PrinterHelper(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    // SharedPreferences untuk menyimpan printer aktif
    private val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
    private val KEY_ACTIVE_PRINTER = "active_printer_mac"

    // UUID Standar untuk Serial Port Profile (SPP)
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Variabel untuk koneksi printer
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    fun enableBluetooth() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun getActivePrinterMac(): String? {
        return prefs.getString(KEY_ACTIVE_PRINTER, null)
    }

    fun saveActivePrinter(macAddress: String) {
        prefs.edit().putString(KEY_ACTIVE_PRINTER, macAddress).apply()
    }

    fun clearActivePrinter() {
        prefs.edit().remove(KEY_ACTIVE_PRINTER).apply()
        disconnectPrinter()
    }

    // FUNGSI BARU: Connect to Printer untuk printing
    @SuppressLint("MissingPermission")
    suspend fun connectToPrinter(macAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Close existing connection first
                disconnectPrinter()

                // Find device
                val device = adapter?.bondedDevices?.find { it.address == macAddress }
                if (device == null) {
                    println("DEBUG: Device not found for MAC: $macAddress")
                    return@withContext false
                }

                println("DEBUG: Attempting to connect to: ${device.name} ($macAddress)")

                // Cancel discovery
                adapter?.cancelDiscovery()

                // Create socket with timeout
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

                // Set connection timeout
                bluetoothSocket?.let { socket ->
                    withTimeout(10000) { // 10 second timeout
                        socket.connect()
                    }
                }

                outputStream = bluetoothSocket?.outputStream

                val isConnected = bluetoothSocket?.isConnected ?: false
                println("DEBUG: Connection result: $isConnected")

                if (isConnected) {
                    saveActivePrinter(macAddress)
                    println("DEBUG: Successfully connected and saved printer")
                }

                isConnected
            } catch (e: Exception) {
                println("DEBUG: Connection error: ${e.message}")
                e.printStackTrace()
                disconnectPrinter()
                false
            }
        }
    }

    // FUNGSI BARU: Print Text Receipt
    suspend fun printTextReceipt(receiptText: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (outputStream == null) {
                    throw Exception("Printer not connected")
                }

                // ESC/POS Commands untuk thermal printer
                val printCommands = byteArrayOf(
                    0x1B, 0x40, // Initialize printer
                    0x1B, 0x61, 0x01, // Center alignment
                ) + receiptText.toByteArray(Charsets.ISO_8859_1) + byteArrayOf(
                    0x0A, 0x0A, 0x0A, 0x0A, // Beberapa line feed
                    0x1D, 0x56, 0x41, 0x10 // Cut paper
                )

                outputStream?.write(printCommands)
                outputStream?.flush()

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // Cek apakah printer terkoneksi
    fun isPrinterConnected(): Boolean {
        return try {
            val socket = bluetoothSocket
            val connected = socket?.isConnected ?: false

            // Additional check: try to write a small test command
            if (connected) {
                try {
                    outputStream?.write(byteArrayOf(0x1B, 0x40)) // Init command
                    outputStream?.flush()
                    true
                } catch (e: Exception) {
                    println("DEBUG: Test write failed: ${e.message}")
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            println("DEBUG: isPrinterConnected error: ${e.message}")
            false
        }
    }
    suspend fun reconnectToActivePrinter(): Boolean {
        val activeMac = getActivePrinterMac()
        return if (!activeMac.isNullOrEmpty()) {
            connectToPrinter(activeMac)
        } else {
            false
        }
    }


    // Disconnect printer
    fun disconnectPrinter() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        outputStream = null
        bluetoothSocket = null
    }

    // Di PrinterHelper class - TAMBAHKAN fungsi ini
    suspend fun printBitmapReceipt(bitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (outputStream == null) {
                    throw Exception("Printer not connected")
                }

                println("DEBUG: Printing bitmap, size: ${bitmap.width}x${bitmap.height}")

                // ESC/POS Commands untuk print bitmap
                val printCommands = convertBitmapToEscPos(bitmap)

                outputStream?.write(printCommands)
                outputStream?.flush()

                println("DEBUG: Bitmap print commands sent successfully")
                true
            } catch (e: Exception) {
                println("DEBUG: Bitmap print error: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    // Fungsi untuk konversi Bitmap ke ESC/POS format
    private fun convertBitmapToEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height

        // Adjust width to printer limits (biasanya 384-576 pixels)
        val printerWidth = 384 // Thermal printer typical width
        val scaleFactor = printerWidth.toFloat() / width.toFloat()
        val scaledWidth = printerWidth
        val scaledHeight = (height * scaleFactor).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        val command = ByteArrayOutputStream()

        try {
            // ESC POS Initialization
            command.write(byteArrayOf(0x1B, 0x40)) // Initialize printer

            // Set line spacing
            command.write(byteArrayOf(0x1B, 0x33, 0x00))

            // Convert bitmap to ESC/POS raster format
            val rasterBytes = bitmapToRasterBytes(scaledBitmap)

            // Print raster image
            for (i in 0 until scaledHeight step 24) {
                // GS v 0 m xL xH yL yH d1...dk
                command.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00)) // GS v 0
                command.write(byteArrayOf(
                    (scaledWidth and 0xFF).toByte(),
                    ((scaledWidth shr 8) and 0xFF).toByte()
                )) // xL xH
                command.write(byteArrayOf(0x01, 0x00)) // yL yH (1 line)

                // Write raster data for this line
                val lineBytes = ByteArray(scaledWidth * 3) // 24 dots = 3 bytes
                val startIndex = i * scaledWidth * 3
                val endIndex = minOf(startIndex + lineBytes.size, rasterBytes.size)

                if (startIndex < rasterBytes.size) {
                    val length = endIndex - startIndex
                    System.arraycopy(rasterBytes, startIndex, lineBytes, 0, length)
                    command.write(lineBytes)
                }
            }

            // Add some line feeds and paper cut
            command.write(byteArrayOf(0x0A, 0x0A, 0x0A))
            command.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10)) // Cut paper

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return command.toByteArray()
    }

    // Fungsi untuk konversi bitmap ke raster bytes
    private fun bitmapToRasterBytes(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val rasterBytes = ByteArray(width * height * 3) // 24 dots per line = 3 bytes

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3

                // Convert to monochrome (threshold 128)
                val isBlack = brightness < 128

                if (isBlack) {
                    val byteIndex = (y * width + x) * 3
                    val bitPosition = x % 8
                    val bytePosition = byteIndex + (x / 8)

                    if (bytePosition < rasterBytes.size) {
                        rasterBytes[bytePosition] = (rasterBytes[bytePosition].toInt() or (1 shl (7 - bitPosition))).toByte()
                    }
                }
            }
        }

        return rasterBytes
    }

    // ALTERNATIF: Versi lebih sederhana untuk thermal printer
    private fun convertBitmapToEscPosSimple(bitmap: Bitmap): ByteArray {
        val command = ByteArrayOutputStream()

        try {
            // ESC POS Initialization
            command.write(byteArrayOf(0x1B, 0x40))

            // Scale bitmap to printer width
            val printerWidth = 384
            val scaleFactor = printerWidth.toFloat() / bitmap.width.toFloat()
            val scaledHeight = (bitmap.height * scaleFactor).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, printerWidth, scaledHeight, true)

            // Convert to monochrome
            val monoBitmap = convertToMonochrome(scaledBitmap)

            // Print each line
            for (y in 0 until monoBitmap.height step 24) {
                val chunkHeight = minOf(24, monoBitmap.height - y)

                command.write(byteArrayOf(0x1B, 0x2A, 0x21)) // Bit image mode
                command.write(byteArrayOf(
                    (printerWidth and 0xFF).toByte(),
                    ((printerWidth shr 8) and 0xFF).toByte()
                ))

                for (x in 0 until printerWidth) {
                    var lineByte: Byte = 0
                    for (bit in 0 until chunkHeight) {
                        if (y + bit < monoBitmap.height) {
                            val pixel = monoBitmap.getPixel(x, y + bit)
                            if (Color.red(pixel) == 0) { // Black pixel
                                lineByte = (lineByte.toInt() or (1 shl (7 - bit))).toByte()
                            }
                        }
                    }
                    command.write(byteArrayOf(lineByte))
                }

                command.write(byteArrayOf(0x0A)) // Line feed
            }

            // Add final line feeds and cut
            command.write(byteArrayOf(0x0A, 0x0A, 0x0A))
            command.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10))

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return command.toByteArray()
    }

    private fun convertToMonochrome(bitmap: Bitmap): Bitmap {
        val monoBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                val newPixel = if (brightness < 128) Color.BLACK else Color.WHITE
                monoBitmap.setPixel(x, y, newPixel)
            }
        }

        return monoBitmap
    }


    // Fungsi Test Koneksi (untuk pairing screen)
    @SuppressLint("MissingPermission")
    suspend fun connectAndTest(device: BluetoothDevice): Boolean {
        return withContext(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            try {
                adapter?.cancelDiscovery()
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                try { socket?.close() } catch (e: Exception) {}
            }
        }
    }
}