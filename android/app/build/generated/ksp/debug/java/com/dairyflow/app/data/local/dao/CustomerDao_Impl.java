package com.dairyflow.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.dairyflow.app.data.local.entities.CustomerEntity;
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
public final class CustomerDao_Impl implements CustomerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CustomerEntity> __insertionAdapterOfCustomerEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeactivateCustomer;

  private final SharedSQLiteStatement __preparedStmtOfClearCustomers;

  public CustomerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCustomerEntity = new EntityInsertionAdapter<CustomerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_customers` (`id`,`name`,`tradeName`,`customerType`,`contactPerson`,`phone`,`alternatePhone`,`email`,`address`,`billingAddress`,`deliveryAddress`,`area`,`city`,`district`,`stateCode`,`postalCode`,`gstRegistrationStatus`,`gstNumber`,`creditEnabled`,`creditLimit`,`currentBalance`,`paymentTermsDays`,`notes`,`isActive`,`lastCachedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomerEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getTradeName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getTradeName());
        }
        statement.bindString(4, entity.getCustomerType());
        if (entity.getContactPerson() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getContactPerson());
        }
        statement.bindString(6, entity.getPhone());
        if (entity.getAlternatePhone() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getAlternatePhone());
        }
        if (entity.getEmail() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getEmail());
        }
        if (entity.getAddress() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAddress());
        }
        if (entity.getBillingAddress() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getBillingAddress());
        }
        if (entity.getDeliveryAddress() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getDeliveryAddress());
        }
        if (entity.getArea() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getArea());
        }
        if (entity.getCity() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getCity());
        }
        if (entity.getDistrict() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getDistrict());
        }
        if (entity.getStateCode() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getStateCode());
        }
        if (entity.getPostalCode() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getPostalCode());
        }
        statement.bindString(17, entity.getGstRegistrationStatus());
        if (entity.getGstNumber() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getGstNumber());
        }
        final int _tmp = entity.getCreditEnabled() ? 1 : 0;
        statement.bindLong(19, _tmp);
        statement.bindDouble(20, entity.getCreditLimit());
        statement.bindDouble(21, entity.getCurrentBalance());
        statement.bindLong(22, entity.getPaymentTermsDays());
        if (entity.getNotes() == null) {
          statement.bindNull(23);
        } else {
          statement.bindString(23, entity.getNotes());
        }
        final int _tmp_1 = entity.isActive() ? 1 : 0;
        statement.bindLong(24, _tmp_1);
        statement.bindLong(25, entity.getLastCachedAt());
      }
    };
    this.__preparedStmtOfDeactivateCustomer = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE cached_customers SET isActive = 0 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearCustomers = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_customers";
        return _query;
      }
    };
  }

  @Override
  public Object insertCustomers(final List<CustomerEntity> customers,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCustomerEntity.insert(customers);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCustomer(final CustomerEntity customer,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCustomerEntity.insert(customer);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deactivateCustomer(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeactivateCustomer.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDeactivateCustomer.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearCustomers(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearCustomers.acquire();
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
          __preparedStmtOfClearCustomers.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CustomerEntity>> getAllCustomers() {
    final String _sql = "SELECT * FROM cached_customers WHERE isActive = 1 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cached_customers"}, new Callable<List<CustomerEntity>>() {
      @Override
      @NonNull
      public List<CustomerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTradeName = CursorUtil.getColumnIndexOrThrow(_cursor, "tradeName");
          final int _cursorIndexOfCustomerType = CursorUtil.getColumnIndexOrThrow(_cursor, "customerType");
          final int _cursorIndexOfContactPerson = CursorUtil.getColumnIndexOrThrow(_cursor, "contactPerson");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfAlternatePhone = CursorUtil.getColumnIndexOrThrow(_cursor, "alternatePhone");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfBillingAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "billingAddress");
          final int _cursorIndexOfDeliveryAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveryAddress");
          final int _cursorIndexOfArea = CursorUtil.getColumnIndexOrThrow(_cursor, "area");
          final int _cursorIndexOfCity = CursorUtil.getColumnIndexOrThrow(_cursor, "city");
          final int _cursorIndexOfDistrict = CursorUtil.getColumnIndexOrThrow(_cursor, "district");
          final int _cursorIndexOfStateCode = CursorUtil.getColumnIndexOrThrow(_cursor, "stateCode");
          final int _cursorIndexOfPostalCode = CursorUtil.getColumnIndexOrThrow(_cursor, "postalCode");
          final int _cursorIndexOfGstRegistrationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "gstRegistrationStatus");
          final int _cursorIndexOfGstNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "gstNumber");
          final int _cursorIndexOfCreditEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "creditEnabled");
          final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
          final int _cursorIndexOfCurrentBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "currentBalance");
          final int _cursorIndexOfPaymentTermsDays = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentTermsDays");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfLastCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastCachedAt");
          final List<CustomerEntity> _result = new ArrayList<CustomerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomerEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTradeName;
            if (_cursor.isNull(_cursorIndexOfTradeName)) {
              _tmpTradeName = null;
            } else {
              _tmpTradeName = _cursor.getString(_cursorIndexOfTradeName);
            }
            final String _tmpCustomerType;
            _tmpCustomerType = _cursor.getString(_cursorIndexOfCustomerType);
            final String _tmpContactPerson;
            if (_cursor.isNull(_cursorIndexOfContactPerson)) {
              _tmpContactPerson = null;
            } else {
              _tmpContactPerson = _cursor.getString(_cursorIndexOfContactPerson);
            }
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpAlternatePhone;
            if (_cursor.isNull(_cursorIndexOfAlternatePhone)) {
              _tmpAlternatePhone = null;
            } else {
              _tmpAlternatePhone = _cursor.getString(_cursorIndexOfAlternatePhone);
            }
            final String _tmpEmail;
            if (_cursor.isNull(_cursorIndexOfEmail)) {
              _tmpEmail = null;
            } else {
              _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final String _tmpBillingAddress;
            if (_cursor.isNull(_cursorIndexOfBillingAddress)) {
              _tmpBillingAddress = null;
            } else {
              _tmpBillingAddress = _cursor.getString(_cursorIndexOfBillingAddress);
            }
            final String _tmpDeliveryAddress;
            if (_cursor.isNull(_cursorIndexOfDeliveryAddress)) {
              _tmpDeliveryAddress = null;
            } else {
              _tmpDeliveryAddress = _cursor.getString(_cursorIndexOfDeliveryAddress);
            }
            final String _tmpArea;
            if (_cursor.isNull(_cursorIndexOfArea)) {
              _tmpArea = null;
            } else {
              _tmpArea = _cursor.getString(_cursorIndexOfArea);
            }
            final String _tmpCity;
            if (_cursor.isNull(_cursorIndexOfCity)) {
              _tmpCity = null;
            } else {
              _tmpCity = _cursor.getString(_cursorIndexOfCity);
            }
            final String _tmpDistrict;
            if (_cursor.isNull(_cursorIndexOfDistrict)) {
              _tmpDistrict = null;
            } else {
              _tmpDistrict = _cursor.getString(_cursorIndexOfDistrict);
            }
            final String _tmpStateCode;
            if (_cursor.isNull(_cursorIndexOfStateCode)) {
              _tmpStateCode = null;
            } else {
              _tmpStateCode = _cursor.getString(_cursorIndexOfStateCode);
            }
            final String _tmpPostalCode;
            if (_cursor.isNull(_cursorIndexOfPostalCode)) {
              _tmpPostalCode = null;
            } else {
              _tmpPostalCode = _cursor.getString(_cursorIndexOfPostalCode);
            }
            final String _tmpGstRegistrationStatus;
            _tmpGstRegistrationStatus = _cursor.getString(_cursorIndexOfGstRegistrationStatus);
            final String _tmpGstNumber;
            if (_cursor.isNull(_cursorIndexOfGstNumber)) {
              _tmpGstNumber = null;
            } else {
              _tmpGstNumber = _cursor.getString(_cursorIndexOfGstNumber);
            }
            final boolean _tmpCreditEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCreditEnabled);
            _tmpCreditEnabled = _tmp != 0;
            final double _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
            final double _tmpCurrentBalance;
            _tmpCurrentBalance = _cursor.getDouble(_cursorIndexOfCurrentBalance);
            final int _tmpPaymentTermsDays;
            _tmpPaymentTermsDays = _cursor.getInt(_cursorIndexOfPaymentTermsDays);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final boolean _tmpIsActive;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_1 != 0;
            final long _tmpLastCachedAt;
            _tmpLastCachedAt = _cursor.getLong(_cursorIndexOfLastCachedAt);
            _item = new CustomerEntity(_tmpId,_tmpName,_tmpTradeName,_tmpCustomerType,_tmpContactPerson,_tmpPhone,_tmpAlternatePhone,_tmpEmail,_tmpAddress,_tmpBillingAddress,_tmpDeliveryAddress,_tmpArea,_tmpCity,_tmpDistrict,_tmpStateCode,_tmpPostalCode,_tmpGstRegistrationStatus,_tmpGstNumber,_tmpCreditEnabled,_tmpCreditLimit,_tmpCurrentBalance,_tmpPaymentTermsDays,_tmpNotes,_tmpIsActive,_tmpLastCachedAt);
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
  public Object getCustomerById(final String id,
      final Continuation<? super CustomerEntity> $completion) {
    final String _sql = "SELECT * FROM cached_customers WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CustomerEntity>() {
      @Override
      @Nullable
      public CustomerEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTradeName = CursorUtil.getColumnIndexOrThrow(_cursor, "tradeName");
          final int _cursorIndexOfCustomerType = CursorUtil.getColumnIndexOrThrow(_cursor, "customerType");
          final int _cursorIndexOfContactPerson = CursorUtil.getColumnIndexOrThrow(_cursor, "contactPerson");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfAlternatePhone = CursorUtil.getColumnIndexOrThrow(_cursor, "alternatePhone");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfBillingAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "billingAddress");
          final int _cursorIndexOfDeliveryAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deliveryAddress");
          final int _cursorIndexOfArea = CursorUtil.getColumnIndexOrThrow(_cursor, "area");
          final int _cursorIndexOfCity = CursorUtil.getColumnIndexOrThrow(_cursor, "city");
          final int _cursorIndexOfDistrict = CursorUtil.getColumnIndexOrThrow(_cursor, "district");
          final int _cursorIndexOfStateCode = CursorUtil.getColumnIndexOrThrow(_cursor, "stateCode");
          final int _cursorIndexOfPostalCode = CursorUtil.getColumnIndexOrThrow(_cursor, "postalCode");
          final int _cursorIndexOfGstRegistrationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "gstRegistrationStatus");
          final int _cursorIndexOfGstNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "gstNumber");
          final int _cursorIndexOfCreditEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "creditEnabled");
          final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
          final int _cursorIndexOfCurrentBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "currentBalance");
          final int _cursorIndexOfPaymentTermsDays = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentTermsDays");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfLastCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastCachedAt");
          final CustomerEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTradeName;
            if (_cursor.isNull(_cursorIndexOfTradeName)) {
              _tmpTradeName = null;
            } else {
              _tmpTradeName = _cursor.getString(_cursorIndexOfTradeName);
            }
            final String _tmpCustomerType;
            _tmpCustomerType = _cursor.getString(_cursorIndexOfCustomerType);
            final String _tmpContactPerson;
            if (_cursor.isNull(_cursorIndexOfContactPerson)) {
              _tmpContactPerson = null;
            } else {
              _tmpContactPerson = _cursor.getString(_cursorIndexOfContactPerson);
            }
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpAlternatePhone;
            if (_cursor.isNull(_cursorIndexOfAlternatePhone)) {
              _tmpAlternatePhone = null;
            } else {
              _tmpAlternatePhone = _cursor.getString(_cursorIndexOfAlternatePhone);
            }
            final String _tmpEmail;
            if (_cursor.isNull(_cursorIndexOfEmail)) {
              _tmpEmail = null;
            } else {
              _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final String _tmpBillingAddress;
            if (_cursor.isNull(_cursorIndexOfBillingAddress)) {
              _tmpBillingAddress = null;
            } else {
              _tmpBillingAddress = _cursor.getString(_cursorIndexOfBillingAddress);
            }
            final String _tmpDeliveryAddress;
            if (_cursor.isNull(_cursorIndexOfDeliveryAddress)) {
              _tmpDeliveryAddress = null;
            } else {
              _tmpDeliveryAddress = _cursor.getString(_cursorIndexOfDeliveryAddress);
            }
            final String _tmpArea;
            if (_cursor.isNull(_cursorIndexOfArea)) {
              _tmpArea = null;
            } else {
              _tmpArea = _cursor.getString(_cursorIndexOfArea);
            }
            final String _tmpCity;
            if (_cursor.isNull(_cursorIndexOfCity)) {
              _tmpCity = null;
            } else {
              _tmpCity = _cursor.getString(_cursorIndexOfCity);
            }
            final String _tmpDistrict;
            if (_cursor.isNull(_cursorIndexOfDistrict)) {
              _tmpDistrict = null;
            } else {
              _tmpDistrict = _cursor.getString(_cursorIndexOfDistrict);
            }
            final String _tmpStateCode;
            if (_cursor.isNull(_cursorIndexOfStateCode)) {
              _tmpStateCode = null;
            } else {
              _tmpStateCode = _cursor.getString(_cursorIndexOfStateCode);
            }
            final String _tmpPostalCode;
            if (_cursor.isNull(_cursorIndexOfPostalCode)) {
              _tmpPostalCode = null;
            } else {
              _tmpPostalCode = _cursor.getString(_cursorIndexOfPostalCode);
            }
            final String _tmpGstRegistrationStatus;
            _tmpGstRegistrationStatus = _cursor.getString(_cursorIndexOfGstRegistrationStatus);
            final String _tmpGstNumber;
            if (_cursor.isNull(_cursorIndexOfGstNumber)) {
              _tmpGstNumber = null;
            } else {
              _tmpGstNumber = _cursor.getString(_cursorIndexOfGstNumber);
            }
            final boolean _tmpCreditEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCreditEnabled);
            _tmpCreditEnabled = _tmp != 0;
            final double _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getDouble(_cursorIndexOfCreditLimit);
            final double _tmpCurrentBalance;
            _tmpCurrentBalance = _cursor.getDouble(_cursorIndexOfCurrentBalance);
            final int _tmpPaymentTermsDays;
            _tmpPaymentTermsDays = _cursor.getInt(_cursorIndexOfPaymentTermsDays);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final boolean _tmpIsActive;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp_1 != 0;
            final long _tmpLastCachedAt;
            _tmpLastCachedAt = _cursor.getLong(_cursorIndexOfLastCachedAt);
            _result = new CustomerEntity(_tmpId,_tmpName,_tmpTradeName,_tmpCustomerType,_tmpContactPerson,_tmpPhone,_tmpAlternatePhone,_tmpEmail,_tmpAddress,_tmpBillingAddress,_tmpDeliveryAddress,_tmpArea,_tmpCity,_tmpDistrict,_tmpStateCode,_tmpPostalCode,_tmpGstRegistrationStatus,_tmpGstNumber,_tmpCreditEnabled,_tmpCreditLimit,_tmpCurrentBalance,_tmpPaymentTermsDays,_tmpNotes,_tmpIsActive,_tmpLastCachedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
