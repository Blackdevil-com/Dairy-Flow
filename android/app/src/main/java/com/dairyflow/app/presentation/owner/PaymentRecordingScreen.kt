package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.CustomerDto
import com.dairyflow.app.data.remote.dto.PaymentCreateRequest
import com.dairyflow.app.data.repository.BillingPaymentRepository
import com.dairyflow.app.data.repository.CustomerRepository
import kotlinx.coroutines.launch

@Composable
fun PaymentRecordingScreen(
    billingRepo: BillingPaymentRepository,
    customerRepo: CustomerRepository,
    onBack: () -> Unit
) {
    var customers by remember { mutableStateOf<List<CustomerDto>>(emptyList()) }
    var selectedCustomer by remember { mutableStateOf<CustomerDto?>(null) }
    var amountStr by remember { mutableStateOf("500.00") }
    var paymentMode by remember { mutableStateOf("UPI") } // CASH, UPI, BANK_TRANSFER, OTHER
    var transactionRef by remember { mutableStateOf("UPI-TXN-123456") }
    var notes by remember { mutableStateOf("Received partial clearance") }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val cRes = customerRepo.refreshCustomers()
        cRes.onSuccess {
            customers = it
            if (it.isNotEmpty()) selectedCustomer = it.first()
        }
    }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Record Customer Payment",
                subtitle = "Multi-channel FIFO allocation",
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
                            label = { Text("${c.trade_name ?: c.name} (₹ %.2f outstanding)".format(c.current_balance)) }
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
                    Text("Payment Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)

                    Text("Payment Channel:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CASH", "UPI", "BANK_TRANSFER", "OTHER").forEach { mode ->
                            FilterChip(
                                selected = paymentMode == mode,
                                onClick = { paymentMode = mode },
                                label = { Text(mode) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Payment Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = transactionRef,
                        onValueChange = { transactionRef = it },
                        label = { Text("Reference / UPI UTR / Cheque Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Payment Notes / Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            if (message != null) {
                Text(text = message!!, color = if (message!!.contains("Success")) GreenPositive else RedCritical, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (selectedCustomer == null) return@Button
                    isSubmitting = true
                    message = null
                    scope.launch {
                        val req = PaymentCreateRequest(
                            customer_id = selectedCustomer!!.id,
                            payment_mode = paymentMode,
                            amount = amountStr.toDoubleOrNull() ?: 0.0,
                            transaction_ref = transactionRef.trim(),
                            notes = notes.trim()
                        )
                        val res = billingRepo.recordPayment(req)
                        isSubmitting = false
                        res.onSuccess {
                            message = "Success! Payment recorded and automatically allocated to oldest open invoices."
                        }.onFailure {
                            message = it.message ?: "Failed to record payment"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPositive),
                enabled = !isSubmitting && selectedCustomer != null && (amountStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record & Allocate Payment", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
