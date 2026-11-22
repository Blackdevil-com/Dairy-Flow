package com.dairyflow.app.presentation.inventory

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                subtitle = "Depot: WH-CENTRAL (Role: Manager)",
                actions = {
                    IconButton(onClick = onNavigateToSync) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
                    }
                }
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
            // Low Stock Alert Banner if any
            if (lowStockCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAlerts() },
                        colors = CardDefaults.cardColors(containerColor = RedContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RedCritical)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$lowStockCount Low-Stock Alert(s) Detected",
                                    fontWeight = FontWeight.Bold,
                                    color = RedCritical,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Stock is below minimum warehouse threshold. Tap to inspect.",
                                    color = RedCritical.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RedCritical)
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
                        title = "Milk Stock",
                        value = "%.1f L".format(totalStockLitres),
                        icon = Icons.Default.WaterDrop,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Solid Dairy",
                        value = "%.1f kg".format(totalStockKg),
                        icon = Icons.Default.Inventory2,
                        iconColor = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions Section
            item {
                Text(
                    text = "Warehouse Operations",
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
                        title = "Inward Stock",
                        subtitle = "Receive shipments",
                        icon = Icons.Default.AddBox,
                        color = GreenPositive,
                        onClick = onNavigateToIncomingStock,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        title = "Product Catalog",
                        subtitle = "Units & conversions",
                        icon = Icons.Default.Category,
                        color = DeepTeal,
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
                        subtitle = "FEFO queue",
                        icon = Icons.Default.DateRange,
                        color = AmberWarning,
                        onClick = onNavigateToBatches,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        title = "Adjustments",
                        subtitle = "Correction & write-off",
                        icon = Icons.Default.Tune,
                        color = TealLight,
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
                    color = DeepTeal,
                    onClick = onNavigateToLedger,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Stock Overview List
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
                        color = TextPrimary
                    )
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = DeepTeal)
                    }
                }
            }

            items(summaryList.size) { idx ->
                val item = summaryList[idx]
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product_name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "SKU: ${item.product_sku} | Active Batches: ${item.active_batches_count}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            if (item.nearest_expiry != null) {
                                Text(
                                    text = "Nearest Expiry: ${item.nearest_expiry}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.is_low_stock) RedCritical else TextSecondary
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "%.2f %s".format(item.available_stock, item.canonical_unit),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (item.is_low_stock) RedCritical else DeepTeal
                            )
                            StatusBadge(status = if (item.is_low_stock) "LOW STOCK" else "ACTIVE")
                        }
                    }
                }
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
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}
