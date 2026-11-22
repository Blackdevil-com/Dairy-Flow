package com.dairyflow.app.presentation.owner

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.EmptyState
import com.dairyflow.app.core.components.StatusBadge
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.CustomerDto
import com.dairyflow.app.data.repository.CustomerRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManagementScreen(
    customerRepo: CustomerRepository,
    onBack: () -> Unit,
    onNavigateToAddCustomer: () -> Unit,
    onNavigateToEditCustomer: (String) -> Unit,
    onNavigateToCustomerDetails: (String) -> Unit,
    onNavigateToCreateDelivery: (String) -> Unit
) {
    var customers by remember { mutableStateOf<List<CustomerDto>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, RETAIL_SHOP, INDIVIDUAL, OTHER_BUSINESS, ACTIVE, INACTIVE
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        isLoading = true
        errorMessage = null
        scope.launch {
            val typeFilter = when (selectedFilter) {
                "RETAIL_SHOP" -> "RETAIL_SHOP"
                "INDIVIDUAL" -> "INDIVIDUAL"
                "OTHER_BUSINESS" -> "OTHER_BUSINESS"
                else -> null
            }
            val activeFilter = when (selectedFilter) {
                "INACTIVE" -> false
                "ALL" -> null
                else -> true
            }
            val res = customerRepo.refreshCustomers(
                query = searchQuery.takeIf { it.isNotBlank() },
                customerType = typeFilter,
                isActive = activeFilter
            )
            isLoading = false
            res.onSuccess {
                customers = it
            }.onFailure {
                errorMessage = it.message ?: "Failed to load customers"
            }
        }
    }

    LaunchedEffect(selectedFilter, searchQuery) {
        load()
    }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Customers Directory",
                subtitle = "Manage retail shops & customer accounts",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToAddCustomer) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddCustomer,
                containerColor = DeepTeal,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Customer", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhiteBackground)
                .padding(padding)
        ) {
            // Search & Count Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Accounts (${customers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        TextButton(onClick = { load() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = DeepTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", color = DeepTeal)
                        }
                    }

                    // Search TextField
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search shop, name, phone, or location...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Filter Chips Scrollable Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "RETAIL_SHOP" to "Retail Shops",
                            "INDIVIDUAL" to "Individuals",
                            "ACTIVE" to "Active",
                            "INACTIVE" to "Inactive"
                        ).forEach { (filterKey, label) ->
                            FilterChip(
                                selected = selectedFilter == filterKey,
                                onClick = { selectedFilter = filterKey },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Customer List / Empty State / Loading State
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DeepTeal)
                }
            } else if (customers.isEmpty()) {
                if (searchQuery.isNotBlank()) {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No Customers Found",
                        description = "No customer accounts match '$searchQuery'. Try adjusting your search query or filters.",
                        actionText = "Clear Search",
                        onAction = { searchQuery = "" },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    EmptyState(
                        icon = Icons.Default.Storefront,
                        title = "No Customers Registered",
                        description = "Register retail shops and individual buyers once. They will appear here and can be selected for future delivery runs.",
                        actionText = "Register Retail Shop",
                        onAction = onNavigateToAddCustomer,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(customers.size) { idx ->
                        val c = customers[idx]
                        val hasBalance = c.current_balance > 0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToCustomerDetails(c.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                                            color = TextPrimary
                                        )
                                        if (!c.trade_name.isNullOrBlank() && c.trade_name != c.name) {
                                            Text(
                                                text = c.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }
                                        if (!c.contact_person.isNullOrBlank()) {
                                            Text(
                                                text = "Contact: ${c.contact_person}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Text(
                                            text = "${c.customer_type.replace('_', ' ')} • Ph: ${c.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                        if (!c.area.isNullOrBlank() || !c.city.isNullOrBlank()) {
                                            Text(
                                                text = "Locality: ${listOfNotNull(c.area, c.city).joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Outstanding:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "₹ %.2f".format(c.current_balance),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasBalance) RedCritical else GreenPositive
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        StatusBadge(
                                            status = if (c.credit_enabled) "CREDIT ON" else "CASH ONLY",
                                            isPositive = c.credit_enabled
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = OutlineVariant)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { onNavigateToCreateDelivery(c.id) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Create Delivery", color = DeepTeal, fontWeight = FontWeight.SemiBold)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { onNavigateToEditCustomer(c.id) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { onNavigateToCustomerDetails(c.id) }) {
                                            Icon(Icons.Default.ChevronRight, contentDescription = "View Details", tint = TextMuted, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}
