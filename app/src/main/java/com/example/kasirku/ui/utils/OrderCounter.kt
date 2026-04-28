package com.example.kasirku.ui.utils


import android.content.Context

class OrderCounter(context: Context) {
    private val prefs = context.getSharedPreferences("order_prefs", Context.MODE_PRIVATE)

    // Ambil nomor urut terakhir, tambah 1, lalu simpan
    fun getNextSequence(): Int {
        val current = prefs.getInt("sequence", 0)
        val next = current + 1
        prefs.edit().putInt("sequence", next).apply()
        return next
    }

    // Kembalikan nomor urut ke 0 (Reset)
    fun resetSequence() {
        prefs.edit().putInt("sequence", 0).apply()
    }
}