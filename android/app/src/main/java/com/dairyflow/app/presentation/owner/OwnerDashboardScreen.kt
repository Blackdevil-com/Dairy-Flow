package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.MetricCard
import com.dairyflow.app.core.components.StatusBadge
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.DailyAnalyticsDto
import com.dairyflow.app.data.remote.dto.DeliveryDto
import com.dairyflow.app.data.remote.dto.StockSummaryDto
import com.dairyflow.app.data.repository.*
import com.dairyflow.app.presentation.inventory.ActionTile
import kotlinx.coroutines.launch

@Composable
fun OwnerDashboardScreen(
    analyticsRepo: AnalyticsRepository,
    inventoryRepo: InventoryRepository,
    deliveryRepo: DeliveryRepository,
    onNavigateToNewDelivery: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToOutstanding: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToDeliveryDetails: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSync: () -> Unit
) {
    var dailySummary by remember { mutableStateOf<DailyAnalyticsDto?>(null) }
    var stockList by remember { mutableStateOf<List<StockSummaryDto>>(emptyList()) }
    var recentDeliveries by remember { mutableStateOf<List<DeliveryDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        isLoading = true
        scope.launch {
            val aRes = analyticsRepo.getDailySummary()
            aRes.onSuccess { dailySummary = it }

            val sRes = inventoryRepo.getInventorySummary()
            sRes.onSuccess { stockList = it }

            val dRes = deliveryRepo.getDeliveries()
            dRes.onSuccess { recentDeliveries = it }

            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "DairyFlow Owner Portal",
                subtitle = "Distribution, Invoicing & Receivables",
                actions = {
                    IconButton(onClick = onNavigateToSync) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewDelivery,
                containerColor = DeepTeal,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                text = { Text("New Delivery", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhiteBackground)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High Level Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Today's Sales",
                        value = "₹ %.2f".format(dailySummary?.total_sales_amount ?: 0.0),
                        subtitle = "${dailySummary?.deliveries_count ?: 0} deliveries today",
                        icon = Icons.Default.CurrencyRupee,
                        iconColor = DeepTeal,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Collections",
                        value = "₹ %.2f".format(dailySummary?.total_collections_amount ?: 0.0),
                        subtitle = "UPI: ₹%.0f | Cash: ₹%.0f".format(dailySummary?.upi_collections ?: 0.0, dailySummary?.cash_collections ?: 0.0),
                        icon = Icons.Default.Payments,
                        iconColor = GreenPositive,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "Commercial Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionTile(
                        title = "Customers Directory",
                        subtitle = "Retail & individual balances",
                        icon = Icons.Default.People,
                        color = DeepTeal,
                        onClick = onNavigateToCustomers,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        title = "Receive Payment",
                        subtitle = "Cash, UPI & Bank transfer",
                        icon = Icons.Default.Payment,
                        color = GreenPositive,
                        onClick = onNavigateToPayments,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionTile(
                        title = "Invoices & PDFs",
                        subtitle = "Download demo-watermarked bills",
                        icon = Icons.Default.Receipt,
                        color = AmberWarning,
                        onClick = onNavigateToInvoices,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        title = "Outstanding Ledger",
                        subtitle = "Accounts receivable & aging",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = RedCritical,
                        onClick = onNavigateToOutstanding,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                ActionTile(
                    title = "Monthly & Yearly Analytics",
                    subtitle = "Turnover, financial year reports, product breakdown",
                    icon = Icons.Default.BarChart,
                    color = TealLight,
                    onClick = onNavigateToAnalytics,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Warehouse Inventory Available to Distribute
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Warehouse Stock Available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = { load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = DeepTeal)
                    }
                }
            }

            items(stockList.size) { idx ->
                val s = stockList[idx]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(s.product_name, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("SKU: ${s.product_sku}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Text(
                            text = "%.2f %s".format(s.available_stock, s.canonical_unit),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepTeal
                        )
                    }
                }
            }

            // Recent Deliveries
            item {
                Text(
                    text = "Recent Deliveries & Dispatches",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(recentDeliveries.size) { idx ->
                val d = recentDeliveries[idx]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDeliveryDetails(d.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.customer_name ?: "Customer", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Deliv No: ${d.delivery_no} | Date: ${d.delivery_date}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            if (d.invoice_no != null) {
                                Text("Invoice: ${d.invoice_no}", style = MaterialTheme.typography.labelSmall, color = DeepTeal, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹ %.2f".format(d.net_amount), fontWeight = FontWeight.Bold, color = TextPrimary)
                            StatusBadge(status = d.status)
                        }
                    }
                }
            }
        }
    }
}
