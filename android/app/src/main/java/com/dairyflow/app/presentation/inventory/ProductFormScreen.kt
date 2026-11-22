package com.dairyflow.app.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.ProductCreateRequest
import com.dairyflow.app.data.remote.dto.ProductUnitDto
import com.dairyflow.app.data.repository.InventoryRepository
import kotlinx.coroutines.launch

@Composable
fun ProductFormScreen(
    inventoryRepo: InventoryRepository,
    onBack: () -> Unit
) {
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var canonicalUnit by remember { mutableStateOf("LTR") } // LTR, KG, PKT
    var salePrice by remember { mutableStateOf("60.00") }
    var costPrice by remember { mutableStateOf("48.00") }
    var minStockAlert by remember { mutableStateOf("30.0") }
    var crateConversion by remember { mutableStateOf("12.0") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "New Product",
                subtitle = "Define canonical unit & conversions",
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
                    Text("Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                    
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it.uppercase() },
                        label = { Text("Product SKU Code (e.g. MLK-TON-01)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("Canonical Stock Unit (Base Unit)", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LTR", "KG", "PKT").forEach { unit ->
                            FilterChip(
                                selected = canonicalUnit == unit,
                                onClick = { canonicalUnit = unit },
                                label = { Text(unit) }
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Unit Conversion Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                    Text(
                        text = "Conversions are strictly configured per product. Enter how many base units ($canonicalUnit) are contained in 1 Crate (CRT).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = crateConversion,
                        onValueChange = { crateConversion = it },
                        label = { Text("1 Crate (CRT) = how many $canonicalUnit?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pricing & Alert Thresholds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = salePrice,
                            onValueChange = { salePrice = it },
                            label = { Text("Sale Price (₹)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            label = { Text("Cost Price (₹)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = minStockAlert,
                        onValueChange = { minStockAlert = it },
                        label = { Text("Minimum Stock Alert Threshold ($canonicalUnit)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = RedCritical, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    isSubmitting = true
                    errorMessage = null
                    scope.launch {
                        val units = listOf(
                            ProductUnitDto(
                                id = null,
                                unit_code = "CRT",
                                conversion_to_base = crateConversion.toDoubleOrNull() ?: 12.0
                            )
                        )
                        val req = ProductCreateRequest(
                            category_id = "default", // will be matched to primary
                            sku = sku.trim(),
                            name = name.trim(),
                            description = null,
                            canonical_unit_code = canonicalUnit,
                            default_sale_price = salePrice.toDoubleOrNull() ?: 0.0,
                            default_cost_price = costPrice.toDoubleOrNull() ?: 0.0,
                            min_stock_alert = minStockAlert.toDoubleOrNull() ?: 20.0,
                            units = units
                        )
                        val res = inventoryRepo.createProduct(req)
                        isSubmitting = false
                        res.onSuccess { onBack() }
                           .onFailure { errorMessage = it.message ?: "Failed to save product" }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                enabled = !isSubmitting && sku.isNotBlank() && name.isNotBlank()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Product & Units", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
