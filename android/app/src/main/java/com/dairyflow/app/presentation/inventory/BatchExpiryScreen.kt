package com.dairyflow.app.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.EmptyState
import com.dairyflow.app.core.components.StatusBadge
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.StockBatchDto
import com.dairyflow.app.data.repository.InventoryRepository
import kotlinx.coroutines.launch

@Composable
fun BatchExpiryScreen(
    inventoryRepo: InventoryRepository,
    onBack: () -> Unit
) {
    var batches by remember { mutableStateOf<List<StockBatchDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        isLoading = true
        scope.launch {
            val res = inventoryRepo.getBatches()
            isLoading = false
            res.onSuccess { batches = it }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Batch & Expiry Management",
                subtitle = "FEFO (First Expiry, First Out) Queue",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && batches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepTeal)
            }
        } else if (batches.isEmpty()) {
            EmptyState(
                icon = Icons.Default.DateRange,
                title = "No Active Batches",
                description = "There are no active batches in the warehouse. Receive inward stock to track batches.",
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
                item {
                    Text(
                        text = "Active Batches Sorted by Expiry (FEFO)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextMuted
                    )
                }

                items(batches.size) { idx ->
                    val b = batches[idx]
                    val isNearExpiry = b.days_to_expiry in 0..3
                    val isExpired = b.days_to_expiry < 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = b.product_name ?: "Product",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Batch No: ${b.batch_no}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepTeal,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                StatusBadge(status = if (isExpired) "EXPIRED" else b.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = OutlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Mfg Date: ${b.mfg_date}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    Text(
                                        text = "Expiry Date: ${b.expiry_date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpired || isNearExpiry) RedCritical else TextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Available: %.2f".format(b.available_base_qty),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = DeepTeal
                                    )
                                    Text(
                                        text = if (isExpired) "Expired" else "${b.days_to_expiry} days remaining",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isNearExpiry || isExpired) RedCritical else GreenPositive,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
