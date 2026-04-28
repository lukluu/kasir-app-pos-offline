package com.example.kasirku.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Splash : Screen("splash")
    // Halaman utama
    object Dashboard : Screen("dashboard")
    object Pesanan : Screen("pesanan")
    object BuatPesanan : Screen("buat_pesanan")
    object Laporan : Screen("laporan")
    object ReportDetail : Screen("report_detail/{type}") {
        fun createRoute(type: String) = "report_detail/$type"
    }
    object Pengaturan : Screen("pengaturan")
    object Category : Screen("category")
    object Product : Screen("product")
    object AddEditProduct : Screen("add_edit_product?productId={productId}") {
        fun createRoute(productId: Int?): String {
            return if (productId != null) "add_edit_product?productId=$productId" else "add_edit_product"
        }
    }
    object Customer : Screen("customer")


    // Grafik Navigasi untuk Pembuatan Pesanan
//    object CreateOrderGraph : Screen("create_order_graph")
    object ProductSelection : Screen("product_selection")
    object OrderSummary : Screen("order_summary")

    // Halaman Daftar Pesanan
    object OrderList : Screen("order_list")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: Long) = "order_detail/$orderId"
    }

    // === TAMBAHKAN INI ===
    object Receipt : Screen("receipt/{orderId}") {
        fun createRoute(orderId: Long) = "receipt/$orderId"
    }

    // Update route agar bisa terima parameter opsional orderId
    object CreateOrderGraph : Screen("create_order_graph?orderId={orderId}&mode={mode}") {
        fun createRoute(orderId: Long? = null, mode: String = "EDIT"): String {
            val baseId = orderId ?: -1L
            return "create_order_graph?orderId=$baseId&mode=$mode"
        }
    }

    object SettingPrinters : Screen("setting_printers")
    object SettingPaymentMethods : Screen("setting_payment_methods")

}
