package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.components.MetricCard
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.DailyAnalyticsDto
import com.dairyflow.app.data.remote.dto.MonthlyAnalyticsDto
import com.dairyflow.app.data.remote.dto.YearlyAnalyticsDto
import com.dairyflow.app.data.repository.AnalyticsRepository
import kotlinx.coroutines.launch

@Composable
fun AnalyticsScreen(
    analyticsRepo: AnalyticsRepository,
    onBack: () -> Unit
) {
    var daily by remember { mutableStateOf<DailyAnalyticsDto?>(null) }
    var monthly by remember { mutableStateOf<MonthlyAnalyticsDto?>(null) }
    var yearly by remember { mutableStateOf<YearlyAnalyticsDto?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        scope.launch {
            val d = analyticsRepo.getDailySummary()
            d.onSuccess { daily = it }

            val m = analyticsRepo.getMonthlySummary()
            m.onSuccess { monthly = it }

            val y = analyticsRepo.getYearlySummary()
            y.onSuccess { yearly = it }

            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = "Business Analytics & Tax Report",
                subtitle = "Daily, Monthly & Financial Year (Apr-Mar)",
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
            // Daily Performance
            Text("Today's Dispatches & Sales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Daily Dispatched",
                    value = "%.1f L".format(daily?.total_stock_dispatched_litres ?: 0.0),
                    icon = Icons.Default.WaterDrop,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Daily Sales",
                    value = "₹ %.2f".format(daily?.total_sales_amount ?: 0.0),
                    icon = Icons.Default.CurrencyRupee,
                    modifier = Modifier.weight(1f)
                )
            }

            // Monthly Performance
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Current Month Summary (${monthly?.month_name ?: "November"} ${monthly?.year ?: 2026})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                    HorizontalDivider(color = OutlineVariant)
                    AnalyticsMetricRow("Total Dispatches / Deliveries", "${monthly?.total_deliveries ?: 0}")
                    AnalyticsMetricRow("Total Monthly Sales Turnover", "₹ %.2f".format(monthly?.total_sales_amount ?: 0.0))
                    AnalyticsMetricRow("Total Collections Received", "₹ %.2f".format(monthly?.total_collected_amount ?: 0.0))
                    AnalyticsMetricRow("Net Outstanding Created", "₹ %.2f".format(monthly?.total_outstanding_created ?: 0.0))
                }
            }

            // Financial Year (April - March) Performance
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Financial Year Report (${yearly?.financial_year ?: "2026-27"})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                    Text("Period: April 1 through March 31", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    HorizontalDivider(color = OutlineVariant)
                    AnalyticsMetricRow("Total Annual Deliveries", "${yearly?.total_deliveries ?: 0}")
                    AnalyticsMetricRow("Total FY Gross Turnover", "₹ %.2f".format(yearly?.total_turnover ?: 0.0))
                    AnalyticsMetricRow("Total Realized Collections", "₹ %.2f".format(yearly?.total_collected ?: 0.0))
                    AnalyticsMetricRow("Current Total Accounts Receivable", "₹ %.2f".format(yearly?.total_outstanding ?: 0.0), isWarning = true)
                }
            }

            // GST Watermarked Disclaimer & Demo Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AmberContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "GST Registration Status: UNVERIFIED (DEMO)",
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "All tax calculations in DairyFlow are provisional and explicitly watermarked DEMO. No official production GST tax invoices will be issued until business GSTIN registration, tax scheme, HSN and product classifications have been formally verified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsMetricRow(label: String, value: String, isWarning: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) RedCritical else TextPrimary
        )
    }
}
