package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dairyflow.app.core.components.DairySearchBar
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.StatusBadge
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.*
import com.dairyflow.app.data.repository.CustomerRepository
import com.dairyflow.app.data.repository.DeliveryRepository
import com.dairyflow.app.data.repository.InventoryRepository
import kotlinx.coroutines.launch
import java.util.UUID

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val product: ProductDto,
    val unitCode: String,
    val quantity: Double,
    val unitPrice: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDeliveryScreen(
    initialCustomerId: String? = null,
    navController: NavController? = null,
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
    
    // Multi-product line items cart
    val cartItems = remember { mutableStateListOf<CartItem>() }

    // Line item builder inputs
    var activeProduct by remember { mutableStateOf<ProductDto?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var selectedUnitCode by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("") }
    var unitPriceInput by remember { mutableStateOf("") }

    // Order level options
    var discountInput by remember { mutableStateOf("0") }
    var notesInput by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Function to reload customers
    fun reloadCustomers(selectIdAfter: String? = null) {
        scope.launch {
            val cRes = customerRepo.refreshCustomers()
            cRes.onSuccess { list ->
                customers = list
                val targetId = selectIdAfter ?: initialCustomerId
                if (targetId != null && selectedCustomer == null) {
                    val match = list.find { it.id == targetId }
                    if (match != null) {
                        selectedCustomer = match
                        deliveryAddressOverride = match.delivery_address ?: match.address ?: ""
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        reloadCustomers()

        val pRes = inventoryRepo.refreshProducts()
        pRes.onSuccess { prodList ->
            products = prodList
            if (prodList.isNotEmpty()) {
                val first = prodList.first()
                activeProduct = first
                selectedUnitCode = first.canonical_unit_code
                unitPriceInput = first.default_sale_price.toString()
            }
        }
    }

    // Observe savedStateHandle for newly registered customer
    navController?.currentBackStackEntry?.savedStateHandle?.let { handle ->
        val returnedCustomerId by handle.getStateFlow<String?>("new_customer_id", null).collectAsState()
        LaunchedEffect(returnedCustomerId) {
            returnedCustomerId?.let { newId ->
                reloadCustomers(selectIdAfter = newId)
                handle.remove<String>("new_customer_id")
            }
        }
    }

    val filteredCustomers = remember(customerSearchQuery, customers) {
        if (customerSearchQuery.isBlank()) {
            customers.take(5)
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

    val subtotal = cartItems.sumOf { it.lineTotal }
    val discount = discountInput.toDoubleOrNull() ?: 0.0
    val netTotal = (subtotal - discount).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Create Delivery Run",
                subtitle = "Multi-product dispatch order",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DairyBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. CUSTOMER SELECTION CARD
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DairySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DairyBorder)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DairyPrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = DairyPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Customer / Retail Shop",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DairyTextPrimary
                            )
                        }

                        TextButton(
                            onClick = onNavigateToAddCustomer,
                            colors = ButtonDefaults.textButtonColors(contentColor = DairyPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add New Shop", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (selectedCustomer == null) {
                        DairySearchBar(
                            query = customerSearchQuery,
                            onQueryChange = { customerSearchQuery = it },
                            placeholder = "Search shop, phone, contact..."
                        )

                        Text(
                            text = if (customerSearchQuery.isBlank()) "Recent Customers" else "Matching Customers (${filteredCustomers.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = DairyTextMuted
                        )

                        if (filteredCustomers.isEmpty()) {
                            Text(
                                text = "No matching customer found. Tap '+ Add New Shop' above to register.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DairyTextMuted,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredCustomers.forEach { c ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                selectedCustomer = c
                                                deliveryAddressOverride = c.delivery_address ?: c.address ?: ""
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        color = DairySurfaceVariant,
                                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DairyBorder))
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
                                                    color = DairyTextPrimary
                                                )
                                                Text(
                                                    text = "${c.contact_person ?: "Contact"} • ${c.phone}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = DairyTextSecondary
                                                )
                                                if (!c.area.isNullOrBlank() || !c.city.isNullOrBlank()) {
                                                    Text(
                                                        text = listOfNotNull(c.area, c.city).joinToString(", "),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = DairyTextMuted
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "₹ %.2f".format(c.current_balance),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (c.current_balance > 0) DairyCritical else DairySuccess
                                                )
                                                Text(
                                                    text = "Select",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DairyPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // SELECTED CUSTOMER BADGE CARD
                        val c = selectedCustomer!!
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = DairyPrimaryLight
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = c.trade_name ?: c.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DairyPrimaryDark
                                        )
                                        Text(
                                            text = "Contact: ${c.contact_person ?: "N/A"} • ${c.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DairyTextSecondary
                                        )
                                    }
                                    TextButton(
                                        onClick = { selectedCustomer = null },
                                        colors = ButtonDefaults.textButtonColors(contentColor = DairyPrimary)
                                    ) {
                                        Text("Change", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusBadge(status = c.customer_type)
                                    StatusBadge(
                                        status = "Balance: ₹ %.2f".format(c.current_balance),
                                        isPositive = c.current_balance <= 0
                                    )
                                }
                            }
                        }

                        // Delivery Address Snapshot Field
                        OutlinedTextField(
                            value = deliveryAddressOverride,
                            onValueChange = { deliveryAddressOverride = it },
                            label = { Text("Delivery Address (Destination Snapshot)") },
                            placeholder = { Text("Shop drop-off address...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DairyPrimary,
                                unfocusedBorderColor = DairyBorder
                            ),
                            minLines = 2
                        )
                    }
                }
            }

            // ==========================================
            // 2. PRODUCT LINE ITEMS (MULTI-PRODUCT CART)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DairySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DairyBorder)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DairyPrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = DairyPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Order Items (${cartItems.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DairyTextPrimary
                            )
                        }
                    }

                    // Existing Cart Items List
                    if (cartItems.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = DairySurfaceVariant
                        ) {
                            Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No products added yet. Select a product below and tap 'Add to Delivery'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DairyTextMuted
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            cartItems.forEachIndexed { index, item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = DairySurfaceVariant,
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DairyBorder))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.product.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = DairyTextPrimary
                                            )
                                            Text(
                                                text = "${item.quantity} ${item.unitCode} × ₹ %.2f".format(item.unitPrice),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DairyTextSecondary
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "₹ %.2f".format(item.lineTotal),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = DairyPrimary
                                            )
                                            IconButton(onClick = { cartItems.removeAt(index) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = DairyCritical)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DairyBorder)

                    // Line Item Builder Section
                    Text(
                        text = "Add Item to Cart",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DairyTextPrimary
                    )

                    // Product Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = productDropdownExpanded,
                        onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = activeProduct?.let { "${it.name} (${it.sku})" } ?: "Select Product",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Product") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DairyPrimary,
                                unfocusedBorderColor = DairyBorder
                            )
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
                                            Text(
                                                "SKU: ${prod.sku} • Avail: %.1f %s".format(prod.current_stock_base, prod.canonical_unit_code),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DairyTextMuted
                                            )
                                        }
                                    },
                                    onClick = {
                                        activeProduct = prod
                                        selectedUnitCode = prod.canonical_unit_code
                                        unitPriceInput = prod.default_sale_price.toString()
                                        productDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Unit Selection Chips
                    if (activeProduct != null) {
                        val availableUnits = remember(activeProduct) {
                            val units = mutableListOf(activeProduct!!.canonical_unit_code)
                            activeProduct!!.units.forEach { u ->
                                if (!units.contains(u.unit_code)) units.add(u.unit_code)
                            }
                            units
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Unit:", style = MaterialTheme.typography.bodySmall, color = DairyTextSecondary)
                            availableUnits.forEach { u ->
                                FilterChip(
                                    selected = selectedUnitCode == u,
                                    onClick = { selectedUnitCode = u },
                                    label = { Text(u, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DairyPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Quantity and Unit Price Inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            label = { Text("Quantity") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DairyPrimary,
                                unfocusedBorderColor = DairyBorder
                            )
                        )

                        OutlinedTextField(
                            value = unitPriceInput,
                            onValueChange = { unitPriceInput = it },
                            label = { Text("Price (₹)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DairyPrimary,
                                unfocusedBorderColor = DairyBorder
                            )
                        )
                    }

                    // Add Item Button
                    val itemQty = quantityInput.toDoubleOrNull() ?: 0.0
                    val itemPrice = unitPriceInput.toDoubleOrNull() ?: 0.0

                    OutlinedButton(
                        onClick = {
                            if (activeProduct != null && itemQty > 0) {
                                cartItems.add(
                                    CartItem(
                                        product = activeProduct!!,
                                        unitCode = selectedUnitCode,
                                        quantity = itemQty,
                                        unitPrice = itemPrice
                                    )
                                )
                                quantityInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DairyPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, DairyPrimary),
                        enabled = activeProduct != null && itemQty > 0
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Delivery (₹ %.2f)".format(itemQty * itemPrice), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ==========================================
            // 3. ORDER SUMMARY & NOTES
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DairySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DairyBorder)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Order Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DairyTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = DairyTextSecondary)
                        Text("₹ %.2f".format(subtotal), fontWeight = FontWeight.Bold, color = DairyTextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discount (₹)", color = DairyTextSecondary)
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { discountInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(110.dp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    HorizontalDivider(color = DairyBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DairyTextPrimary)
                        Text("₹ %.2f".format(netTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DairyPrimary)
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Delivery Notes") },
                        placeholder = { Text("Morning dispatch route instructions...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DairyPrimary,
                            unfocusedBorderColor = DairyBorder
                        )
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = DairyCriticalLight
                ) {
                    Text(
                        text = errorMessage!!,
                        color = DairyCritical,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (selectedCustomer == null) {
                        errorMessage = "Please select a customer for this delivery."
                        return@Button
                    }
                    if (cartItems.isEmpty()) {
                        errorMessage = "Please add at least one product item to the delivery."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null
                    scope.launch {
                        val req = DeliveryCreateRequest(
                            customer_id = selectedCustomer!!.id,
                            discount = discount,
                            delivery_address = deliveryAddressOverride.takeIf { it.isNotBlank() },
                            notes = notesInput.takeIf { it.isNotBlank() },
                            idempotency_key = UUID.randomUUID().toString(),
                            items = cartItems.map {
                                DeliveryItemRequest(
                                    product_id = it.product.id,
                                    unit_code = it.unitCode,
                                    requested_qty = it.quantity,
                                    unit_price = it.unitPrice
                                )
                            }
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
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DairyPrimary),
                enabled = !isSubmitting && selectedCustomer != null && cartItems.isNotEmpty()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Delivery Draft", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
