package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.MetricCard
import com.dairyflow.app.core.components.StatusBadge
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.CustomerDto
import com.dairyflow.app.data.repository.CustomerRepository
import kotlinx.coroutines.launch

@Composable
fun CustomerDetailsScreen(
    customerId: String,
    customerRepo: CustomerRepository,
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToCreateDelivery: (String) -> Unit,
    onNavigateToRecordPayment: (String) -> Unit
) {
    var customer by remember { mutableStateOf<CustomerDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeactivateDialog by remember { mutableStateOf(false) }
    var isDeactivating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        isLoading = true
        scope.launch {
            val res = customerRepo.getCustomerById(customerId)
            isLoading = false
            res.onSuccess { customer = it }
        }
    }

    LaunchedEffect(customerId) {
        load()
    }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = customer?.trade_name ?: customer?.name ?: "Customer Details",
                subtitle = "Account profile & financial summary",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onNavigateToEdit(customerId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepTeal)
            }
        } else if (customer == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Customer record not found", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
            }
        } else {
            val c = customer!!
            val hasBalance = c.current_balance > 0

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OffWhiteBackground)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = c.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (!c.trade_name.isNullOrBlank() && c.trade_name != c.name) {
                                    Text(
                                        text = "Trade: ${c.trade_name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                if (!c.contact_person.isNullOrBlank()) {
                                    Text(
                                        text = "Contact: ${c.contact_person}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            StatusBadge(
                                status = if (c.is_active) "ACTIVE" else "INACTIVE",
                                isPositive = c.is_active
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = false,
                                onClick = {},
                                label = { Text(c.customer_type.replace('_', ' ')) }
                            )
                            if (c.credit_enabled) {
                                FilterChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text("Credit: ₹ %.0f".format(c.credit_limit)) }
                                )
                            }
                        }
                    }
                }

                // Outstanding Balance Hero Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (hasBalance) RedContainer else TealContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Current Outstanding",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (hasBalance) RedCritical else DeepTeal
                            )
                            Text(
                                text = "₹ %.2f".format(c.current_balance),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (hasBalance) RedCritical else DeepTeal
                            )
                            Text(
                                text = if (hasBalance) "Payment pending on confirmed deliveries" else "Account fully cleared",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        if (hasBalance) {
                            Button(
                                onClick = { onNavigateToRecordPayment(c.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = RedCritical),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Receive Pay", color = Color.White)
                            }
                        }
                    }
                }

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigateToCreateDelivery(c.id) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Delivery")
                    }

                    OutlinedButton(
                        onClick = { onNavigateToEdit(c.id) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = DeepTeal)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile", color = DeepTeal)
                    }
                }

                // Contact Details Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Contact Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        HorizontalDivider(color = OutlineVariant)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Primary Phone: ${c.phone}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }

                        if (!c.alternate_phone.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Alternate: ${c.alternate_phone}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }

                        if (!c.email.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Email: ${c.email}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }

                // Address Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Addresses & Delivery Route", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        HorizontalDivider(color = OutlineVariant)

                        Text("Delivery / Dispatch Address:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Text(
                            text = c.delivery_address ?: c.address ?: "No delivery address specified",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Billing / Registered Address:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Text(
                            text = c.billing_address ?: c.address ?: "Same as delivery",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        if (!c.area.isNullOrBlank() || !c.city.isNullOrBlank()) {
                            Text(
                                text = "Locality: ${listOfNotNull(c.area, c.city, c.district, c.postal_code).joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                // GST & Tax Configuration
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("GST & Tax Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        HorizontalDivider(color = OutlineVariant)

                        Text("Registration Scheme: ${c.gst_registration_status}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(
                            text = "GSTIN: ${c.gst_number ?: "Not registered / Unspecified"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (c.gst_number != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (c.gst_number != null) DeepTeal else TextMuted
                        )
                        Text("State Code: ${c.state_code ?: "Unspecified"}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }

                // Additional Notes
                if (!c.notes.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Delivery Notes & Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                            Text(text = c.notes, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }

                // Deactivation Action
                if (c.is_active) {
                    OutlinedButton(
                        onClick = { showDeactivateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCritical),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = RedCritical)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deactivate Customer Account", color = RedCritical)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showDeactivateDialog) {
            AlertDialog(
                onDismissRequest = { showDeactivateDialog = false },
                title = { Text("Deactivate Customer?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Deactivating '${customer?.name}' will hide it from new deliveries and customer lists, but preserve historical invoices and ledger records safely.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeactivating = true
                            scope.launch {
                                customerRepo.deactivateCustomer(customerId)
                                isDeactivating = false
                                showDeactivateDialog = false
                                load()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedCritical)
                    ) {
                        Text("Deactivate")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeactivateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
