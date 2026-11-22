package com.dairyflow.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dairyflow.app.data.repository.*
import com.dairyflow.app.presentation.inventory.*
import com.dairyflow.app.presentation.owner.*
import com.dairyflow.app.presentation.shared.*

@Composable
fun NavGraph(
    navController: NavHostController,
    authRepo: AuthRepository,
    inventoryRepo: InventoryRepository,
    customerRepo: CustomerRepository,
    deliveryRepo: DeliveryRepository,
    billingRepo: BillingPaymentRepository,
    analyticsRepo: AnalyticsRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // --- Shared Screens ---
        composable(Screen.Splash.route) {
            SplashScreen(
                isLoggedIn = authRepo.isLoggedIn(),
                userRole = authRepo.role,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOwner = {
                    navController.navigate(Screen.OwnerDashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToManager = {
                    navController.navigate(Screen.InventoryDashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                authRepo = authRepo,
                onLoginSuccess = { role ->
                    if (role == "OWNER") {
                        navController.navigate(Screen.OwnerDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.InventoryDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                authRepo = authRepo,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SyncStatus.route) {
            SyncStatusScreen(onBack = { navController.popBackStack() })
        }

        // --- Inventory Manager Screens (Role Guarded) ---
        composable(Screen.InventoryDashboard.route) {
            InventoryDashboardScreen(
                inventoryRepo = inventoryRepo,
                onNavigateToProducts = { navController.navigate(Screen.ProductList.route) },
                onNavigateToIncomingStock = { navController.navigate(Screen.IncomingStock.route) },
                onNavigateToBatches = { navController.navigate(Screen.BatchExpiry.route) },
                onNavigateToAdjustments = { navController.navigate(Screen.StockAdjustment.route) },
                onNavigateToLedger = { navController.navigate(Screen.StockLedger.route) },
                onNavigateToAlerts = { navController.navigate(Screen.LowStockAlerts.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToSync = { navController.navigate(Screen.SyncStatus.route) }
            )
        }

        composable(Screen.ProductList.route) {
            ProductListScreen(
                inventoryRepo = inventoryRepo,
                onBack = { navController.popBackStack() },
                onNavigateToAddProduct = { navController.navigate(Screen.ProductForm.route) }
            )
        }

        composable(Screen.ProductForm.route) {
            ProductFormScreen(
                inventoryRepo = inventoryRepo,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.IncomingStock.route) {
            IncomingStockScreen(
                inventoryRepo = inventoryRepo,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BatchExpiry.route) {
            BatchExpiryScreen(
                inventoryRepo = inventoryRepo,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StockAdjustment.route) {
            StockAdjustmentScreen(
                inventoryRepo = inventoryRepo,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StockLedger.route) {
            StockLedgerScreen(
                inventoryRepo = inventoryRepo,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LowStockAlerts.route) {
            LowStockAlertsScreen(
                inventoryRepo = inventoryRepo,
                onBack = { navController.popBackStack() },
                onNavigateToIncomingStock = { navController.navigate(Screen.IncomingStock.route) }
            )
        }

        // --- Owner Screens (Role Guarded) ---
        composable(Screen.OwnerDashboard.route) {
            OwnerDashboardScreen(
                analyticsRepo = analyticsRepo,
                inventoryRepo = inventoryRepo,
                deliveryRepo = deliveryRepo,
                onNavigateToNewDelivery = { navController.navigate(Screen.NewDelivery.route) },
                onNavigateToCustomers = { navController.navigate(Screen.CustomerList.route) },
                onNavigateToInvoices = { navController.navigate(Screen.InvoicesHistory.route) },
                onNavigateToPayments = { navController.navigate(Screen.PaymentRecording.route) },
                onNavigateToOutstanding = { navController.navigate(Screen.OutstandingBalances.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToDeliveryDetails = { delivId ->
                    navController.navigate(Screen.DeliveryDetails.createRoute(delivId))
                },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToSync = { navController.navigate(Screen.SyncStatus.route) }
            )
        }

        composable(Screen.CustomerList.route) {
            CustomerManagementScreen(
                customerRepo = customerRepo,
                onBack = { navController.popBackStack() },
                onNavigateToAddCustomer = { navController.navigate(Screen.CustomerAddEdit.createRoute()) },
                onNavigateToEditCustomer = { id -> navController.navigate(Screen.CustomerAddEdit.createRoute(id)) },
                onNavigateToCustomerDetails = { id -> navController.navigate(Screen.CustomerDetails.createRoute(id)) },
                onNavigateToCreateDelivery = { id -> navController.navigate(Screen.NewDelivery.createRoute(id)) }
            )
        }

        composable(
            route = Screen.CustomerAddEdit.route,
            arguments = listOf(navArgument("id") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("id")
            AddEditCustomerScreen(
                customerId = customerId,
                customerRepo = customerRepo,
                onBack = { navController.popBackStack() },
                onCustomerSaved = { savedId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("new_customer_id", savedId)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.CustomerDetails.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("id") ?: ""
            CustomerDetailsScreen(
                customerId = customerId,
                customerRepo = customerRepo,
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate(Screen.CustomerAddEdit.createRoute(id)) },
                onNavigateToCreateDelivery = { id -> navController.navigate(Screen.NewDelivery.createRoute(id)) },
                onNavigateToRecordPayment = { _ -> navController.navigate(Screen.PaymentRecording.route) }
            )
        }

        composable(
            route = Screen.NewDelivery.route,
            arguments = listOf(navArgument("customerId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val custId = backStackEntry.arguments?.getString("customerId")
            NewDeliveryScreen(
                initialCustomerId = custId,
                navController = navController,
                deliveryRepo = deliveryRepo,
                customerRepo = customerRepo,
                inventoryRepo = inventoryRepo,
                onBack = { navController.popBackStack() },
                onNavigateToAddCustomer = { navController.navigate(Screen.CustomerAddEdit.createRoute()) },
                onDeliveryCreated = { delivId ->
                    navController.navigate(Screen.DeliveryDetails.createRoute(delivId)) {
                        popUpTo(Screen.NewDelivery.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.DeliveryDetails.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            DeliveryDetailsScreen(
                deliveryId = id,
                deliveryRepo = deliveryRepo,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.InvoicesHistory.route) {
            InvoicesHistoryScreen(
                billingRepo = billingRepo,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PaymentRecording.route) {
            PaymentRecordingScreen(
                billingRepo = billingRepo,
                customerRepo = customerRepo,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OutstandingBalances.route) {
            OutstandingBalancesScreen(
                billingRepo = billingRepo,
                onBack = { navController.popBackStack() },
                onNavigateToRecordPayment = { navController.navigate(Screen.PaymentRecording.route) }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                analyticsRepo = analyticsRepo,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
