package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.dairyflow.app.core.components.DairyBottomNavigation
import com.dairyflow.app.core.components.DairySearchBar
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
        bottomBar = {
            DairyBottomNavigation(
                selectedTab = "CUSTOMERS",
                onTabSelected = { tab ->
                    when (tab) {
                        "HOME" -> onBack()
                        "DELIVERIES" -> onNavigateToCreateDelivery("")
                        "CUSTOMERS" -> { /* Already here */ }
                        "MORE" -> onBack()
                    }
                },
                isOwner = true
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddCustomer,
                containerColor = DairyPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Customer", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DairyBackground)
                .padding(padding)
        ) {
            // Search & Filters Header Surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DairySurface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DairySearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search shop, customer name, mobile, area..."
                    )

                    // Filter Chips
                    val filters = listOf(
                        "ALL" to "All (${customers.size})",
                        "RETAIL_SHOP" to "Retail Shops",
                        "INDIVIDUAL" to "Individuals",
                        "ACTIVE" to "Active Only",
                        "INACTIVE" to "Inactive"
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filters) { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DairyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = DairySurfaceVariant,
                                    labelColor = DairyTextSecondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (selectedFilter == key) DairyPrimary else DairyBorder,
                                    enabled = true,
                                    selected = selectedFilter == key
                                )
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DairyPrimary)
                }
            } else if (customers.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Storefront,
                    title = "No Customers Found",
                    description = if (searchQuery.isNotBlank()) "No records matching '$searchQuery'." else "Get started by registering your first retail shop or customer.",
                    actionText = "+ Register Customer",
                    onAction = onNavigateToAddCustomer,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(customers) { c ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onNavigateToCustomerDetails(c.id) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DairySurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DairyBorder)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(DairyPrimaryLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Store, contentDescription = null, tint = DairyPrimary, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = c.trade_name ?: c.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = DairyTextPrimary
                                            )
                                            if (c.trade_name != null && c.name != c.trade_name) {
                                                Text(
                                                    text = "Prop: ${c.name}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = DairyTextSecondary
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹ %.2f".format(c.current_balance),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (c.current_balance > 0) DairyCritical else DairySuccess
                                        )
                                        Text(
                                            text = if (c.current_balance > 0) "Receivable" else "Cleared",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DairyTextMuted
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = DairyTextMuted, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${c.contact_person ?: "Contact"} • ${c.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DairyTextSecondary
                                        )
                                    }

                                    if (!c.area.isNullOrBlank() || !c.city.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = DairyTextMuted, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = listOfNotNull(c.area, c.city).joinToString(", "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DairyTextSecondary
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = DairyBorder)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusBadge(status = c.customer_type)
                                        if (!c.is_active) {
                                            StatusBadge(status = "INACTIVE")
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onNavigateToCreateDelivery(c.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DairyPrimary),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DairyPrimary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("New Delivery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(onClick = { onNavigateToEditCustomer(c.id) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = DairyTextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
