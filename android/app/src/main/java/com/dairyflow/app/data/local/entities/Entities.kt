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
    val tradeName: String? = null,
    val customerType: String = "RETAIL_SHOP",
    val contactPerson: String? = null,
    val phone: String,
    val alternatePhone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val billingAddress: String? = null,
    val deliveryAddress: String? = null,
    val area: String? = null,
    val city: String? = null,
    val district: String? = null,
    val stateCode: String? = null,
    val postalCode: String? = null,
    val gstRegistrationStatus: String = "UNKNOWN",
    val gstNumber: String? = null,
    val creditEnabled: Boolean = false,
    val creditLimit: Double = 0.0,
    val currentBalance: Double = 0.0,
    val paymentTermsDays: Int = 0,
    val notes: String? = null,
    val isActive: Boolean = true,
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
