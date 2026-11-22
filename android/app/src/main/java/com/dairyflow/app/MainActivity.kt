package com.dairyflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.dairyflow.app.core.theme.DairyFlowTheme
import com.dairyflow.app.data.repository.*
import com.dairyflow.app.presentation.navigation.NavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authRepo = AuthRepository(this)
        val inventoryRepo = InventoryRepository(this)
        val customerRepo = CustomerRepository(this)
        val deliveryRepo = DeliveryRepository(this)
        val billingRepo = BillingPaymentRepository(this)
        val analyticsRepo = AnalyticsRepository(this)

        setContent {
            DairyFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        authRepo = authRepo,
                        inventoryRepo = inventoryRepo,
                        customerRepo = customerRepo,
                        deliveryRepo = deliveryRepo,
                        billingRepo = billingRepo,
                        analyticsRepo = analyticsRepo
                    )
                }
            }
        }
    }
}
