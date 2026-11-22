package com.dairyflow.app.presentation.owner

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.StatusBadge
import com.dairyflow.app.core.network.ApiClient
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.DeliveryConfirmRequest
import com.dairyflow.app.data.remote.dto.DeliveryDto
import com.dairyflow.app.data.repository.DeliveryRepository
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun DeliveryDetailsScreen(
    deliveryId: String,
    deliveryRepo: DeliveryRepository,
    onBack: () -> Unit
) {
    var delivery by remember { mutableStateOf<DeliveryDto?>(null) }
    var isConfirming by remember { mutableStateOf(false) }
    var initialPaymentStr by remember { mutableStateOf("0.00") }
    var paymentMode by remember { mutableStateOf("CASH") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun load() {
        scope.launch {
            val list = deliveryRepo.getDeliveries()
            list.onSuccess { deliveries ->
                delivery = deliveries.find { it.id == deliveryId }
            }
        }
    }

    LaunchedEffect(deliveryId) { load() }

    val isDraft = delivery?.status == "DRAFT"

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Delivery #${delivery?.delivery_no ?: ""}",
                subtitle = "Status: ${delivery?.status ?: ""}",
                onBack = onBack
            )
        }
    ) { padding ->
        if (delivery == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepTeal)
            }
        } else {
            val deliv = delivery!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OffWhiteBackground)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deliv.customer_name ?: "Customer",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text("Delivery Date: ${deliv.delivery_date}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            StatusBadge(status = deliv.status)
                        }

                        if (deliv.invoice_no != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = OutlineVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Generated Invoice:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                Text(deliv.invoice_no!!, fontWeight = FontWeight.Bold, color = DeepTeal)
                            }
                        }
                    }
                }

                // Line Items
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Dispatched Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)

                        deliv.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(item.product_name ?: "Product", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = "%.2f %s (%.2f Base Units) @ ₹%.2f".format(
                                            item.requested_qty, item.unit_code, item.base_qty, item.unit_price
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                Text("₹ %.2f".format(item.line_total), fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        HorizontalDivider(color = OutlineVariant)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Payable:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("₹ %.2f".format(deliv.net_amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DeepTeal)
                        }
                    }
                }

                // If Draft: Initial Payment & Confirmation controls
                if (isDraft) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Delivery Confirmation & Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                            Text(
                                text = "Confirming will atomically lock inventory records, allocate batches via FEFO, deduct signed stock (-%.2f LTR), snapshot the invoice, and update customer credit balance.".format(deliv.total_base_qty),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            OutlinedTextField(
                                value = initialPaymentStr,
                                onValueChange = { initialPaymentStr = it },
                                label = { Text("Initial Payment Amount (₹) [0 for Credit]") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("CASH", "UPI", "BANK_TRANSFER").forEach { mode ->
                                    FilterChip(
                                        selected = paymentMode == mode,
                                        onClick = { paymentMode = mode },
                                        label = { Text(mode) }
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
                            isConfirming = true
                            errorMessage = null
                            scope.launch {
                                val req = DeliveryConfirmRequest(
                                    idempotency_key = UUID.randomUUID().toString(),
                                    initial_payment_amount = initialPaymentStr.toDoubleOrNull(),
                                    payment_mode = paymentMode
                                )
                                val res = deliveryRepo.confirmDelivery(deliveryId, req)
                                isConfirming = false
                                res.onSuccess {
                                    load()
                                    successMessage = "Delivery Confirmed! FEFO stock deducted & invoice generated."
                                }.onFailure {
                                    errorMessage = it.message ?: "Confirmation failed"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPositive),
                        enabled = !isConfirming
                    ) {
                        if (isConfirming) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Delivery & Deduct Stock", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    // Confirmed State Actions: PDF Sharing
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = GreenContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "✓ Delivery Confirmed & Settled",
                                fontWeight = FontWeight.Bold,
                                color = GreenPositive,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Inventory batch balance updated. Commercial invoice # ${deliv.invoice_no ?: ""} created with DEMO watermark.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GreenPositive.copy(alpha = 0.85f)
                            )
                        }
                    }

                    deliv.invoice_id?.let { invId ->
                        Button(
                            onClick = {
                                val pdfUrl = "${ApiClient.baseUrl}invoices/$invId/pdf"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View & Download PDF Invoice (Demo Watermark)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
