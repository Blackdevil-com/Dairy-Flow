package com.dairyflow.app.core.network

import com.dairyflow.app.data.remote.dto.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @GET("auth/me")
    suspend fun getMe(): UserDto

    // Products & Units
    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @POST("products")
    suspend fun createProduct(@Body body: ProductCreateRequest): ProductDto

    @GET("products/categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("products/units")
    suspend fun getUnits(): List<UnitDto>

    // Inventory & Stock
    @GET("inventory/summary")
    suspend fun getInventorySummary(): List<StockSummaryDto>

    @GET("inventory/stock")
    suspend fun getStockBatches(@Query("product_id") productId: String? = null): List<StockBatchDto>

    @GET("inventory/ledger")
    suspend fun getStockLedger(@Query("product_id") productId: String? = null): List<StockMovementDto>

    @POST("inventory/adjustments")
    suspend fun adjustStock(@Body body: StockAdjustmentRequest): Map<String, Any>

    @POST("inventory/damaged-expired")
    suspend fun recordDamagedExpired(@Body body: DamagedExpiredRequest): Map<String, Any>

    // Purchases
    @GET("purchases")
    suspend fun getPurchases(): List<PurchaseReceiptDto>

    @POST("purchases")
    suspend fun createPurchaseReceipt(@Body body: PurchaseReceiptRequest): PurchaseReceiptDto

    // Customers & Suppliers
    @GET("customers")
    suspend fun getCustomers(
        @Query("query") query: String? = null,
        @Query("customer_type") customerType: String? = null,
        @Query("is_active") isActive: Boolean? = true
    ): List<CustomerDto>

    @GET("customers/{id}")
    suspend fun getCustomerById(@Path("id") id: String): CustomerDto

    @POST("customers")
    suspend fun createCustomer(@Body body: CustomerCreateRequest): CustomerDto

    @PUT("customers/{id}")
    suspend fun updateCustomer(
        @Path("id") id: String,
        @Body body: CustomerUpdateRequest
    ): CustomerDto

    @DELETE("customers/{id}")
    suspend fun deactivateCustomer(@Path("id") id: String): Map<String, Any>

    @GET("customers/suppliers/list")
    suspend fun getSuppliers(): List<SupplierDto>

    // Deliveries
    @GET("deliveries")
    suspend fun getDeliveries(@Query("status") status: String? = null): List<DeliveryDto>

    @POST("deliveries")
    suspend fun createDeliveryDraft(@Body body: DeliveryCreateRequest): DeliveryDto

    @POST("deliveries/{id}/confirm")
    suspend fun confirmDelivery(
        @Path("id") id: String,
        @Body body: DeliveryConfirmRequest
    ): DeliveryDto

    // Invoices & Billing
    @GET("invoices")
    suspend fun getInvoices(): List<InvoiceDto>

    @GET("invoices/{id}/pdf")
    @Streaming
    suspend fun downloadInvoicePdf(@Path("id") id: String): ResponseBody

    // Payments
    @GET("payments")
    suspend fun getPayments(): List<PaymentDto>

    @POST("payments")
    suspend fun recordPayment(@Body body: PaymentCreateRequest): PaymentDto

    @GET("payments/outstanding")
    suspend fun getOutstandingCustomers(): List<OutstandingCustomerDto>

    // Analytics
    @GET("analytics/summary")
    suspend fun getDailySummary(): DailyAnalyticsDto

    @GET("analytics/monthly")
    suspend fun getMonthlySummary(): MonthlyAnalyticsDto

    @GET("analytics/yearly")
    suspend fun getYearlySummary(): YearlyAnalyticsDto

    // Sync
    @GET("sync/changes")
    suspend fun pullSyncChanges(@Query("device_id") deviceId: String): Map<String, Any>

    @POST("sync/push")
    suspend fun pushSync(@Body body: SyncPushRequestDto): SyncPushResponseDto
}
