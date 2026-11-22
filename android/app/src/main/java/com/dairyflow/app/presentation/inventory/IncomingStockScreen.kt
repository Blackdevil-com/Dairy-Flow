package com.dairyflow.app.presentation.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.network.ApiClient
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.*
import com.dairyflow.app.data.repository.InventoryRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomingStockScreen(
    inventoryRepo: InventoryRepository,
    onBack: () -> Unit
) {
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<ProductDto?>(null) }
    var suppliers by remember { mutableStateOf<List<SupplierDto>>(emptyList()) }
    var selectedSupplier by remember { mutableStateOf<SupplierDto?>(null) }
    
    var quantityStr by remember { mutableStateOf("10.0") }
    var unitCode by remember { mutableStateOf("CRT") }
    var batchNo by remember { mutableStateOf("BATCH-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}-01") }
    var unitCostStr by remember { mutableStateOf("45.00") }
    var invoiceRef by remember { mutableStateOf("PO-${SimpleDateFormat("MMdd", Locale.getDefault()).format(Date())}-001") }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoadingInit by remember { mutableStateOf(true) }
    var successReceipt by remember { mutableStateOf<PurchaseReceiptDto?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Dates
    val cal = Calendar.getInstance()
    val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, 4)
    val expiryIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

    // Load products and suppliers
    LaunchedEffect(Unit) {
        isLoadingInit = true
        scope.launch {
            val pRes = inventoryRepo.refreshProducts()
            pRes.onSuccess { list ->
                products = list
                if (list.isNotEmpty()) {
                    selectedProduct = list.first()
                    unitCode = list.first().units.firstOrNull()?.unit_code ?: list.first().canonical_unit_code
                }
            }
            try {
                val sList = ApiClient.apiService.getSuppliers()
                suppliers = sList
                if (sList.isNotEmpty()) {
                    selectedSupplier = sList.first()
                }
            } catch (ignored: Exception) {}
            isLoadingInit = false
        }
    }

    // Calculations
    val qty = quantityStr.toDoubleOrNull() ?: 0.0
    val costRate = unitCostStr.toDoubleOrNull() ?: 0.0
    val totalCost = qty * costRate

    val conversionFactor = if (unitCode == selectedProduct?.canonical_unit_code) 1.0 else {
        selectedProduct?.units?.find { it.unit_code == unitCode }?.conversion_to_base ?: 1.0
    }
    val calculatedBaseQty = qty * conversionFactor

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Inward Stock Receipt",
                subtitle = "Batch Tracking & Conversion Ledger",
                onBack = onBack
            )
        }
    ) { padding ->
        if (isLoadingInit) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DeepTeal)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OffWhiteBackground)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Success Banner
                AnimatedVisibility(visible = successReceipt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = GreenContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPositive, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Stock Successfully Received!",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GreenPositive
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Receipt: ${successReceipt?.receipt_no} | Status: ${successReceipt?.status}\nStock movement ledger and FEFO batches updated.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkTeal
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        successReceipt = null
                                        batchNo = "BATCH-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())}"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenPositive),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Receive Another Item", color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = onBack,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Back to Dashboard", color = GreenPositive)
                                }
                            }
                        }
                    }
                }

                // Error Banner
                AnimatedVisibility(visible = errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = RedContainer)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RedCritical)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = RedCritical,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Section 1: Supplier Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1. Dairy Supplier Source", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        }

                        if (suppliers.isEmpty()) {
                            Text(
                                text = "Default: Primary Dairy Consortium Co-operative",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                suppliers.forEach { supp ->
                                    val isSelected = selectedSupplier?.id == supp.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSupplier = supp },
                                        label = { Text(supp.name) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TealContainer,
                                            selectedLabelColor = DarkTeal
                                        )
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = invoiceRef,
                            onValueChange = { invoiceRef = it },
                            label = { Text("Supplier Invoice / PO Reference") },
                            leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = DeepTeal) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                // Section 2: Product & Conversion Calculator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("2. Product & Mixed Unit Conversions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        }

                        Text("Select Product:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            products.forEach { prod ->
                                val isSelected = selectedProduct?.id == prod.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedProduct = prod
                                        unitCode = prod.units.firstOrNull()?.unit_code ?: prod.canonical_unit_code
                                        unitCostStr = prod.default_cost_price.toString()
                                    },
                                    label = { Text(prod.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealContainer,
                                        selectedLabelColor = DarkTeal
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Reception Unit (Mixed Units Supported):", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val availableUnits = mutableListOf<String>()
                            selectedProduct?.canonical_unit_code?.let { availableUnits.add(it) }
                            selectedProduct?.units?.forEach { availableUnits.add(it.unit_code) }
                            availableUnits.distinct().forEach { u ->
                                FilterChip(
                                    selected = unitCode == u,
                                    onClick = { unitCode = u },
                                    label = { Text(u) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealContainer,
                                        selectedLabelColor = DarkTeal
                                    )
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = quantityStr,
                                onValueChange = { quantityStr = it },
                                label = { Text("Quantity ($unitCode)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = unitCostStr,
                                onValueChange = { unitCostStr = it },
                                label = { Text("Cost Rate / $unitCode (₹)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // Hero Visual Live Base Conversion Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TealContainer)
                                .border(1.dp, TealMedium.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CANONICAL INVENTORY EQUIVALENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(GreenContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "1 $unitCode = $conversionFactor ${selectedProduct?.canonical_unit_code ?: ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GreenPositive,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "%.2f %s = %.2f %s".format(
                                        qty, unitCode, calculatedBaseQty, selectedProduct?.canonical_unit_code ?: ""
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepTeal
                                )
                                Text(
                                    text = "Estimated Purchase Total: ₹ %.2f".format(totalCost),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Section 3: Batch Number & FEFO Shelf-Life
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("3. Batch & Expiry Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        }

                        OutlinedTextField(
                            value = batchNo,
                            onValueChange = { batchNo = it.uppercase() },
                            label = { Text("Batch Number") },
                            leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = DeepTeal) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = todayIso,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mfg Date") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = expiryIso,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Expiry Date") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Button
                Button(
                    onClick = {
                        if (selectedProduct == null) return@Button
                        isSubmitting = true
                        errorMessage = null
                        successReceipt = null

                        scope.launch {
                            val supplierId = selectedSupplier?.id ?: "default"
                            val req = PurchaseReceiptRequest(
                                supplier_id = supplierId,
                                invoice_reference = invoiceRef.trim(),
                                notes = "Received via warehouse mobile app",
                                items = listOf(
                                    PurchaseItemRequest(
                                        product_id = selectedProduct!!.id,
                                        batch_no = batchNo.trim(),
                                        quantity_received = qty,
                                        unit_code = unitCode,
                                        unit_cost = costRate,
                                        mfg_date = todayIso,
                                        expiry_date = expiryIso
                                    )
                                )
                            )
                            val res = inventoryRepo.receivePurchase(req)
                            isSubmitting = false
                            res.onSuccess { receipt ->
                                successReceipt = receipt
                            }.onFailure { ex ->
                                errorMessage = ex.message ?: "Failed to post purchase receipt"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPositive),
                    enabled = !isSubmitting && selectedProduct != null && qty > 0
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Posting to Ledger...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Post Inward Stock & Update Ledger", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
