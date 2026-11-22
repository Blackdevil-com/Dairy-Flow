package com.dairyflow.app.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.dairyflow.app.data.local.entities.ProductEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ProductDao_Impl implements ProductDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ProductEntity> __insertionAdapterOfProductEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearProducts;

  public ProductDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfProductEntity = new EntityInsertionAdapter<ProductEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_products` (`id`,`sku`,`name`,`canonicalUnit`,`defaultSalePrice`,`currentStockBase`,`minStockAlert`,`isLowStock`,`lastCachedAt`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProductEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSku());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getCanonicalUnit());
        statement.bindDouble(5, entity.getDefaultSalePrice());
        statement.bindDouble(6, entity.getCurrentStockBase());
        statement.bindDouble(7, entity.getMinStockAlert());
        final int _tmp = entity.isLowStock() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getLastCachedAt());
      }
    };
    this.__preparedStmtOfClearProducts = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_products";
        return _query;
      }
    };
  }

  @Override
  public Object insertProducts(final List<ProductEntity> products,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfProductEntity.insert(products);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearProducts(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearProducts.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearProducts.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ProductEntity>> getAllProducts() {
    final String _sql = "SELECT * FROM cached_products ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cached_products"}, new Callable<List<ProductEntity>>() {
      @Override
      @NonNull
      public List<ProductEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSku = CursorUtil.getColumnIndexOrThrow(_cursor, "sku");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCanonicalUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "canonicalUnit");
          final int _cursorIndexOfDefaultSalePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultSalePrice");
          final int _cursorIndexOfCurrentStockBase = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStockBase");
          final int _cursorIndexOfMinStockAlert = CursorUtil.getColumnIndexOrThrow(_cursor, "minStockAlert");
          final int _cursorIndexOfIsLowStock = CursorUtil.getColumnIndexOrThrow(_cursor, "isLowStock");
          final int _cursorIndexOfLastCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastCachedAt");
          final List<ProductEntity> _result = new ArrayList<ProductEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ProductEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSku;
            _tmpSku = _cursor.getString(_cursorIndexOfSku);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCanonicalUnit;
            _tmpCanonicalUnit = _cursor.getString(_cursorIndexOfCanonicalUnit);
            final double _tmpDefaultSalePrice;
            _tmpDefaultSalePrice = _cursor.getDouble(_cursorIndexOfDefaultSalePrice);
            final double _tmpCurrentStockBase;
            _tmpCurrentStockBase = _cursor.getDouble(_cursorIndexOfCurrentStockBase);
            final double _tmpMinStockAlert;
            _tmpMinStockAlert = _cursor.getDouble(_cursorIndexOfMinStockAlert);
            final boolean _tmpIsLowStock;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLowStock);
            _tmpIsLowStock = _tmp != 0;
            final long _tmpLastCachedAt;
            _tmpLastCachedAt = _cursor.getLong(_cursorIndexOfLastCachedAt);
            _item = new ProductEntity(_tmpId,_tmpSku,_tmpName,_tmpCanonicalUnit,_tmpDefaultSalePrice,_tmpCurrentStockBase,_tmpMinStockAlert,_tmpIsLowStock,_tmpLastCachedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
