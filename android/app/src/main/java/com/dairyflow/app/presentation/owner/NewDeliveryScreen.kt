package com.dairyflow.app.presentation.owner

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
import com.dairyflow.app.data.remote.dto.*
import com.dairyflow.app.data.repository.CustomerRepository
import com.dairyflow.app.data.repository.DeliveryRepository
import com.dairyflow.app.data.repository.InventoryRepository
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun NewDeliveryScreen(
    deliveryRepo: DeliveryRepository,
    customerRepo: CustomerRepository,
    inventoryRepo: InventoryRepository,
    onBack: () -> Unit,
    onDeliveryCreated: (String) -> Unit
) {
    var customers by remember { mutableStateOf<List<CustomerDto>>(emptyList()) }
    var selectedCustomer by remember { mutableStateOf<CustomerDto?>(null) }
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<ProductDto?>(null) }
    var unitCode by remember { mutableStateOf("LTR") }
    var quantityStr by remember { mutableStateOf("20.0") }
    var unitPriceStr by remember { mutableStateOf("56.00") }
    var notes by remember { mutableStateOf("Morning milk run dispatch") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val cRes = customerRepo.refreshCustomers()
        cRes.onSuccess {
            customers = it
            if (it.isNotEmpty()) selectedCustomer = it.first()
        }

        val pRes = inventoryRepo.refreshProducts()
        pRes.onSuccess {
            products = it
            if (it.isNotEmpty()) {
                selectedProduct = it.first()
                unitCode = it.first().canonical_unit_code
                unitPriceStr = it.first().default_sale_price.toString()
            }
        }
    }

    val qty = quantityStr.toDoubleOrNull() ?: 0.0
    val price = unitPriceStr.toDoubleOrNull() ?: 0.0
    val subtotal = qty * price

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Create Delivery Run",
                subtitle = "Draft dispatch order",
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Customer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                    customers.forEach { c ->
                        FilterChip(
                            selected = selectedCustomer?.id == c.id,
                            onClick = { selectedCustomer = c },
                            label = { Text("${c.trade_name ?: c.name} (₹${c.current_balance} bal)") }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Product & Quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)

                    Text("Product:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    products.forEach { prod ->
                        FilterChip(
                            selected = selectedProduct?.id == prod.id,
                            onClick = {
                                selectedProduct = prod
                                unitCode = prod.canonical_unit_code
                                unitPriceStr = prod.default_sale_price.toString()
                            },
                            label = { Text("${prod.name} (avail: ${prod.current_stock_base} ${prod.canonical_unit_code})") }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Delivery Unit:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val availableUnits = mutableListOf<String>()
                        selectedProduct?.canonical_unit_code?.let { availableUnits.add(it) }
                        selectedProduct?.units?.forEach { availableUnits.add(it.unit_code) }
                        availableUnits.distinct().forEach { u ->
                            FilterChip(
                                selected = unitCode == u,
                                onClick = { unitCode = u },
                                label = { Text(u) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Quantity ($unitCode)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = unitPriceStr,
                            onValueChange = { unitPriceStr = it },
                            label = { Text("Price per $unitCode (₹)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Calculation Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Estimated Bill Amount: ₹ %.2f".format(subtotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepTeal
                            )
                            Text(
                                text = "Stock will be deducted from warehouse using FEFO upon confirmation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = RedCritical, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    if (selectedCustomer == null || selectedProduct == null) return@Button
                    isSubmitting = true
                    errorMessage = null
                    scope.launch {
                        val req = DeliveryCreateRequest(
                            customer_id = selectedCustomer!!.id,
                            discount = 0.0,
                            notes = notes,
                            idempotency_key = UUID.randomUUID().toString(),
                            items = listOf(
                                DeliveryItemRequest(
                                    product_id = selectedProduct!!.id,
                                    unit_code = unitCode,
                                    requested_qty = qty,
                                    unit_price = price
                                )
                            )
                        )
                        val res = deliveryRepo.createDraft(req)
                        isSubmitting = false
                        res.onSuccess { deliv ->
                            onDeliveryCreated(deliv.id)
                        }.onFailure {
                            errorMessage = it.message ?: "Failed to draft delivery"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                enabled = !isSubmitting && selectedCustomer != null && selectedProduct != null && qty > 0
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Delivery Draft", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
