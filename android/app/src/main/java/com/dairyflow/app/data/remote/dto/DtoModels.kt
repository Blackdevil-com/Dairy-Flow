package com.dairyflow.app.data.remote.dto

data class LoginRequest(val username: String, val password: String)

data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String,
    val role: String,
    val user_id: String,
    val username: String,
    val full_name: String
)

data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    val full_name: String,
    val role: String,
    val phone: String?,
    val is_active: Boolean
)

data class ProductUnitDto(
    val id: String?,
    val unit_code: String,
    val conversion_to_base: Double,
    val is_purchase_unit: Boolean = true,
    val is_delivery_unit: Boolean = true
)

data class TaxProfileDto(
    val id: String?,
    val hsn_code: String,
    val cgst_rate: Double,
    val sgst_rate: Double,
    val igst_rate: Double,
    val is_demo: Boolean = true
)

data class ProductDto(
    val id: String,
    val category_id: String,
    val category_name: String?,
    val sku: String,
    val name: String,
    val description: String?,
    val canonical_unit_code: String,
    val default_sale_price: Double,
    val default_cost_price: Double,
    val min_stock_alert: Double,
    val is_active: Boolean,
    val current_stock_base: Double = 0.0,
    val units: List<ProductUnitDto> = emptyList(),
    val tax_profiles: List<TaxProfileDto> = emptyList()
)

data class ProductCreateRequest(
    val category_id: String,
    val sku: String,
    val name: String,
    val description: String?,
    val canonical_unit_code: String,
    val default_sale_price: Double,
    val default_cost_price: Double,
    val min_stock_alert: Double,
    val hsn_code: String = "0401",
    val cgst_rate: Double = 0.0,
    val sgst_rate: Double = 0.0,
    val igst_rate: Double = 0.0,
    val units: List<ProductUnitDto> = emptyList()
)

data class CategoryDto(val id: String, val name: String, val description: String?)
data class UnitDto(val code: String, val name: String, val symbol: String, val is_base_standard: Boolean)

data class StockSummaryDto(
    val product_id: String,
    val product_sku: String,
    val product_name: String,
    val canonical_unit: String,
    val available_stock: Double,
    val min_stock_alert: Double,
    val is_low_stock: Boolean,
    val active_batches_count: Int,
    val nearest_expiry: String?
)

data class StockBatchDto(
    val id: String,
    val product_id: String,
    val product_name: String?,
    val warehouse_id: String,
    val batch_no: String,
    val mfg_date: String,
    val expiry_date: String,
    val initial_base_qty: Double,
    val available_base_qty: Double,
    val status: String,
    val days_to_expiry: Int
)

data class StockMovementDto(
    val id: String,
    val movement_no: String,
    val product_id: String,
    val product_name: String?,
    val batch_no: String?,
    val movement_type: String,
    val signed_base_quantity: Double,
    val reference_type: String,
    val reference_id: String,
    val remarks: String?,
    val created_at: String
)

data class StockAdjustmentRequest(
    val product_id: String,
    val batch_id: String?,
    val adjustment_type: String, // "IN" or "OUT"
    val quantity: Double,
    val unit_code: String,
    val reason: String
)

data class DamagedExpiredRequest(
    val product_id: String,
    val batch_id: String,
    val quantity: Double,
    val unit_code: String,
    val reason: String
)

data class PurchaseItemRequest(
    val product_id: String,
    val batch_no: String,
    val quantity_received: Double,
    val unit_code: String,
    val unit_cost: Double,
    val mfg_date: String,
    val expiry_date: String
)

data class PurchaseReceiptRequest(
    val supplier_id: String,
    val invoice_reference: String?,
    val notes: String?,
    val items: List<PurchaseItemRequest>
)

data class PurchaseReceiptDto(
    val id: String,
    val receipt_no: String,
    val supplier_name: String?,
    val total_amount: Double,
    val status: String,
    val receipt_date: String
)

data class SupplierDto(
    val id: String,
    val name: String,
    val contact_person: String?,
    val phone: String?,
    val email: String?,
    val address: String?
)

data class CustomerDto(
    val id: String,
    val name: String,
    val trade_name: String? = null,
    val customer_type: String = "RETAIL_SHOP",
    val contact_person: String? = null,
    val phone: String,
    val alternate_phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val billing_address: String? = null,
    val delivery_address: String? = null,
    val area: String? = null,
    val city: String? = null,
    val district: String? = null,
    val state_code: String? = null,
    val postal_code: String? = null,
    val gst_registration_status: String = "UNKNOWN",
    val gst_number: String? = null,
    val credit_enabled: Boolean = false,
    val credit_limit: Double = 0.0,
    val current_balance: Double = 0.0,
    val payment_terms_days: Int = 0,
    val notes: String? = null,
    val is_active: Boolean = true
)

