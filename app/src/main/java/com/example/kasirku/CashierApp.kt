package com.example.kasirku

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kasirku.ui.components.AppBottomBar
import com.example.kasirku.ui.navigation.AppNavHost
import com.example.kasirku.ui.navigation.Screen
import com.example.kasirku.ui.viewmodel.AuthState
import com.example.kasirku.ui.viewmodel.AuthViewModel
import android.util.Log
import kotlinx.coroutines.delay

@Composable
fun CashierApp(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    // PERBAIKAN: Gunakan currentBackStackEntryAsState() sebagai fungsi
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarScreens = listOf(Screen.Dashboard.route)
    val shouldShowBottomBar = currentRoute in bottomBarScreens

    // PERBAIKAN: Navigation logic yang lebih robust
    LaunchedEffect(authState) {
        Log.d("CashierApp", "AuthState changed: ${authState::class.simpleName}")

        // Tunggu sebentar untuk memastikan navigasi stabil
        delay(100)

        val currentDestination = navController.currentDestination?.route
        Log.d("CashierApp", "Current destination: $currentDestination")

        when (authState) {
            is AuthState.Authenticated -> {
                // Jika user sudah login, arahkan ke Dashboard dari screen auth
                if (currentDestination == Screen.Login.route ||
                    currentDestination == Screen.Register.route ||
                    currentDestination == Screen.Splash.route) {

                    Log.d("CashierApp", "Navigating to Dashboard from: $currentDestination")
                    navController.navigate(Screen.Dashboard.route) {
                        // Hapus semua screen dari back stack
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            is AuthState.Unauthenticated -> {
                // Jika user logout, arahkan ke Login dari screen yang butuh auth
                if (currentDestination == Screen.Dashboard.route) {
                    Log.d("CashierApp", "Navigating to Login from: $currentDestination")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            is AuthState.Loading -> {
                // Tidak melakukan navigasi saat loading
                Log.d("CashierApp", "Auth loading state")
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                AppBottomBar(navController = navController)
            }
        }
    ) { paddingValues ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            startDestination = Screen.Splash.route,
            authViewModel = authViewModel
        )
    }
}