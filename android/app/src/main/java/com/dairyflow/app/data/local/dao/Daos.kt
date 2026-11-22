package com.dairyflow.app.data.local.dao

import androidx.room.*
import com.dairyflow.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM cached_products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM cached_products")
    suspend fun clearProducts()
}

@Dao
interface BatchDao {
    @Query("SELECT * FROM cached_batches WHERE productId = :productId ORDER BY expiryDate ASC")
    fun getBatchesForProduct(productId: String): Flow<List<BatchEntity>>

    @Query("SELECT * FROM cached_batches ORDER BY expiryDate ASC")
    fun getAllBatches(): Flow<List<BatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<BatchEntity>)

    @Query("DELETE FROM cached_batches")
    suspend fun clearBatches()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM cached_customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Query("DELETE FROM cached_customers")
    suspend fun clearCustomers()
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOutbox(): List<OutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutbox(item: OutboxEntity)

    @Query("UPDATE sync_outbox SET status = 'SYNCED' WHERE operationId = :opId")
    suspend fun markSynced(opId: String)

    @Query("UPDATE sync_outbox SET status = 'FAILED' WHERE operationId = :opId")
    suspend fun markFailed(opId: String)

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
}
