package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dairyflow.app.core.components.DairyBottomNavigation
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.MetricCard
import com.dairyflow.app.core.components.StatusBadge
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.DailyAnalyticsDto
import com.dairyflow.app.data.remote.dto.DeliveryDto
import com.dairyflow.app.data.remote.dto.StockSummaryDto
import com.dairyflow.app.data.repository.*
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
                title = "DairyFlow Owner",
                subtitle = "Warehouse & Commercial Hub",
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
        bottomBar = {
            DairyBottomNavigation(
                selectedTab = "HOME",
                onTabSelected = { tab ->
                    when (tab) {
                        "HOME" -> { /* Already here */ }
                        "DELIVERIES" -> onNavigateToNewDelivery()
                        "CUSTOMERS" -> onNavigateToCustomers()
                        "MORE" -> onNavigateToProfile()
                    }
                },
                isOwner = true
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewDelivery,
                containerColor = DairyPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                text = { Text("New Delivery", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DairyBackground)
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
                        iconColor = DairyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Collections",
                        value = "₹ %.2f".format(dailySummary?.total_collections_amount ?: 0.0),
                        subtitle = "UPI: ₹%.0f | Cash: ₹%.0f".format(
                            dailySummary?.upi_collections ?: 0.0,
                            dailySummary?.cash_collections ?: 0.0
                        ),
                        icon = Icons.Default.Payments,
                        iconColor = DairySuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions / Commercial Hub
            item {
                Text(
                    text = "Commercial Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DairyTextPrimary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernActionTile(
                        title = "Customer Accounts",
                        subtitle = "Retail & Individual directory",
                        icon = Icons.Default.Storefront,
                        color = DairyPrimary,
                        onClick = onNavigateToCustomers,
                        modifier = Modifier.weight(1f)
                    )
                    ModernActionTile(
                        title = "Receive Payment",
                        subtitle = "Cash, UPI & Bank transfer",
                        icon = Icons.Default.Payment,
                        color = DairySuccess,
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
                    ModernActionTile(
                        title = "Invoices & PDFs",
                        subtitle = "Download demo bills",
                        icon = Icons.Default.Receipt,
                        color = DairyWarning,
                        onClick = onNavigateToInvoices,
                        modifier = Modifier.weight(1f)
                    )
                    ModernActionTile(
                        title = "Outstanding Ledger",
                        subtitle = "Accounts receivable & aging",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = DairyCritical,
                        onClick = onNavigateToOutstanding,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                ModernActionTile(
                    title = "Turnover & Analytics",
                    subtitle = "Monthly & annual sales turnover breakdown",
                    icon = Icons.Default.BarChart,
                    color = DairyBlue,
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
                        color = DairyTextPrimary
                    )
                    IconButton(onClick = { load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = DairyPrimary)
                    }
                }
            }

            items(stockList.size) { idx ->
                val s = stockList[idx]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DairySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DairyBorder)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(s.product_name, fontWeight = FontWeight.Bold, color = DairyTextPrimary)
                            Text("SKU: ${s.product_sku}", style = MaterialTheme.typography.bodySmall, color = DairyTextMuted)
                        }
                        Box(
                            modifier = Modifier
                                .background(DairyPrimaryLight, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "%.2f %s".format(s.available_stock, s.canonical_unit),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = DairyPrimary
                            )
                        }
                    }
                }
            }

            // Recent Deliveries
            item {
                Text(
                    text = "Recent Deliveries & Dispatches",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DairyTextPrimary
                )
            }

            items(recentDeliveries.size) { idx ->
                val d = recentDeliveries[idx]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDeliveryDetails(d.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DairySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DairyBorder)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.customer_name ?: "Customer", fontWeight = FontWeight.Bold, color = DairyTextPrimary)
                            Text("Deliv: ${d.delivery_no} • ${d.delivery_date}", style = MaterialTheme.typography.bodySmall, color = DairyTextMuted)
                            if (d.invoice_no != null) {
                                Text("Inv: ${d.invoice_no}", style = MaterialTheme.typography.labelSmall, color = DairyPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹ %.2f".format(d.net_amount), fontWeight = FontWeight.Bold, color = DairyTextPrimary)
                            StatusBadge(status = d.status)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun ModernActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DairySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DairyBorder)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = DairyTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DairyTextMuted,
                maxLines = 2
            )
        }
    }
}
