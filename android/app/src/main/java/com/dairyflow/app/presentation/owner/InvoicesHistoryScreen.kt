package com.dairyflow.app.presentation.owner

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.EmptyState
import com.dairyflow.app.core.components.StatusBadge
import com.dairyflow.app.core.network.ApiClient
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.InvoiceDto
import com.dairyflow.app.data.repository.BillingPaymentRepository
import kotlinx.coroutines.launch

@Composable
fun InvoicesHistoryScreen(
    billingRepo: BillingPaymentRepository,
    onBack: () -> Unit
) {
    var invoices by remember { mutableStateOf<List<InvoiceDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun load() {
        isLoading = true
        scope.launch {
            val res = billingRepo.getInvoices()
            isLoading = false
            res.onSuccess { invoices = it }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Invoices & Billing History",
                subtitle = "FY Sequence Series: DF/YYYY-YY/XXXX",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && invoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepTeal)
            }
        } else if (invoices.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Receipt,
                title = "No Invoices Issued",
                description = "Invoices are automatically generated and sequentially numbered when deliveries are confirmed.",
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
                items(invoices.size) { idx ->
                    val inv = invoices[idx]
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = inv.invoice_no,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeepTeal
                                    )
                                    Text(
                                        text = "Billed to: ${inv.customer_name_snap} | ${inv.invoice_date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                StatusBadge(status = inv.payment_status)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (inv.is_demo_watermark) {
                                Box(
                                    modifier = Modifier
                                        .background(AmberContainer, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "DEMO WATERMARKED - COMMERCIAL RECEIPT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberWarning,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            HorizontalDivider(color = OutlineVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Amount: ₹ %.2f".format(inv.total_amount), fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Paid: ₹ %.2f | Balance: ₹ %.2f".format(inv.paid_amount, inv.outstanding_amount), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }

                                IconButton(
                                    onClick = {
                                        val pdfUrl = "${ApiClient.baseUrl}invoices/${inv.id}/pdf"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "View PDF", tint = RedCritical)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
