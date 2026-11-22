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
import com.dairyflow.app.data.local.entities.BatchEntity;
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
public final class BatchDao_Impl implements BatchDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BatchEntity> __insertionAdapterOfBatchEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearBatches;

  public BatchDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBatchEntity = new EntityInsertionAdapter<BatchEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_batches` (`id`,`productId`,`batchNo`,`availableBaseQty`,`expiryDate`,`status`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BatchEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getProductId());
        statement.bindString(3, entity.getBatchNo());
        statement.bindDouble(4, entity.getAvailableBaseQty());
        statement.bindString(5, entity.getExpiryDate());
        statement.bindString(6, entity.getStatus());
      }
    };
    this.__preparedStmtOfClearBatches = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_batches";
        return _query;
      }
    };
  }

  @Override
  public Object insertBatches(final List<BatchEntity> batches,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBatchEntity.insert(batches);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearBatches(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearBatches.acquire();
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
          __preparedStmtOfClearBatches.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BatchEntity>> getBatchesForProduct(final String productId) {
    final String _sql = "SELECT * FROM cached_batches WHERE productId = ? ORDER BY expiryDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, productId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cached_batches"}, new Callable<List<BatchEntity>>() {
      @Override
      @NonNull
      public List<BatchEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfProductId = CursorUtil.getColumnIndexOrThrow(_cursor, "productId");
          final int _cursorIndexOfBatchNo = CursorUtil.getColumnIndexOrThrow(_cursor, "batchNo");
          final int _cursorIndexOfAvailableBaseQty = CursorUtil.getColumnIndexOrThrow(_cursor, "availableBaseQty");
          final int _cursorIndexOfExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "expiryDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<BatchEntity> _result = new ArrayList<BatchEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BatchEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpProductId;
            _tmpProductId = _cursor.getString(_cursorIndexOfProductId);
            final String _tmpBatchNo;
            _tmpBatchNo = _cursor.getString(_cursorIndexOfBatchNo);
            final double _tmpAvailableBaseQty;
            _tmpAvailableBaseQty = _cursor.getDouble(_cursorIndexOfAvailableBaseQty);
            final String _tmpExpiryDate;
            _tmpExpiryDate = _cursor.getString(_cursorIndexOfExpiryDate);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _item = new BatchEntity(_tmpId,_tmpProductId,_tmpBatchNo,_tmpAvailableBaseQty,_tmpExpiryDate,_tmpStatus);
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

  @Override
  public Flow<List<BatchEntity>> getAllBatches() {
    final String _sql = "SELECT * FROM cached_batches ORDER BY expiryDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cached_batches"}, new Callable<List<BatchEntity>>() {
      @Override
      @NonNull
      public List<BatchEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfProductId = CursorUtil.getColumnIndexOrThrow(_cursor, "productId");
          final int _cursorIndexOfBatchNo = CursorUtil.getColumnIndexOrThrow(_cursor, "batchNo");
          final int _cursorIndexOfAvailableBaseQty = CursorUtil.getColumnIndexOrThrow(_cursor, "availableBaseQty");
          final int _cursorIndexOfExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "expiryDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<BatchEntity> _result = new ArrayList<BatchEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BatchEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpProductId;
            _tmpProductId = _cursor.getString(_cursorIndexOfProductId);
            final String _tmpBatchNo;
            _tmpBatchNo = _cursor.getString(_cursorIndexOfBatchNo);
            final double _tmpAvailableBaseQty;
            _tmpAvailableBaseQty = _cursor.getDouble(_cursorIndexOfAvailableBaseQty);
            final String _tmpExpiryDate;
            _tmpExpiryDate = _cursor.getString(_cursorIndexOfExpiryDate);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            _item = new BatchEntity(_tmpId,_tmpProductId,_tmpBatchNo,_tmpAvailableBaseQty,_tmpExpiryDate,_tmpStatus);
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
