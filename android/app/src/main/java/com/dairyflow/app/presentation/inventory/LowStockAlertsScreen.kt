package com.dairyflow.app.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.EmptyState
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.StockSummaryDto
import com.dairyflow.app.data.repository.InventoryRepository
import kotlinx.coroutines.launch

@Composable
fun LowStockAlertsScreen(
    inventoryRepo: InventoryRepository,
    onBack: () -> Unit,
    onNavigateToIncomingStock: () -> Unit
) {
    var alerts by remember { mutableStateOf<List<StockSummaryDto>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val res = inventoryRepo.getInventorySummary()
            res.onSuccess { list ->
                alerts = list.filter { it.is_low_stock }
            }
        }
    }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Critical Low-Stock Alerts",
                subtitle = "Warehouse re-order notifications",
                onBack = onBack
            )
        }
    ) { padding ->
        if (alerts.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Warning,
                title = "Inventory Healthy",
                description = "All products currently meet or exceed their configured minimum warehouse threshold.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OffWhiteBackground)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alerts.size) { idx ->
                    val a = alerts[idx]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = a.product_name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = RedCritical
                                    )
                                    Text(
                                        text = "SKU: ${a.product_sku}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(RedContainer, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ALERT",
                                        color = RedCritical,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = OutlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Current Stock: %.2f %s".format(a.available_stock, a.canonical_unit), fontWeight = FontWeight.Bold, color = RedCritical)
                                    Text("Min Threshold: %.2f %s".format(a.min_stock_alert, a.canonical_unit), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                Button(
                                    onClick = onNavigateToIncomingStock,
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Receive Stock", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
