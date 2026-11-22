package com.dairyflow.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dairyflow.app.core.components.DairyTopBar
import com.dairyflow.app.core.theme.*
import com.dairyflow.app.data.remote.dto.CustomerCreateRequest
import com.dairyflow.app.data.remote.dto.CustomerUpdateRequest
import com.dairyflow.app.data.repository.CustomerRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(
    customerId: String? = null,
    customerRepo: CustomerRepository,
    onBack: () -> Unit,
    onCustomerSaved: (String) -> Unit
) {
    val isEditing = customerId != null
    val scope = rememberCoroutineScope()

    // Form fields
    var name by remember { mutableStateOf("") }
    var tradeName by remember { mutableStateOf("") }
    var customerType by remember { mutableStateOf("RETAIL_SHOP") }
    var contactPerson by remember { mutableStateOf("") }

    var phone by remember { mutableStateOf("") }
    var alternatePhone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var building by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Pune") }
    var district by remember { mutableStateOf("Pune") }
    var stateCode by remember { mutableStateOf("27") }
    var postalCode by remember { mutableStateOf("") }
    var billingAddress by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var sameAsBilling by remember { mutableStateOf(true) }

    var gstStatus by remember { mutableStateOf("UNKNOWN") }
    var gstin by remember { mutableStateOf("") }

    var creditEnabled by remember { mutableStateOf(false) }
    var creditLimitStr by remember { mutableStateOf("0") }
    var paymentTermsDaysStr by remember { mutableStateOf("0") }
    var preferredPaymentMethod by remember { mutableStateOf("UPI") }

    var notes by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pre-populate if editing
    LaunchedEffect(customerId) {
        if (customerId != null) {
            isLoading = true
            val res = customerRepo.getCustomerById(customerId)
            isLoading = false
            res.onSuccess { c ->
                name = c.name
                tradeName = c.trade_name ?: ""
                customerType = c.customer_type
                contactPerson = c.contact_person ?: ""
                phone = c.phone
                alternatePhone = c.alternate_phone ?: ""
                email = c.email ?: ""
                area = c.area ?: ""
                city = c.city ?: "Pune"
                district = c.district ?: "Pune"
                stateCode = c.state_code ?: "27"
                postalCode = c.postal_code ?: ""
                billingAddress = c.billing_address ?: c.address ?: ""
                deliveryAddress = c.delivery_address ?: c.address ?: ""
                sameAsBilling = (billingAddress == deliveryAddress)
                gstStatus = c.gst_registration_status
                gstin = c.gst_number ?: ""
                creditEnabled = c.credit_enabled
                creditLimitStr = c.credit_limit.toInt().toString()
                paymentTermsDaysStr = c.payment_terms_days.toString()
                notes = c.notes ?: ""
                isActive = c.is_active
            }
        }
    }

    // Auto-sync delivery address when checkbox checked
    LaunchedEffect(sameAsBilling, billingAddress) {
        if (sameAsBilling) {
            deliveryAddress = billingAddress
        }
    }

    Scaffold(
        topBar = {
            DairyTopBar(
                title = if (isEditing) "Edit Customer" else "Register Retail Shop",
                subtitle = if (isEditing) "Update customer profile" else "Add new customer account",
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = SurfaceWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = RedCritical,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Customer / Shop name is required."
                                return@Button
                            }
                            if (phone.isBlank()) {
                                errorMessage = "Primary mobile number is required."
                                return@Button
                            }

                            isSubmitting = true
                            errorMessage = null

                            val fullBilling = if (billingAddress.isNotBlank()) {
                                billingAddress.trim()
                            } else {
                                listOfNotNull(
                                    building.takeIf { it.isNotBlank() },
                                    street.takeIf { it.isNotBlank() },
                                    area.takeIf { it.isNotBlank() },
                                    city.takeIf { it.isNotBlank() },
                                    postalCode.takeIf { it.isNotBlank() }
                                ).joinToString(", ")
                            }

                            val fullDelivery = if (sameAsBilling) fullBilling else deliveryAddress.trim()
                            val creditLimitVal = if (creditEnabled) (creditLimitStr.toDoubleOrNull() ?: 0.0) else 0.0
                            val paymentTermsVal = paymentTermsDaysStr.toIntOrNull() ?: 0

                            scope.launch {
                                if (isEditing && customerId != null) {
                                    val updateReq = CustomerUpdateRequest(
                                        name = name.trim(),
                                        trade_name = tradeName.trim().takeIf { it.isNotBlank() },
                                        customer_type = customerType,
                                        contact_person = contactPerson.trim().takeIf { it.isNotBlank() },
                                        phone = phone.trim(),
                                        alternate_phone = alternatePhone.trim().takeIf { it.isNotBlank() },
                                        email = email.trim().takeIf { it.isNotBlank() },
                                        address = fullDelivery.takeIf { it.isNotBlank() },
                                        billing_address = fullBilling.takeIf { it.isNotBlank() },
                                        delivery_address = fullDelivery.takeIf { it.isNotBlank() },
                                        area = area.trim().takeIf { it.isNotBlank() },
                                        city = city.trim().takeIf { it.isNotBlank() },
                                        district = district.trim().takeIf { it.isNotBlank() },
                                        state_code = stateCode.trim().takeIf { it.isNotBlank() },
                                        postal_code = postalCode.trim().takeIf { it.isNotBlank() },
                                        gst_registration_status = gstStatus,
                                        gst_number = gstin.trim().takeIf { it.isNotBlank() },
                                        credit_enabled = creditEnabled,
                                        credit_limit = creditLimitVal,
                                        payment_terms_days = paymentTermsVal,
                                        notes = notes.trim().takeIf { it.isNotBlank() },
                                        is_active = isActive
                                    )
                                    val res = customerRepo.updateCustomer(customerId, updateReq)
                                    isSubmitting = false
                                    res.onSuccess {
                                        onCustomerSaved(it.id)
                                    }.onFailure {
                                        errorMessage = it.message ?: "Failed to update customer"
                                    }
                                } else {
                                    val createReq = CustomerCreateRequest(
                                        name = name.trim(),
                                        trade_name = tradeName.trim().takeIf { it.isNotBlank() } ?: name.trim(),
                                        customer_type = customerType,
                                        contact_person = contactPerson.trim().takeIf { it.isNotBlank() },
                                        phone = phone.trim(),
                                        alternate_phone = alternatePhone.trim().takeIf { it.isNotBlank() },
                                        email = email.trim().takeIf { it.isNotBlank() },
                                        address = fullDelivery.takeIf { it.isNotBlank() },
                                        billing_address = fullBilling.takeIf { it.isNotBlank() },
                                        delivery_address = fullDelivery.takeIf { it.isNotBlank() },
                                        area = area.trim().takeIf { it.isNotBlank() },
                                        city = city.trim().takeIf { it.isNotBlank() },
                                        district = district.trim().takeIf { it.isNotBlank() },
                                        state_code = stateCode.trim().takeIf { it.isNotBlank() },
                                        postal_code = postalCode.trim().takeIf { it.isNotBlank() },
                                        gst_registration_status = gstStatus,
                                        gst_number = gstin.trim().takeIf { it.isNotBlank() },
                                        credit_enabled = creditEnabled,
                                        credit_limit = creditLimitVal,
                                        payment_terms_days = paymentTermsVal,
                                        notes = notes.trim().takeIf { it.isNotBlank() }
                                    )
                                    val res = customerRepo.createCustomer(createReq)
                                    isSubmitting = false
                                    res.onSuccess {
                                        onCustomerSaved(it.id)
                                    }.onFailure {
                                        errorMessage = it.message ?: "Failed to save customer"
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                        enabled = !isSubmitting && name.isNotBlank() && phone.isNotBlank()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEditing) "Update Customer" else "Save Customer",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepTeal)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OffWhiteBackground)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1 – SHOP DETAILS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = DeepTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Section 1: Shop & Account Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Shop / Customer Name *") },
                            placeholder = { Text("e.g., Sri Lakshmi Stores") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = tradeName,
                            onValueChange = { tradeName = it },
                            label = { Text("Trade Name / Display Name (Optional)") },
                            placeholder = { Text("e.g., Lakshmi Super Bazaar") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Text("Customer Type *", style = MaterialTheme.typography.labelMedium, color = TextMuted)
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
                            FilterChip(
                                selected = customerType == "OTHER_BUSINESS",
                                onClick = { customerType = "OTHER_BUSINESS" },
                                label = { Text("Other") }
                            )
                        }

                        OutlinedTextField(
                            value = contactPerson,
                            onValueChange = { contactPerson = it },
                            label = { Text("Contact Person") },
                            placeholder = { Text("e.g., Venkatesh Rao") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // SECTION 2 – CONTACT INFORMATION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = DeepTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Section 2: Contact Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        }

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Primary Mobile Number *") },
                            placeholder = { Text("+91 9845012345") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = alternatePhone,
                            onValueChange = { alternatePhone = it },
                            label = { Text("Alternate Phone (Optional)") },
                            placeholder = { Text("+91 9845012346") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address (Optional)") },
                            placeholder = { Text("billing@store.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // SECTION 3 – ADDRESS INFORMATION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = DeepTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Section 3: Address & Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = building,
                                onValueChange = { building = it },
                                label = { Text("Shop / Door No.") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = street,
                                onValueChange = { street = it },
                                label = { Text("Street / Road") },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = area,
                                onValueChange = { area = it },
                                label = { Text("Area / Locality") },
                                placeholder = { Text("Shivajinagar") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = postalCode,
                                onValueChange = { postalCode = it },
                                label = { Text("PIN Code") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = stateCode,
                                onValueChange = { stateCode = it },
                                label = { Text("State Code") },
                                placeholder = { Text("27") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = billingAddress,
                            onValueChange = { billingAddress = it },
                            label = { Text("Full Billing Address") },
                            placeholder = { Text("Detailed official invoice address...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = sameAsBilling,
                                onCheckedChange = { sameAsBilling = it },
                                colors = CheckboxDefaults.colors(checkedColor = DeepTeal)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Delivery address same as billing address",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }

                        if (!sameAsBilling) {
                            OutlinedTextField(
                                value = deliveryAddress,
                                onValueChange = { deliveryAddress = it },
                                label = { Text("Separate Delivery / Drop-off Address") },
                                placeholder = { Text("Depot / back entrance address...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // SECTION 4 – GST INFORMATION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = DeepTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Section 4: GST Registration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        }

                        Text("GST Scheme Status", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "UNKNOWN" to "Unknown",
                                "REGISTERED" to "Registered",
                                "UNREGISTERED" to "Unregistered",
                                "COMPOSITION" to "Composition"
                            ).forEach { (code, label) ->
                                FilterChip(
                                    selected = gstStatus == code,
                                    onClick = { gstStatus = code },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        if (gstStatus == "REGISTERED" || gstStatus == "COMPOSITION") {
                            OutlinedTextField(
                                value = gstin,
                                onValueChange = { gstin = it.uppercase() },
                                label = { Text("GSTIN (15 Alphanumeric) *") },
                                placeholder = { Text("27ABCDE1234F1Z5") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Text(
                                text = "GST number will be verified with business state code for applicable CGST/SGST.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                // SECTION 5 – PAYMENT & CREDIT SETTINGS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = DeepTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Section 5: Credit & Payment Terms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                            }
                            Switch(
                                checked = creditEnabled,
                                onCheckedChange = { creditEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = DeepTeal, checkedTrackColor = TealContainer)
                            )
                        }

                        Text(
                            text = if (creditEnabled) "Credit facility is enabled for this account." else "Credit is disabled. Customer must pay immediately on delivery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (creditEnabled) GreenPositive else TextMuted
                        )

                        if (creditEnabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = creditLimitStr,
                                    onValueChange = { creditLimitStr = it },
                                    label = { Text("Credit Limit (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = paymentTermsDaysStr,
                                    onValueChange = { paymentTermsDaysStr = it },
                                    label = { Text("Credit Days") },
                                    placeholder = { Text("e.g. 7") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        Text("Preferred Payment Mode", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("UPI", "CASH", "BANK_TRANSFER").forEach { mode ->
                                FilterChip(
                                    selected = preferredPaymentMethod == mode,
                                    onClick = { preferredPaymentMethod = mode },
                                    label = { Text(mode) }
                                )
                            }
                        }
                    }
                }

                // SECTION 6 – ADDITIONAL INFORMATION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DeepTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Section 6: Instructions & Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepTeal)
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Delivery Instructions / Route Notes") },
                            placeholder = { Text("e.g., Deliver before 6:30 AM; store keys with security...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (isEditing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Customer Active Status", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Switch(
                                    checked = isActive,
                                    onCheckedChange = { isActive = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GreenPositive, checkedTrackColor = TealContainer)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
