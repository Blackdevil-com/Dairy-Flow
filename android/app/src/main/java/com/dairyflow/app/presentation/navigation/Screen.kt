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
    object CustomerAddEdit : Screen("customer_add_edit?id={id}") {
        fun createRoute(id: String? = null) = if (id != null) "customer_add_edit?id=$id" else "customer_add_edit"
    }
    object CustomerDetails : Screen("customer_details/{id}") {
        fun createRoute(id: String) = "customer_details/$id"
    }
    object NewDelivery : Screen("new_delivery?customerId={customerId}") {
        fun createRoute(customerId: String? = null) = if (customerId != null) "new_delivery?customerId=$customerId" else "new_delivery"
    }
    object DeliveryDetails : Screen("delivery_details/{id}") {
        fun createRoute(id: String) = "delivery_details/$id"
    }
    object InvoicesHistory : Screen("invoices_history")
    object PaymentRecording : Screen("payment_recording")
    object OutstandingBalances : Screen("outstanding_balances")
    object Analytics : Screen("analytics")
}
