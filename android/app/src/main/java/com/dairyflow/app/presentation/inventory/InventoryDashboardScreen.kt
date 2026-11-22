package com.dairyflow.app.presentation.inventory

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
import com.dairyflow.app.data.remote.dto.StockSummaryDto
import com.dairyflow.app.data.repository.InventoryRepository
import kotlinx.coroutines.launch

@Composable
fun InventoryDashboardScreen(
    inventoryRepo: InventoryRepository,
    onNavigateToProducts: () -> Unit,
    onNavigateToIncomingStock: () -> Unit,
    onNavigateToBatches: () -> Unit,
    onNavigateToAdjustments: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSync: () -> Unit
) {
    var summaryList by remember { mutableStateOf<List<StockSummaryDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        isLoading = true
        scope.launch {
            val res = inventoryRepo.getInventorySummary()
            isLoading = false
            res.onSuccess { summaryList = it }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val lowStockCount = summaryList.count { it.is_low_stock }
    val totalStockLitres = summaryList.filter { it.canonical_unit == "LTR" }.sumOf { it.available_stock }
    val totalStockKg = summaryList.filter { it.canonical_unit == "KG" }.sumOf { it.available_stock }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Warehouse Inventory",
                subtitle = "Depot: WH-CENTRAL (Manager)",
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
                selectedTab = "STOCK",
                onTabSelected = { tab ->
                    when (tab) {
                        "STOCK" -> { /* Already here */ }
                        "PRODUCTS" -> onNavigateToProducts()
                        "INWARD" -> onNavigateToIncomingStock()
                        "MORE" -> onNavigateToBatches()
                    }
                },
                isOwner = false
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToIncomingStock,
                containerColor = DairyPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddBox, contentDescription = null) },
                text = { Text("Stock In", fontWeight = FontWeight.Bold) },
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
            // Low Stock Alert Banner if any
            if (lowStockCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToAlerts() },
                        colors = CardDefaults.cardColors(containerColor = DairyCriticalLight),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DairyCritical.copy(alpha = 0.3f)))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DairyCritical.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DairyCritical, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$lowStockCount Low-Stock Alert(s) Detected",
                                    fontWeight = FontWeight.Bold,
                                    color = DairyCritical,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Stock is below minimum warehouse threshold. Tap to inspect.",
                                    color = DairyCritical.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DairyCritical)
                        }
                    }
                }
            }

            // High level metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Liquid Milk",
                        value = "%.1f LTR".format(totalStockLitres),
                        icon = Icons.Default.WaterDrop,
                        iconColor = DairyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Solid Dairy",
                        value = "%.1f KG".format(totalStockKg),
                        icon = Icons.Default.Inventory2,
                        iconColor = DairyWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Operations Section
            item {
                Text(
                    text = "Warehouse Operations",
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
                    ActionTile(
                        title = "Inward Stock",
                        subtitle = "Receive shipments",
                        icon = Icons.Default.AddBox,
                        color = DairySuccess,
                        onClick = onNavigateToIncomingStock,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        title = "Product Catalog",
                        subtitle = "Units & conversions",
                        icon = Icons.Default.Category,
                        color = DairyPrimary,
                        onClick = onNavigateToProducts,
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
                        title = "Batch & Expiry",
                        subtitle = "FEFO dispatch queue",
                        icon = Icons.Default.DateRange,
                        color = DairyWarning,
                        onClick = onNavigateToBatches,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        title = "Adjustments",
                        subtitle = "Correction & write-off",
                        icon = Icons.Default.Tune,
                        color = DairyCritical,
                        onClick = onNavigateToAdjustments,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                ActionTile(
                    title = "Stock Movement Ledger",
                    subtitle = "Signed audit trail of inward & dispatch movements",
                    icon = Icons.Default.HistoryEdu,
                    color = DairyBlue,
                    onClick = onNavigateToLedger,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Stock Balances Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Stock Balances",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DairyTextPrimary
                    )
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = DairyPrimary)
                    }
                }
            }

            items(summaryList.size) { idx ->
                val item = summaryList[idx]
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product_name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = DairyTextPrimary
                            )
                            Text(
                                text = "SKU: ${item.product_sku} • ${item.active_batches_count} active batch(es)",
                                style = MaterialTheme.typography.bodySmall,
                                color = DairyTextMuted
                            )
                            if (item.nearest_expiry != null) {
                                Text(
                                    text = "Nearest Expiry: ${item.nearest_expiry}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.is_low_stock) DairyCritical else DairyTextSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (item.is_low_stock) DairyCriticalLight else DairyPrimaryLight,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "%.2f %s".format(item.available_stock, item.canonical_unit),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (item.is_low_stock) DairyCritical else DairyPrimary
                                )
                            }
                            if (item.is_low_stock) {
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusBadge(status = "LOW STOCK", isPositive = false)
                            }
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
fun ActionTile(
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
