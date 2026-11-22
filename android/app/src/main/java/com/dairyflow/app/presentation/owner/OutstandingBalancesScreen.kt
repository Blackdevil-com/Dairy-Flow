package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
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
import com.dairyflow.app.data.remote.dto.OutstandingCustomerDto
import com.dairyflow.app.data.repository.BillingPaymentRepository
import kotlinx.coroutines.launch

@Composable
fun OutstandingBalancesScreen(
    billingRepo: BillingPaymentRepository,
    onBack: () -> Unit,
    onNavigateToRecordPayment: () -> Unit
) {
    var outstandingList by remember { mutableStateOf<List<OutstandingCustomerDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        isLoading = true
        scope.launch {
            val res = billingRepo.getOutstandingCustomers()
            isLoading = false
            res.onSuccess { outstandingList = it }
        }
    }

    LaunchedEffect(Unit) { load() }

    val totalOutstanding = outstandingList.sumOf { it.outstanding_balance }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Outstanding Ledger",
                subtitle = "Total Receivables: ₹ %.2f".format(totalOutstanding),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && outstandingList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepTeal)
            }
        } else if (outstandingList.isEmpty()) {
            EmptyState(
                icon = Icons.Default.AccountBalanceWallet,
                title = "All Accounts Settled",
                description = "No outstanding balances or unpaid invoices across all customers.",
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
                items(outstandingList.size) { idx ->
                    val o = outstandingList[idx]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = o.customer_name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${o.customer_type} | Phone: ${o.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "${o.unpaid_invoices_count} Unpaid Delivery Invoice(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberWarning,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹ %.2f".format(o.outstanding_balance),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = RedCritical
                                    )
                                    Text(
                                        text = "Limit: ₹%.0f".format(o.credit_limit),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = OutlineVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = onNavigateToRecordPayment,
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear Balance", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
