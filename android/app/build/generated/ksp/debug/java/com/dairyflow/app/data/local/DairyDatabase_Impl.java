package com.dairyflow.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.dairyflow.app.data.local.dao.BatchDao;
import com.dairyflow.app.data.local.dao.BatchDao_Impl;
import com.dairyflow.app.data.local.dao.CustomerDao;
import com.dairyflow.app.data.local.dao.CustomerDao_Impl;
import com.dairyflow.app.data.local.dao.OutboxDao;
import com.dairyflow.app.data.local.dao.OutboxDao_Impl;
import com.dairyflow.app.data.local.dao.ProductDao;
import com.dairyflow.app.data.local.dao.ProductDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DairyDatabase_Impl extends DairyDatabase {
  private volatile ProductDao _productDao;

  private volatile BatchDao _batchDao;

  private volatile CustomerDao _customerDao;

  private volatile OutboxDao _outboxDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_products` (`id` TEXT NOT NULL, `sku` TEXT NOT NULL, `name` TEXT NOT NULL, `canonicalUnit` TEXT NOT NULL, `defaultSalePrice` REAL NOT NULL, `currentStockBase` REAL NOT NULL, `minStockAlert` REAL NOT NULL, `isLowStock` INTEGER NOT NULL, `lastCachedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_batches` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, `batchNo` TEXT NOT NULL, `availableBaseQty` REAL NOT NULL, `expiryDate` TEXT NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_customers` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `tradeName` TEXT, `customerType` TEXT NOT NULL, `contactPerson` TEXT, `phone` TEXT NOT NULL, `alternatePhone` TEXT, `email` TEXT, `address` TEXT, `billingAddress` TEXT, `deliveryAddress` TEXT, `area` TEXT, `city` TEXT, `district` TEXT, `stateCode` TEXT, `postalCode` TEXT, `gstRegistrationStatus` TEXT NOT NULL, `gstNumber` TEXT, `creditEnabled` INTEGER NOT NULL, `creditLimit` REAL NOT NULL, `currentBalance` REAL NOT NULL, `paymentTermsDays` INTEGER NOT NULL, `notes` TEXT, `isActive` INTEGER NOT NULL, `lastCachedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_outbox` (`operationId` TEXT NOT NULL, `entityType` TEXT NOT NULL, `action` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`operationId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '367cff0238c44d764597ece3643f5142')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `cached_products`");
        db.execSQL("DROP TABLE IF EXISTS `cached_batches`");
        db.execSQL("DROP TABLE IF EXISTS `cached_customers`");
        db.execSQL("DROP TABLE IF EXISTS `sync_outbox`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCachedProducts = new HashMap<String, TableInfo.Column>(9);
        _columnsCachedProducts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedProducts.put("sku", new TableInfo.Column("sku", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedProducts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedProducts.put("canonicalUnit", new TableInfo.Column("canonicalUnit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedProducts.put("defaultSalePrice", new TableInfo.Column("defaultSalePrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedProducts.put("currentStockBase", new TableInfo.Column("currentStockBase", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedProducts.put("minStockAlert", new TableInfo.Column("minStockAlert", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedProducts.put("isLowStock", new TableInfo.Column("isLowStock", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedProducts.put("lastCachedAt", new TableInfo.Column("lastCachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedProducts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedProducts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedProducts = new TableInfo("cached_products", _columnsCachedProducts, _foreignKeysCachedProducts, _indicesCachedProducts);
        final TableInfo _existingCachedProducts = TableInfo.read(db, "cached_products");
        if (!_infoCachedProducts.equals(_existingCachedProducts)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_products(com.dairyflow.app.data.local.entities.ProductEntity).\n"
                  + " Expected:\n" + _infoCachedProducts + "\n"
                  + " Found:\n" + _existingCachedProducts);
        }
        final HashMap<String, TableInfo.Column> _columnsCachedBatches = new HashMap<String, TableInfo.Column>(6);
        _columnsCachedBatches.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBatches.put("productId", new TableInfo.Column("productId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBatches.put("batchNo", new TableInfo.Column("batchNo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBatches.put("availableBaseQty", new TableInfo.Column("availableBaseQty", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBatches.put("expiryDate", new TableInfo.Column("expiryDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBatches.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedBatches = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedBatches = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedBatches = new TableInfo("cached_batches", _columnsCachedBatches, _foreignKeysCachedBatches, _indicesCachedBatches);
        final TableInfo _existingCachedBatches = TableInfo.read(db, "cached_batches");
        if (!_infoCachedBatches.equals(_existingCachedBatches)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_batches(com.dairyflow.app.data.local.entities.BatchEntity).\n"
                  + " Expected:\n" + _infoCachedBatches + "\n"
                  + " Found:\n" + _existingCachedBatches);
        }
        final HashMap<String, TableInfo.Column> _columnsCachedCustomers = new HashMap<String, TableInfo.Column>(25);
        _columnsCachedCustomers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("tradeName", new TableInfo.Column("tradeName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("customerType", new TableInfo.Column("customerType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("contactPerson", new TableInfo.Column("contactPerson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("phone", new TableInfo.Column("phone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("alternatePhone", new TableInfo.Column("alternatePhone", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("email", new TableInfo.Column("email", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("address", new TableInfo.Column("address", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("billingAddress", new TableInfo.Column("billingAddress", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("deliveryAddress", new TableInfo.Column("deliveryAddress", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("area", new TableInfo.Column("area", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("city", new TableInfo.Column("city", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("district", new TableInfo.Column("district", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("stateCode", new TableInfo.Column("stateCode", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("postalCode", new TableInfo.Column("postalCode", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("gstRegistrationStatus", new TableInfo.Column("gstRegistrationStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("gstNumber", new TableInfo.Column("gstNumber", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("creditEnabled", new TableInfo.Column("creditEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("creditLimit", new TableInfo.Column("creditLimit", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("currentBalance", new TableInfo.Column("currentBalance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("paymentTermsDays", new TableInfo.Column("paymentTermsDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedCustomers.put("lastCachedAt", new TableInfo.Column("lastCachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedCustomers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedCustomers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedCustomers = new TableInfo("cached_customers", _columnsCachedCustomers, _foreignKeysCachedCustomers, _indicesCachedCustomers);
        final TableInfo _existingCachedCustomers = TableInfo.read(db, "cached_customers");
        if (!_infoCachedCustomers.equals(_existingCachedCustomers)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_customers(com.dairyflow.app.data.local.entities.CustomerEntity).\n"
                  + " Expected:\n" + _infoCachedCustomers + "\n"
                  + " Found:\n" + _existingCachedCustomers);
        }
        final HashMap<String, TableInfo.Column> _columnsSyncOutbox = new HashMap<String, TableInfo.Column>(6);
        _columnsSyncOutbox.put("operationId", new TableInfo.Column("operationId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncOutbox.put("entityType", new TableInfo.Column("entityType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncOutbox.put("action", new TableInfo.Column("action", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncOutbox.put("payloadJson", new TableInfo.Column("payloadJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncOutbox.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncOutbox.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSyncOutbox = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSyncOutbox = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSyncOutbox = new TableInfo("sync_outbox", _columnsSyncOutbox, _foreignKeysSyncOutbox, _indicesSyncOutbox);
        final TableInfo _existingSyncOutbox = TableInfo.read(db, "sync_outbox");
        if (!_infoSyncOutbox.equals(_existingSyncOutbox)) {
          return new RoomOpenHelper.ValidationResult(false, "sync_outbox(com.dairyflow.app.data.local.entities.OutboxEntity).\n"
                  + " Expected:\n" + _infoSyncOutbox + "\n"
                  + " Found:\n" + _existingSyncOutbox);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "367cff0238c44d764597ece3643f5142", "192601179f946807c26b73342f7a7f0e");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "cached_products","cached_batches","cached_customers","sync_outbox");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `cached_products`");
      _db.execSQL("DELETE FROM `cached_batches`");
      _db.execSQL("DELETE FROM `cached_customers`");
      _db.execSQL("DELETE FROM `sync_outbox`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ProductDao.class, ProductDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BatchDao.class, BatchDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CustomerDao.class, CustomerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(OutboxDao.class, OutboxDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ProductDao productDao() {
    if (_productDao != null) {
      return _productDao;
    } else {
      synchronized(this) {
        if(_productDao == null) {
          _productDao = new ProductDao_Impl(this);
        }
        return _productDao;
      }
    }
  }

  @Override
  public BatchDao batchDao() {
    if (_batchDao != null) {
      return _batchDao;
    } else {
      synchronized(this) {
        if(_batchDao == null) {
          _batchDao = new BatchDao_Impl(this);
        }
        return _batchDao;
      }
    }
  }

  @Override
  public CustomerDao customerDao() {
    if (_customerDao != null) {
      return _customerDao;
    } else {
      synchronized(this) {
        if(_customerDao == null) {
          _customerDao = new CustomerDao_Impl(this);
        }
        return _customerDao;
      }
    }
  }

  @Override
  public OutboxDao outboxDao() {
    if (_outboxDao != null) {
      return _outboxDao;
    } else {
      synchronized(this) {
        if(_outboxDao == null) {
          _outboxDao = new OutboxDao_Impl(this);
        }
        return _outboxDao;
      }
    }
  }
}
