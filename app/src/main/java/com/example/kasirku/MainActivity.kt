package com.example.kasirku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.kasirku.ui.theme.CashierAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // Anotasi wajib untuk Hilt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
//        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hapus semua pembuatan ViewModel manual dari sini

        setContent {
            CashierAppTheme {
                CashierApp() // Panggil root composable tanpa parameter
            }
        }
    }
}