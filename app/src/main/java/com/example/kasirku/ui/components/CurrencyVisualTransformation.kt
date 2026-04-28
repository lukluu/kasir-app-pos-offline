package com.example.kasirku.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat

class CurrencyVisualTransformation(
    private val prefix: String = "Rp "
) : VisualTransformation {

    // Menggunakan DecimalFormat untuk secara otomatis menangani pemisah ribuan
    private val formatter = DecimalFormat("#,###")

    override fun filter(text: AnnotatedString): TransformedText {
        // 'text' adalah input asli dari ViewModel (hanya digit, misal: "10000")
        val originalText = text.text

        if (originalText.isBlank()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        // Hindari crash jika angka terlalu besar untuk di-parse
        val number = originalText.toLongOrNull() ?: return TransformedText(text, OffsetMapping.Identity)

        // Teks yang akan ditampilkan ke pengguna (misal: "Rp 10.000")
        val formattedText = prefix + formatter.format(number)

        // OffsetMapping yang baru dan lebih aman
        val offsetMapping = object : OffsetMapping {

            // Mengubah posisi kursor dari teks asli ke teks yang diformat
            override fun originalToTransformed(offset: Int): Int {
                // 'offset' adalah posisi kursor di teks asli (misal: di "10|000", offset = 2)

                // Hitung berapa banyak pemisah (titik) yang akan muncul sebelum posisi kursor tersebut
                // Contoh: jika originalText = "1234567" dan offset = 5 ("12345|67"),
                // maka substringnya adalah "12345". Setelah diformat, menjadi "12.345".
                // Panjangnya bertambah 1 karena ada satu titik.
                val textBeforeCursor = originalText.take(offset)
                val formattedTextBeforeCursor = formatter.format(textBeforeCursor.toLongOrNull() ?: 0)

                // Posisi baru adalah panjang prefix + panjang teks yang sudah diformat
                return prefix.length + formattedTextBeforeCursor.length
            }

            // Mengubah posisi kursor dari teks yang diformat kembali ke teks asli
            override fun transformedToOriginal(offset: Int): Int {
                // 'offset' adalah posisi kursor di teks yang ditampilkan (misal: di "Rp 10|.000", offset = 5)

                // 1. Tangani kasus jika kursor berada di dalam atau sebelum prefix "Rp "
                if (offset <= prefix.length) {
                    return 0
                }

                // 2. Ambil bagian teks setelah prefix, hingga posisi kursor
                // Contoh: jika formattedText = "Rp 1.234.567" dan offset = 10 ("Rp 1.234.|567")
                // maka textUntilCursor akan menjadi "1.234."
                val textUntilCursor = formattedText.substring(prefix.length, offset)

                // 3. Hitung berapa banyak karakter digit di dalam substring tersebut
                // Contoh: "1.234." memiliki 4 digit. Maka posisi kursor di teks asli adalah 4.
                return textUntilCursor.count { it.isDigit() }
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}