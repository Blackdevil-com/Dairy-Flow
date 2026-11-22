package com.dairyflow.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dairyflow.app.core.network.ApiClient
import com.dairyflow.app.data.local.DairyDatabase
import com.dairyflow.app.data.local.entities.ProductEntity
import com.dairyflow.app.data.local.entities.BatchEntity
import com.dairyflow.app.data.local.entities.CustomerEntity
import com.dairyflow.app.data.remote.dto.SyncPushItemDto
import com.dairyflow.app.data.remote.dto.SyncPushRequestDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = DairyDatabase.getDatabase(applicationContext)
        val outboxDao = db.outboxDao()
        val customerDao = db.customerDao()
        val deviceId = "ANDROID-${android.os.Build.MODEL ?: "DEVICE"}"

        try {
            // 1. Process local pending outbox items via real FastAPI /sync/push
            val pendingItems = outboxDao.getPendingOutbox()
            if (pendingItems.isNotEmpty()) {
                val mapType = object : TypeToken<Map<String, Any?>>() {}.type
                val pushItems = pendingItems.map { item ->
                    val payload: Map<String, Any?> = try {
                        gson.fromJson(item.payloadJson, mapType)
                    } catch (e: Exception) {
                        emptyMap()
                    }
                    SyncPushItemDto(
                        operation_id = item.operationId,
                        entity_type = item.entityType,
                        action = item.action,
                        payload = payload,
                        client_timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }.format(java.util.Date(item.createdAt))
                    )
                }

                val pushResponse = ApiClient.apiService.pushSync(
                    SyncPushRequestDto(
                        device_id = deviceId,
                        items = pushItems
                    )
                )

                for (res in pushResponse.results) {
                    if (res.status == "SUCCESS" || res.status == "DUPLICATE_IGNORED") {
                        outboxDao.markSynced(res.operation_id)
                    } else if (res.status == "REJECTED_CONFLICT") {
                        outboxDao.markFailed(res.operation_id)
                    }
                }
            }

            // 2. Pull server changes and update cache (Products, Batches, Customers)
            val changes = ApiClient.apiService.pullSyncChanges(deviceId)
            
            // Refresh local product cache
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
                    isLowStock = p.current_stock_base <= p.min_stock_alert,
                    lastCachedAt = System.currentTimeMillis()
                )
            }
            db.productDao().clearProducts()
            db.productDao().insertProducts(entities)

            // Pull and upsert active customers so both devices stay synchronized
            val serverCustomers = ApiClient.apiService.getCustomers(isActive = true)
            val customerEntities = serverCustomers.map { c ->
                CustomerEntity(
                    id = c.id,
                    name = c.name,
                    tradeName = c.trade_name,
                    customerType = c.customer_type,
                    contactPerson = c.contact_person,
                    phone = c.phone,
                    alternatePhone = c.alternate_phone,
                    email = c.email,
                    address = c.address,
                    billingAddress = c.billing_address,
                    deliveryAddress = c.delivery_address,
                    area = c.area,
                    city = c.city,
                    district = c.district,
                    stateCode = c.state_code,
                    postalCode = c.postal_code,
                    gstRegistrationStatus = c.gst_registration_status,
                    gstNumber = c.gst_number,
                    creditEnabled = c.credit_enabled,
                    creditLimit = c.credit_limit,
                    currentBalance = c.current_balance,
                    paymentTermsDays = c.payment_terms_days,
                    notes = c.notes,
                    isActive = c.is_active,
                    lastCachedAt = System.currentTimeMillis()
                )
            }
            customerDao.insertCustomers(customerEntities)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
