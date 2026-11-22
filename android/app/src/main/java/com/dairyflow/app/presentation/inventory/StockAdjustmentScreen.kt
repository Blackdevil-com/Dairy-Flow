package com.dairyflow.app.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.DamagedExpiredRequest
import com.dairyflow.app.data.remote.dto.ProductDto
import com.dairyflow.app.data.remote.dto.StockAdjustmentRequest
import com.dairyflow.app.data.repository.InventoryRepository
import kotlinx.coroutines.launch

@Composable
fun StockAdjustmentScreen(
    inventoryRepo: InventoryRepository,
    onBack: () -> Unit
) {
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<ProductDto?>(null) }
    var adjustmentType by remember { mutableStateOf("IN") } // "IN" or "OUT"
    var quantityStr by remember { mutableStateOf("5.0") }
    var reason by remember { mutableStateOf("Monthly physical count discrepancy") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val res = inventoryRepo.refreshProducts()
        res.onSuccess {
            products = it
            if (it.isNotEmpty()) selectedProduct = it.first()
        }
    }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Stock Adjustment & Write-off",
                subtitle = "Signed ledger update",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhiteBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Adjustment Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = adjustmentType == "IN",
                            onClick = { adjustmentType = "IN" },
                            label = { Text("Stock In (+ Correction)") }
                        )
                        FilterChip(
                            selected = adjustmentType == "OUT",
                            onClick = { adjustmentType = "OUT" },
                            label = { Text("Stock Out (- Shrinkage/Leakage)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select Product:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    products.forEach { prod ->
                        FilterChip(
                            selected = selectedProduct?.id == prod.id,
                            onClick = { selectedProduct = prod },
                            label = { Text("${prod.name} (${prod.canonical_unit_code})") }
                        )
                    }

                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantity (${selectedProduct?.canonical_unit_code ?: ""})") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Mandatory Reason / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            if (message != null) {
                Text(text = message!!, color = if (message!!.contains("Success")) GreenPositive else RedCritical, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (selectedProduct == null) return@Button
                    isSubmitting = true
                    message = null
                    scope.launch {
                        val req = StockAdjustmentRequest(
                            product_id = selectedProduct!!.id,
                            batch_id = null,
                            adjustment_type = adjustmentType,
                            quantity = quantityStr.toDoubleOrNull() ?: 0.0,
                            unit_code = selectedProduct!!.canonical_unit_code,
                            reason = reason.trim()
                        )
                        val res = inventoryRepo.adjustStock(req)
                        isSubmitting = false
                        res.onSuccess {
                            message = "Success! Movement recorded in signed stock ledger."
                        }.onFailure {
                            message = it.message ?: "Adjustment failed"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (adjustmentType == "IN") GreenPositive else AmberWarning),
                enabled = !isSubmitting && selectedProduct != null
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Commit Signed Adjustment", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
