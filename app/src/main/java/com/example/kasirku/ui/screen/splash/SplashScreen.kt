package com.example.kasirku.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kasirku.R
import com.example.kasirku.ui.navigation.Screen
import com.example.kasirku.ui.viewmodel.AuthState
import com.example.kasirku.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            // Logo - sesuaikan nama file dengan yang Anda punya
            Image(
                painter = painterResource(R.drawable.madecca_2), // ⚠️ GANTI dengan nama file PNG Anda
                contentDescription = "App Logo",
                modifier = Modifier.size(200.dp)
            )

            // Nama App
//            Text(
//                "KasirKu",
//                color = MaterialTheme.colorScheme.onPrimary,
//                fontSize = 32.sp,
//                fontWeight = FontWeight.Bold
//            )
        }
    }

    LaunchedEffect(Unit) {
        delay(2000) // Tampilkan 2 detik

        when (authViewModel.authState.value) {
            is AuthState.Authenticated -> {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            else -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
    }
}