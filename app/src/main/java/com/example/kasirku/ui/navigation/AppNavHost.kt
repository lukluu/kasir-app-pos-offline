package com.example.kasirku.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.kasirku.ui.screen.auth.LoginScreen
import com.example.kasirku.ui.screen.auth.RegisterScreen
import com.example.kasirku.ui.screen.buatpesanan.CreateOrderViewModel
import com.example.kasirku.ui.screen.buatpesanan.OrderSummaryScreen
import com.example.kasirku.ui.screen.buatpesanan.ProductSelectionScreen
import com.example.kasirku.ui.screen.category.CategoryScreen
import com.example.kasirku.ui.screen.customer.CustomerScreen
import com.example.kasirku.ui.screen.dashboard.DashboardScreen
import com.example.kasirku.ui.screen.laporan.ReportDetailScreen
import com.example.kasirku.ui.screen.laporan.ReportDetailViewModel
import com.example.kasirku.ui.screen.laporan.ReportScreen
import com.example.kasirku.ui.screen.pengaturan.PaymentMethodScreen
import com.example.kasirku.ui.screen.pengaturan.PrinterScreen
import com.example.kasirku.ui.screen.pengaturan.SettingScreen
import com.example.kasirku.ui.screen.pesanan.OrderDetailScreen
import com.example.kasirku.ui.screen.pesanan.OrderListScreen
import com.example.kasirku.ui.screen.pesanan.ReceiptScreen
import com.example.kasirku.ui.screen.pesanan.ReceiptViewModel
import com.example.kasirku.ui.screen.product.AddEditProductScreen
import com.example.kasirku.ui.screen.product.ProductScreen
import com.example.kasirku.ui.screen.splash.SplashScreen
import com.example.kasirku.ui.viewmodel.AuthViewModel

// Durasi standar untuk transisi
const val ANIM_DURATION = 300

