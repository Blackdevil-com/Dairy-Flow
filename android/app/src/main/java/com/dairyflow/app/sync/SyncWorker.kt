package com.dairyflow.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dairyflow.app.core.network.ApiClient
import com.dairyflow.app.data.local.DairyDatabase
import com.dairyflow.app.data.local.entities.ProductEntity
import com.dairyflow.app.data.local.entities.BatchEntity
import com.dairyflow.app.data.local.entities.CustomerEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = DairyDatabase.getDatabase(applicationContext)
        val outboxDao = db.outboxDao()

        try {
            // 1. Process local pending outbox items
            val pendingItems = outboxDao.getPendingOutbox()
            for (item in pendingItems) {
                // In production, sync each item with idempotency key
                outboxDao.markSynced(item.operationId)
            }

            // 2. Pull server changes and update cache
            val deviceId = "ANDROID-${android.os.Build.MODEL ?: "DEVICE"}"
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

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
