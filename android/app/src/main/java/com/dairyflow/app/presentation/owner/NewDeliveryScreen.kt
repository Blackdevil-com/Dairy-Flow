package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.StatusBadge
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.*
import com.dairyflow.app.data.repository.CustomerRepository
import com.dairyflow.app.data.repository.DeliveryRepository
import com.dairyflow.app.data.repository.InventoryRepository
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDeliveryScreen(
    initialCustomerId: String? = null,
    deliveryRepo: DeliveryRepository,
    customerRepo: CustomerRepository,
    inventoryRepo: InventoryRepository,
    onBack: () -> Unit,
    onNavigateToAddCustomer: () -> Unit,
    onDeliveryCreated: (String) -> Unit
) {
    var customers by remember { mutableStateOf<List<CustomerDto>>(emptyList()) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<CustomerDto?>(null) }
    var deliveryAddressOverride by remember { mutableStateOf("") }

    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<ProductDto?>(null) }
    var productExpanded by remember { mutableStateOf(false) }

    var unitCode by remember { mutableStateOf("LTR") }
    var quantityStr by remember { mutableStateOf("20.0") }
    var unitPriceStr by remember { mutableStateOf("56.00") }
    var notes by remember { mutableStateOf("Morning milk run dispatch") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val cRes = customerRepo.refreshCustomers()
        cRes.onSuccess { list ->
            customers = list
            if (initialCustomerId != null) {
                selectedCustomer = list.find { it.id == initialCustomerId }
                selectedCustomer?.let {
                    deliveryAddressOverride = it.delivery_address ?: it.address ?: ""
                }
            }
        }

        val pRes = inventoryRepo.refreshProducts()
        pRes.onSuccess { prodList ->
            products = prodList
            if (prodList.isNotEmpty()) {
                val first = prodList.first()
                selectedProduct = first
                unitCode = first.canonical_unit_code
                unitPriceStr = first.default_sale_price.toString()
            }
        }
    }

    LaunchedEffect(initialCustomerId, customers) {
        if (initialCustomerId != null && selectedCustomer == null) {
            val match = customers.find { it.id == initialCustomerId }
            if (match != null) {
                selectedCustomer = match
                deliveryAddressOverride = match.delivery_address ?: match.address ?: ""
            }
        }
    }

    val qty = quantityStr.toDoubleOrNull() ?: 0.0
    val price = unitPriceStr.toDoubleOrNull() ?: 0.0
    val subtotal = qty * price

    val filteredCustomers = remember(customerSearchQuery, customers) {
        if (customerSearchQuery.isBlank()) {
            customers.take(5) // Recently used / active customers
        } else {
            val q = customerSearchQuery.trim().lowercase()
            customers.filter {
                it.name.lowercase().contains(q) ||
                (it.trade_name?.lowercase()?.contains(q) == true) ||
                it.phone.contains(q) ||
                (it.contact_person?.lowercase()?.contains(q) == true) ||
                (it.area?.lowercase()?.contains(q) == true) ||
                (it.city?.lowercase()?.contains(q) == true)
            }
        }
    }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // SECTION 1: CUSTOMER SELECTION (Dedicated Component)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = DeepTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select Customer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepTeal
                            )
                        }

                        // Add New Retail Shop Button
                        TextButton(
                            onClick = onNavigateToAddCustomer,
                            colors = ButtonDefaults.textButtonColors(contentColor = DeepTeal)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add New Retail Shop", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (selectedCustomer == null) {
                        // Search Input
                        OutlinedTextField(
                            value = customerSearchQuery,
                            onValueChange = { customerSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search shop name, mobile, contact person...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Text(
                            text = if (customerSearchQuery.isBlank()) "Recently Used Customers" else "Matching Customers (${filteredCustomers.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )

                        if (filteredCustomers.isEmpty()) {
                            Text(
                                text = "No matching customer found. Tap '+ Add New Retail Shop' to register one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredCustomers.forEach { c ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCustomer = c
                                                deliveryAddressOverride = c.delivery_address ?: c.address ?: ""
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = OffWhiteBackground
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = c.trade_name ?: c.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${c.contact_person ?: "No contact"} • ${c.phone}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                                if (!c.area.isNullOrBlank() || !c.city.isNullOrBlank()) {
                                                    Text(
                                                        text = listOfNotNull(c.area, c.city).joinToString(", "),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextMuted
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "₹ %.2f".format(c.current_balance),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (c.current_balance > 0) RedCritical else GreenPositive
                                                )
                                                Text(
                                                    text = "Select",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DeepTeal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // SELECTED CUSTOMER SUMMARY CARD
                        val c = selectedCustomer!!
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = TealContainer
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = c.trade_name ?: c.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepTeal
                                        )
                                        Text(
                                            text = "Phone: ${c.phone} ${if (!c.contact_person.isNullOrBlank()) "(${c.contact_person})" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "Outstanding Balance: ₹ %.2f".format(c.current_balance),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (c.current_balance > 0) RedCritical else GreenPositive
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { selectedCustomer = null },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Change", style = MaterialTheme.typography.labelSmall, color = DeepTeal)
                                    }
                                }

                                HorizontalDivider(color = DeepTeal.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                                OutlinedTextField(
                                    value = deliveryAddressOverride,
                                    onValueChange = { deliveryAddressOverride = it },
                                    label = { Text("Delivery Drop-off Location") },
                                    placeholder = { Text("Saved address or delivery override...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Text(
                                    text = "Modifications here apply to this delivery run only without changing the customer's permanent record.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 2: PRODUCT & QUANTITY
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Product & Dispatch Unit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)

                    // Product Dropdown
                    ExposedDropdownMenuBox(
                        expanded = productExpanded,
                        onExpandedChange = { productExpanded = !productExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.name ?: "Select Dairy Product",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Product *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = productExpanded,
                            onDismissRequest = { productExpanded = false }
                        ) {
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(prod.name, fontWeight = FontWeight.Bold)
                                            Text("Avail: ${prod.current_stock_base} ${prod.canonical_unit_code} • ₹${prod.default_sale_price}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = prod
                                        unitCode = prod.canonical_unit_code
                                        unitPriceStr = prod.default_sale_price.toString()
                                        productExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Available Units for this product only
                    Text("Delivery Unit:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
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

                    // Quantity and Price Row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Quantity ($unitCode) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = unitPriceStr,
                            onValueChange = { unitPriceStr = it },
                            label = { Text("Rate per $unitCode (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Delivery Run Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Calculation Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TealContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Estimated Bill Total: ₹ %.2f".format(subtotal),
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

            // Submit Button
            Button(
                onClick = {
                    if (selectedCustomer == null) {
                        errorMessage = "Please explicitly select a customer."
                        return@Button
                    }
                    if (selectedProduct == null) {
                        errorMessage = "Please select a product."
                        return@Button
                    }
                    if (qty <= 0) {
                        errorMessage = "Quantity must be greater than zero."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null
                    scope.launch {
                        val fullNotes = listOfNotNull(
                            notes.takeIf { it.isNotBlank() },
                            deliveryAddressOverride.takeIf { it.isNotBlank() }?.let { "Drop-off: $it" }
                        ).joinToString(" | ")

                        val req = DeliveryCreateRequest(
                            customer_id = selectedCustomer!!.id,
                            discount = 0.0,
                            notes = fullNotes,
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

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
