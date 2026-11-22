package com.dairyflow.app.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.ProductDto
import com.dairyflow.app.data.remote.dto.StockAdjustmentRequest
import com.dairyflow.app.data.repository.InventoryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(
    inventoryRepo: InventoryRepository,
    onBack: () -> Unit
) {
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<ProductDto?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    // Adjustment categories
    var adjustmentCategory by remember { mutableStateOf("MANUAL") } // MANUAL, DAMAGED, EXPIRED
    var adjustmentDirection by remember { mutableStateOf("OUT") } // "IN" or "OUT"

    var quantityStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
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
                subtitle = "Signed warehouse ledger update",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Adjustment Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = adjustmentCategory == "MANUAL",
                            onClick = {
                                adjustmentCategory = "MANUAL"
                            },
                            label = { Text("Physical Audit") }
                        )
                        FilterChip(
                            selected = adjustmentCategory == "DAMAGED",
                            onClick = {
                                adjustmentCategory = "DAMAGED"
                                adjustmentDirection = "OUT"
                            },
                            label = { Text("Damaged / Leakage") }
                        )
                        FilterChip(
                            selected = adjustmentCategory == "EXPIRED",
                            onClick = {
                                adjustmentCategory = "EXPIRED"
                                adjustmentDirection = "OUT"
                            },
                            label = { Text("Expired Stock") }
                        )
                    }

                    if (adjustmentCategory == "MANUAL") {
                        Text("Direction:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = adjustmentDirection == "IN",
                                onClick = { adjustmentDirection = "IN" },
                                label = { Text("Stock In (+ Excess found)") }
                            )
                            FilterChip(
                                selected = adjustmentDirection == "OUT",
                                onClick = { adjustmentDirection = "OUT" },
                                label = { Text("Stock Out (- Shortage)") }
                            )
                        }
                    }

                    // Product Dropdown Menu
                    Text("Select Product:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    ExposedDropdownMenuBox(
                        expanded = productDropdownExpanded,
                        onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.name ?: "Select a product",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Product *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = productDropdownExpanded,
                            onDismissRequest = { productDropdownExpanded = false }
                        ) {
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(prod.name, fontWeight = FontWeight.Bold)
                                            Text("Current Stock: ${prod.current_stock_base} ${prod.canonical_unit_code}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = prod
                                        productDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Adjustment Quantity (${selectedProduct?.canonical_unit_code ?: "Base"}) *") },
                        placeholder = { Text("e.g. 5.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Mandatory Reason / Audit Note *") },
                        placeholder = { Text("e.g., Cold storage temperature fault batch loss") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            if (message != null) {
                Text(
                    text = message!!,
                    color = if (isError) RedCritical else GreenPositive,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val qty = quantityStr.toDoubleOrNull() ?: 0.0

            Button(
                onClick = {
                    if (selectedProduct == null) {
                        message = "Please select a product."
                        isError = true
                        return@Button
                    }
                    if (qty <= 0) {
                        message = "Quantity must be greater than zero."
                        isError = true
                        return@Button
                    }
                    if (reason.isBlank()) {
                        message = "Mandatory reason / audit note is required."
                        isError = true
                        return@Button
                    }

                    isSubmitting = true
                    message = null
                    scope.launch {
                        val fullReason = "[$adjustmentCategory] ${reason.trim()}"
                        val req = StockAdjustmentRequest(
                            product_id = selectedProduct!!.id,
                            batch_id = null,
                            adjustment_type = adjustmentDirection,
                            quantity = qty,
                            unit_code = selectedProduct!!.canonical_unit_code,
                            reason = fullReason
                        )
                        val res = inventoryRepo.adjustStock(req)
                        isSubmitting = false
                        res.onSuccess {
                            isError = false
                            message = "Success! Movement recorded in signed stock ledger."
                            quantityStr = ""
                            reason = ""
                        }.onFailure {
                            isError = true
                            message = it.message ?: "Stock adjustment rejected by server"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (adjustmentDirection == "IN") GreenPositive else AmberWarning
                ),
                enabled = !isSubmitting && selectedProduct != null && qty > 0 && reason.isNotBlank()
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