data class CustomerCreateRequest(
    val name: String,
    val trade_name: String? = null,
    val customer_type: String = "RETAIL_SHOP",
    val contact_person: String? = null,
    val phone: String,
    val alternate_phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val billing_address: String? = null,
    val delivery_address: String? = null,
    val area: String? = null,
    val city: String? = null,
    val district: String? = null,
    val state_code: String? = null,
    val postal_code: String? = null,
    val gst_registration_status: String = "UNKNOWN",
    val gst_number: String? = null,
    val credit_enabled: Boolean = false,
    val credit_limit: Double = 0.0,
    val payment_terms_days: Int = 0,
    val notes: String? = null
)

data class CustomerUpdateRequest(
    val name: String? = null,
    val trade_name: String? = null,
    val customer_type: String? = null,
    val contact_person: String? = null,
    val phone: String? = null,
    val alternate_phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val billing_address: String? = null,
    val delivery_address: String? = null,
    val area: String? = null,
    val city: String? = null,
    val district: String? = null,
    val state_code: String? = null,
    val postal_code: String? = null,
    val gst_registration_status: String? = null,
    val gst_number: String? = null,
    val credit_enabled: Boolean? = null,
    val credit_limit: Double? = null,
    val payment_terms_days: Int? = null,
    val notes: String? = null,
    val is_active: Boolean? = null
)

data class DeliveryItemRequest(
    val product_id: String,
    val unit_code: String,
    val requested_qty: Double,
    val unit_price: Double? = null
)

data class DeliveryCreateRequest(
    val customer_id: String,
    val discount: Double = 0.0,
    val delivery_address: String? = null,
    val notes: String?,
    val idempotency_key: String?,
    val items: List<DeliveryItemRequest>
)

data class DeliveryItemDto(
    val id: String,
    val product_id: String,
    val product_name: String?,
    val unit_code: String,
    val requested_qty: Double,
    val conversion_factor: Double,
    val base_qty: Double,
    val unit_price: Double,
    val line_total: Double
)

data class DeliveryDto(
    val id: String,
    val delivery_no: String,
    val customer_id: String,
    val customer_name: String?,
    val status: String, // DRAFT, CONFIRMED, CANCELLED
    val delivery_date: String,
    val total_base_qty: Double,
    val subtotal: Double,
    val tax_amount: Double,
    val discount: Double,
    val net_amount: Double,
    val delivery_address: String? = null,
    val notes: String?,
    val invoice_id: String?,
    val invoice_no: String?,
    val items: List<DeliveryItemDto> = emptyList()
)

data class DeliveryConfirmRequest(
    val idempotency_key: String?,
    val initial_payment_amount: Double? = null,
    val payment_mode: String? = "CASH",
    val payment_ref: String? = null
)

data class InvoiceDto(
    val id: String,
    val invoice_no: String,
    val financial_year: String,
    val doc_type: String,
    val customer_name_snap: String,
    val customer_phone_snap: String?,
    val invoice_date: String,
    val subtotal: Double,
    val total_tax: Double,
    val total_amount: Double,
    val paid_amount: Double,
    val outstanding_amount: Double,
    val payment_status: String,
    val is_demo_watermark: Boolean,
    val pdf_path: String?
)

data class PaymentCreateRequest(
    val customer_id: String,
    val payment_mode: String = "CASH",
    val amount: Double,
    val transaction_ref: String?,
    val notes: String?,
    val specific_invoice_id: String? = null
)

data class PaymentDto(
    val id: String,
    val payment_no: String,
    val customer_id: String,
    val customer_name: String?,
    val payment_mode: String,
    val transaction_ref: String?,
    val amount: Double,
    val payment_date: String,
    val notes: String?
)

data class OutstandingCustomerDto(
    val customer_id: String,
    val customer_name: String,
    val customer_type: String,
    val phone: String,
    val credit_limit: Double,
    val outstanding_balance: Double,
    val unpaid_invoices_count: Int
)

data class DailyAnalyticsDto(
    val date: String,
    val deliveries_count: Int,
    val total_sales_amount: Double,
    val total_collections_amount: Double,
    val cash_collections: Double,
    val upi_collections: Double,
    val bank_collections: Double,
    val total_stock_dispatched_litres: Double
)

data class MonthlyAnalyticsDto(
    val year: Int,
    val month: Int,
    val month_name: String,
    val total_deliveries: Int,
    val total_sales_amount: Double,
    val total_collected_amount: Double,
    val total_outstanding_created: Double
)

data class YearlyAnalyticsDto(
    val financial_year: String,
    val total_deliveries: Int,
    val total_turnover: Double,
    val total_collected: Double,
    val total_outstanding: Double
)

data class SyncPushItemDto(
    val operation_id: String,
    val entity_type: String,
    val action: String,
    val payload: Map<String, Any?>,
    val client_timestamp: String
)

data class SyncPushRequestDto(
    val device_id: String,
    val items: List<SyncPushItemDto>
)

data class SyncPushResultDto(
    val operation_id: String,
    val status: String,
    val server_id: String? = null,
    val message: String? = null
)

data class SyncPushResponseDto(
    val processed_count: Int,
    val results: List<SyncPushResultDto>,
    val server_time: String
)
