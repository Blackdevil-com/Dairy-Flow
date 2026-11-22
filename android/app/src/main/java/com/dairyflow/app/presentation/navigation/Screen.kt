package com.dairyflow.app.presentation.navigation

sealed class Screen(val route: String) {
    // Shared
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Profile : Screen("profile")
    object SyncStatus : Screen("sync_status")

    // Inventory Manager
    object InventoryDashboard : Screen("inventory_dashboard")
    object ProductList : Screen("product_list")
    object ProductForm : Screen("product_form")
    object IncomingStock : Screen("incoming_stock")
    object BatchExpiry : Screen("batch_expiry")
    object StockAdjustment : Screen("stock_adjustment")
    object StockLedger : Screen("stock_ledger")
    object LowStockAlerts : Screen("low_stock_alerts")

    // Owner
    object OwnerDashboard : Screen("owner_dashboard")
    object CustomerList : Screen("customer_list")
    object NewDelivery : Screen("new_delivery")
    object DeliveryDetails : Screen("delivery_details/{id}") {
        fun createRoute(id: String) = "delivery_details/$id"
    }
    object InvoicesHistory : Screen("invoices_history")
    object PaymentRecording : Screen("payment_recording")
    object OutstandingBalances : Screen("outstanding_balances")
    object Analytics : Screen("analytics")
}
