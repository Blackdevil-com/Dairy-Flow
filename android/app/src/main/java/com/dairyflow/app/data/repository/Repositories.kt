package com.dairyflow.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.dairyflow.app.core.network.ApiClient
import com.dairyflow.app.data.local.DairyDatabase
import com.dairyflow.app.data.local.entities.*
import com.dairyflow.app.data.remote.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dairy_auth", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString("token", null)
        private set(value) = prefs.edit().putString("token", value).apply()

    var role: String?
        get() = prefs.getString("role", null)
        private set(value) = prefs.edit().putString("role", value).apply()

    var username: String?
        get() = prefs.getString("username", null)
        private set(value) = prefs.edit().putString("username", value).apply()

    var fullName: String?
        get() = prefs.getString("full_name", null)
        private set(value) = prefs.edit().putString("full_name", value).apply()

    init {
        ApiClient.setToken(token)
    }

    suspend fun login(req: LoginRequest): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.apiService.login(req)
            token = response.access_token
            role = response.role
            username = response.username
            fullName = response.full_name
            ApiClient.setToken(response.access_token)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        ApiClient.setToken(null)
    }

    fun isLoggedIn(): Boolean = !token.isNullOrBlank()
}


class InventoryRepository(context: Context) {
    private val db = DairyDatabase.getDatabase(context)
    private val productDao = db.productDao()
    private val batchDao = db.batchDao()

    val cachedProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    suspend fun refreshProducts(): Result<List<ProductDto>> = withContext(Dispatchers.IO) {
        try {
            val products = ApiClient.apiService.getProducts()
            val entities = products.map { p ->
                ProductEntity(
                    id = p.id,
                    sku = p.sku,
                    name = p.name,
                    canonicalUnit = p.canonical_unit_code,
                    defaultSalePrice = p.default_sale_price,
                    currentStockBase = p.current_stock_base,
                    minStockAlert = p.min_stock_alert,
                    isLowStock = p.current_stock_base <= p.min_stock_alert
                )
            }
            productDao.clearProducts()
            productDao.insertProducts(entities)
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInventorySummary(): Result<List<StockSummaryDto>> = withContext(Dispatchers.IO) {
        try {
            val summary = ApiClient.apiService.getInventorySummary()
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBatches(productId: String? = null): Result<List<StockBatchDto>> = withContext(Dispatchers.IO) {
        try {
            val batches = ApiClient.apiService.getStockBatches(productId)
            val entities = batches.map { b ->
                BatchEntity(
                    id = b.id,
                    productId = b.product_id,
                    batchNo = b.batch_no,
                    availableBaseQty = b.available_base_qty,
                    expiryDate = b.expiry_date,
                    status = b.status
                )
            }
            batchDao.insertBatches(entities)
            Result.success(batches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLedger(productId: String? = null): Result<List<StockMovementDto>> = withContext(Dispatchers.IO) {
        try {
            val movements = ApiClient.apiService.getStockLedger(productId)
            Result.success(movements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProduct(req: ProductCreateRequest): Result<ProductDto> = withContext(Dispatchers.IO) {
        try {
            val prod = ApiClient.apiService.createProduct(req)
            refreshProducts()
            Result.success(prod)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adjustStock(req: StockAdjustmentRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ApiClient.apiService.adjustStock(req)
            refreshProducts()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordDamagedExpired(req: DamagedExpiredRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ApiClient.apiService.recordDamagedExpired(req)
            refreshProducts()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun receivePurchase(req: PurchaseReceiptRequest): Result<PurchaseReceiptDto> = withContext(Dispatchers.IO) {
        try {
            val res = ApiClient.apiService.createPurchaseReceipt(req)
            refreshProducts()
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


class CustomerRepository(context: Context) {
    private val db = DairyDatabase.getDatabase(context)
    private val customerDao = db.customerDao()

    val cachedCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    suspend fun refreshCustomers(query: String? = null): Result<List<CustomerDto>> = withContext(Dispatchers.IO) {
        try {
            val customers = ApiClient.apiService.getCustomers(query)
            val entities = customers.map { c ->
                CustomerEntity(
                    id = c.id,
                    name = c.name,
                    tradeName = c.trade_name,
                    customerType = c.customer_type,
                    phone = c.phone,
                    currentBalance = c.current_balance,
                    creditLimit = c.credit_limit
                )
            }
            customerDao.clearCustomers()
            customerDao.insertCustomers(entities)
            Result.success(customers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCustomer(req: CustomerCreateRequest): Result<CustomerDto> = withContext(Dispatchers.IO) {
        try {
            val cust = ApiClient.apiService.createCustomer(req)
            refreshCustomers()
            Result.success(cust)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


class DeliveryRepository(context: Context) {
    suspend fun getDeliveries(status: String? = null): Result<List<DeliveryDto>> = withContext(Dispatchers.IO) {
        try {
            val deliveries = ApiClient.apiService.getDeliveries(status)
            Result.success(deliveries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDraft(req: DeliveryCreateRequest): Result<DeliveryDto> = withContext(Dispatchers.IO) {
        try {
            val res = ApiClient.apiService.createDeliveryDraft(req)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmDelivery(id: String, req: DeliveryConfirmRequest): Result<DeliveryDto> = withContext(Dispatchers.IO) {
        try {
            val res = ApiClient.apiService.confirmDelivery(id, req)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


class BillingPaymentRepository(context: Context) {
    suspend fun getInvoices(): Result<List<InvoiceDto>> = withContext(Dispatchers.IO) {
        try {
            val invoices = ApiClient.apiService.getInvoices()
            Result.success(invoices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPayments(): Result<List<PaymentDto>> = withContext(Dispatchers.IO) {
        try {
            val payments = ApiClient.apiService.getPayments()
            Result.success(payments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordPayment(req: PaymentCreateRequest): Result<PaymentDto> = withContext(Dispatchers.IO) {
        try {
            val res = ApiClient.apiService.recordPayment(req)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOutstandingCustomers(): Result<List<OutstandingCustomerDto>> = withContext(Dispatchers.IO) {
        try {
            val list = ApiClient.apiService.getOutstandingCustomers()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


class AnalyticsRepository(context: Context) {
    suspend fun getDailySummary(): Result<DailyAnalyticsDto> = withContext(Dispatchers.IO) {
        try {
            val data = ApiClient.apiService.getDailySummary()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMonthlySummary(): Result<MonthlyAnalyticsDto> = withContext(Dispatchers.IO) {
        try {
            val data = ApiClient.apiService.getMonthlySummary()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getYearlySummary(): Result<YearlyAnalyticsDto> = withContext(Dispatchers.IO) {
        try {
            val data = ApiClient.apiService.getYearlySummary()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
