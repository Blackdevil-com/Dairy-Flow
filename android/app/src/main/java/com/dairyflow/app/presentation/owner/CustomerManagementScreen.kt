package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.EmptyState
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.CustomerCreateRequest
import com.dairyflow.app.data.remote.dto.CustomerDto
import com.dairyflow.app.data.repository.CustomerRepository
import kotlinx.coroutines.launch

@Composable
fun CustomerManagementScreen(
    customerRepo: CustomerRepository,
    onBack: () -> Unit
) {
    var customers by remember { mutableStateOf<List<CustomerDto>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var tradeName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var customerType by remember { mutableStateOf("RETAIL_SHOP") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        isLoading = true
        scope.launch {
            val res = customerRepo.refreshCustomers()
            isLoading = false
            res.onSuccess { customers = it }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Customers Directory",
                subtitle = "Retail shops & individual accounts",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DeepTeal,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
            }
        }
    ) { padding ->
        if (customers.isEmpty() && !isLoading) {
            EmptyState(
                icon = Icons.Default.PersonAdd,
                title = "No Customers Registered",
                description = "Register retail shops and individual buyers to create delivery runs and track credit balances.",
                actionText = "Add First Customer",
                onAction = { showAddDialog = true },
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
                items(customers.size) { idx ->
                    val c = customers[idx]
                    val hasBalance = c.current_balance > 0

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
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = c.trade_name ?: c.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${c.customer_type.replace('_', ' ')} | Phone: ${c.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                    if (!c.address.isNullOrBlank()) {
                                        Text(
                                            text = c.address,
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
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = OutlineVariant)
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Credit Limit: ₹ %.2f".format(c.credit_limit),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // Add Customer Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Register Customer", fontWeight = FontWeight.Bold, color = DeepTeal) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = customerType == "RETAIL_SHOP",
                                onClick = { customerType = "RETAIL_SHOP" },
                                label = { Text("Retail Shop") }
                            )
                            FilterChip(
                                selected = customerType == "INDIVIDUAL",
                                onClick = { customerType = "INDIVIDUAL" },
                                label = { Text("Individual") }
                            )
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Customer / Shop Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Delivery Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val req = CustomerCreateRequest(
                                    name = name.trim(),
                                    trade_name = name.trim(),
                                    customer_type = customerType,
                                    phone = phone.trim(),
                                    email = null,
                                    address = address.trim()
                                )
                                customerRepo.createCustomer(req)
                                showAddDialog = false
                                load()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                        enabled = name.isNotBlank() && phone.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            )
        }
    }
}
