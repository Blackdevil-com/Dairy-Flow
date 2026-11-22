package com.dairyflow.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val name: String,
    val canonicalUnit: String,
    val defaultSalePrice: Double,
    val currentStockBase: Double,
    val minStockAlert: Double,
    val isLowStock: Boolean,
    val lastCachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_batches")
data class BatchEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val batchNo: String,
    val availableBaseQty: Double,
    val expiryDate: String,
    val status: String
)

@Entity(tableName = "cached_customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tradeName: String?,
    val customerType: String,
    val phone: String,
    val currentBalance: Double,
    val creditLimit: Double,
    val lastCachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_outbox")
data class OutboxEntity(
    @PrimaryKey val operationId: String,
    val entityType: String, // "DELIVERY_DRAFT", "CUSTOMER", "PURCHASE_RECEIPT"
    val action: String, // "CREATE", "UPDATE"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // "PENDING", "SYNCED", "FAILED"
)
