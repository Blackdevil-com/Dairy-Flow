package com.dairyflow.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dairyflow.app.data.local.dao.*
import com.dairyflow.app.data.local.entities.*

@Database(
    entities = [
        ProductEntity::class,
        BatchEntity::class,
        CustomerEntity::class,
        OutboxEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DairyDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun batchDao(): BatchDao
    abstract fun customerDao(): CustomerDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        @Volatile
        private var INSTANCE: DairyDatabase? = null

        fun getDatabase(context: Context): DairyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DairyDatabase::class.java,
                    "dairyflow_local.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
