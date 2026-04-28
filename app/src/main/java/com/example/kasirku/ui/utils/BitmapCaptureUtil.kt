package com.example.kasirku.ui.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap

import androidx.compose.ui.platform.ComposeView
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap

object BitmapCaptureUtil {

    fun captureComposableToBitmap(
        context: Context,
        content: @Composable () -> Unit
    ): Bitmap? {
        return try {
            val composeView = ComposeView(context).apply {
                setContent {
                    content()
                }
            }

            // Gunakan ukuran yang fixed untuk thermal receipt
            val width = 400
            val height = 800

            // Measure dengan ukuran yang spesifik
            composeView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.AT_MOST)
            )

            composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

            // Create bitmap dari ComposeView
            val bitmap = createBitmap(composeView.measuredWidth, composeView.measuredHeight)
            val canvas = android.graphics.Canvas(bitmap)
            composeView.draw(canvas)

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ALTERNATIVE: Simple text-based bitmap creation
    fun createSimpleReceiptBitmap(context: Context, orderText: String): Bitmap {
        val width = 400
        val height = 600

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Background putih
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            typeface = android.graphics.Typeface.MONOSPACE
        }

        // Draw text lines
        val lines = orderText.split("\n")
        var y = 40f

        lines.forEach { line ->
            canvas.drawText(line, 20f, y, paint)
            y += 30f
        }

        return bitmap
    }
}