// Helper Transisi Horizontal (Digunakan untuk Detail Pages)
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInLeft() =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(ANIM_DURATION))
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutRight() =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(ANIM_DURATION))
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInRight() =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(ANIM_DURATION))
fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutLeft() =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(ANIM_DURATION))
fun fadeInStandard() = fadeIn(animationSpec = tween(ANIM_DURATION))
fun fadeOutStandard() = fadeOut(animationSpec = tween(ANIM_DURATION))

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.createOrderGraph(navController: NavController) {
    navigation(
        startDestination = Screen.ProductSelection.route,
        route = Screen.CreateOrderGraph.route,
        arguments = listOf(navArgument("orderId") {
            type = NavType.LongType; defaultValue = -1L
        })
        // Transisi untuk masuk/keluar dari Graph Pesanan (Seperti modal slide dari bawah)
        ,enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(ANIM_DURATION)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(ANIM_DURATION)) }
    ) {
        composable(
            route = Screen.ProductSelection.route,
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.CreateOrderGraph.route)
            }
            val viewModel: CreateOrderViewModel = hiltViewModel(parentEntry)

            val orderIdToEdit = parentEntry.arguments?.getLong("orderId") ?: -1L
            LaunchedEffect(orderIdToEdit) {
                if (orderIdToEdit != -1L) {
                    viewModel.loadOrderForEditing(orderIdToEdit)
                }
            }
            ProductSelectionScreen(navController = navController, viewModel = viewModel)
        }
        composable(Screen.OrderSummary.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.CreateOrderGraph.route)
            }
            val viewModel: CreateOrderViewModel = hiltViewModel(parentEntry)
            OrderSummaryScreen(navController = navController, viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String,
    authViewModel: AuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
//        composable(Screen.Splash.route) {
//            SplashScreen(navController = navController)
//        }
        composable(Screen.Splash.route) { backStackEntry ->
            SplashScreen(
                navController = navController,
                authViewModel = authViewModel // ⚠️ PASS authViewModel
            )
        }
        // --- AUTENTIKASI & DASHBOARD (FADE IN/OUT) ---
        composable(
            Screen.Login.route,
            enterTransition = { fadeInStandard() },
            exitTransition = { fadeOutStandard() }
        ) { backStackEntry ->
            // ⚠️ PERBAIKAN: Gunakan hiltViewModel() dengan backStackEntry
            val loginViewModel: AuthViewModel = hiltViewModel(backStackEntry)
            LoginScreen(
                navController = navController,
                viewModel = loginViewModel
            )
        }
        composable(
            Screen.Register.route,
            enterTransition = { slideInLeft() },
            exitTransition = { fadeOutStandard() }
        ) {
            RegisterScreen(navController = navController, viewModel = authViewModel)
        }

        composable(
            Screen.Dashboard.route,
            enterTransition = { fadeInStandard() },
            exitTransition = { fadeOutStandard() }
        ) {
            DashboardScreen(navController = navController)
        }

        // --- MASTER/DETAIL SCREENS (SLIDE KIRI/KANAN) ---

        // Category
        composable(
            Screen.Category.route,
            enterTransition = { slideInLeft() }, exitTransition = { slideOutLeft() },
            popEnterTransition = { slideInRight() }, popExitTransition = { slideOutLeft() }
        ) {
            CategoryScreen(navController = navController, viewModel = hiltViewModel())
        }

        // Product List
        composable(
            Screen.Product.route,
            enterTransition = { slideInLeft() }, exitTransition = { slideOutLeft() },
            popEnterTransition = { slideInRight() }, popExitTransition = { slideOutRight() }
        ) {
            ProductScreen(navController = navController, viewModel = hiltViewModel())
        }

        // Add/Edit Product (Internal Detail)
        composable(
            route = Screen.AddEditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.IntType; defaultValue = -1 }),
            enterTransition = { slideInLeft() }, exitTransition = { slideOutLeft() },
            popEnterTransition = { slideInRight() }, popExitTransition = { slideOutRight() }
        ) {
            AddEditProductScreen(navController = navController, viewModel = hiltViewModel())
        }

        // Customer List
        composable(
            Screen.Customer.route,
            enterTransition = { slideInLeft() }, exitTransition = { slideOutLeft() },
            popEnterTransition = { slideInRight() }, popExitTransition = { slideOutRight() }
        ) {
            CustomerScreen(navController = navController, viewModel = hiltViewModel())
        }

        // Order List
        composable(
            Screen.Pesanan.route,
            enterTransition = { fadeInStandard() }, exitTransition = { fadeOutStandard() }
        ) {
            OrderListScreen(navController = navController, viewModel = hiltViewModel())
        }

        // Order Detail (Drill Down)
        composable(
            route = Screen.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType }),
            enterTransition = { slideInLeft() }, exitTransition = { slideOutLeft() },
            popEnterTransition = { slideInRight() }, popExitTransition = { slideOutRight() }
        ) {
            OrderDetailScreen(navController = navController, viewModel = hiltViewModel())
        }

        // Receipt (Modal)
        composable(
            route = Screen.Receipt.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(ANIM_DURATION)) },
            exitTransition = { fadeOutStandard() }
        ) {
            val viewModel = hiltViewModel<ReceiptViewModel>()
            ReceiptScreen(navController = navController, viewModel = viewModel)
        }

        // Laporan (Slide for master report screen)
        composable(
            Screen.Laporan.route,
            enterTransition = { fadeInStandard() }, exitTransition = { fadeOutStandard() }
        ) {
            ReportScreen(navController = navController)
        }

        // Report Detail (Drill Down)
        composable(
            route = Screen.ReportDetail.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
            enterTransition = { slideInLeft() }, exitTransition = { slideOutLeft() },
            popEnterTransition = { slideInRight() }, popExitTransition = { slideOutRight() }
        ) {
            val viewModel = hiltViewModel<ReportDetailViewModel>()
            ReportDetailScreen(navController = navController, viewModel = viewModel)
        }

        // Pengaturan (Master screen)
        composable(
            Screen.Pengaturan.route,
            enterTransition = { fadeInStandard() }, exitTransition = { fadeOutStandard() }
        ) {
            SettingScreen(navController = navController)
        }

        // Printer Screen
        composable(
            Screen.SettingPrinters.route,
            enterTransition = { slideInLeft() }, exitTransition = { slideOutLeft() },
            popEnterTransition = { slideInRight() }, popExitTransition = { slideOutRight() }
        ) {
            PrinterScreen(navController = navController)
        }

        // Payment Method Screen
        composable(
            Screen.SettingPaymentMethods.route,
            enterTransition = { slideInLeft() }, exitTransition = { slideOutLeft() },
            popEnterTransition = { slideInRight() }, popExitTransition = { slideOutRight() }
        ) {
            PaymentMethodScreen(navController = navController)
        }

        // Daftarkan grafik navigasi bersarang untuk Buat Pesanan
        createOrderGraph(navController)
    }
}