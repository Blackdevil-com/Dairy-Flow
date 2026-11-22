package com.dairyflow.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dairyflow.app.data.local.dao.*
import com.dairyflow.app.data.local.entities.*

@Database(
    entities = [
        ProductEntity::class,
        BatchEntity::class,
        CustomerEntity::class,
        OutboxEntity::class
    ],
    version = 2,
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN contactPerson TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN alternatePhone TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN email TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN address TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN billingAddress TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN deliveryAddress TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN area TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN city TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN district TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN stateCode TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN postalCode TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN gstRegistrationStatus TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN gstNumber TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN creditEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN paymentTermsDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN notes TEXT")
                db.execSQL("ALTER TABLE cached_customers ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): DairyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DairyDatabase::class.java,
                    "dairyflow_local.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
