import uuid
from datetime import datetime, date
from decimal import Decimal
from typing import Optional, List
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Text, DateTime, Date, Boolean, ForeignKey, Numeric, Index
from app.db.base import Base, utc_now

class PurchaseReceipt(Base):
    __tablename__ = "purchase_receipts"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    receipt_no: Mapped[str] = mapped_column(String(50), unique=True, nullable=False, index=True)
    supplier_id: Mapped[str] = mapped_column(String(36), ForeignKey("suppliers.id"), nullable=False, index=True)
    warehouse_id: Mapped[str] = mapped_column(String(36), ForeignKey("warehouses.id"), nullable=False)
    invoice_reference: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    receipt_date: Mapped[date] = mapped_column(Date, default=date.today)
    total_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    status: Mapped[str] = mapped_column(String(30), default="RECEIVED")  # RECEIVED, CANCELLED
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    created_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)

    items: Mapped[List["PurchaseReceiptItem"]] = relationship("PurchaseReceiptItem", back_populates="receipt", cascade="all, delete-orphan")


class PurchaseReceiptItem(Base):
    __tablename__ = "purchase_receipt_items"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    receipt_id: Mapped[str] = mapped_column(String(36), ForeignKey("purchase_receipts.id"), nullable=False, index=True)
    product_id: Mapped[str] = mapped_column(String(36), ForeignKey("products.id"), nullable=False, index=True)
    batch_no: Mapped[str] = mapped_column(String(50), nullable=False)
    quantity_received: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False)
    unit_code: Mapped[str] = mapped_column(String(20), ForeignKey("units.code"), nullable=False)
    conversion_factor: Mapped[Decimal] = mapped_column(Numeric(14, 4), default=Decimal("1.0000"), nullable=False)
    quantity_base: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False)
    unit_cost: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    total_cost: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    mfg_date: Mapped[date] = mapped_column(Date, nullable=False)
    expiry_date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)

    receipt: Mapped["PurchaseReceipt"] = relationship("PurchaseReceipt", back_populates="items")


class StockBatch(Base):
    __tablename__ = "stock_batches"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    product_id: Mapped[str] = mapped_column(String(36), ForeignKey("products.id"), nullable=False, index=True)
    warehouse_id: Mapped[str] = mapped_column(String(36), ForeignKey("warehouses.id"), nullable=False, index=True)
    batch_no: Mapped[str] = mapped_column(String(50), nullable=False, index=True)
    mfg_date: Mapped[date] = mapped_column(Date, nullable=False)
    expiry_date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    initial_base_qty: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False)
    available_base_qty: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False, index=True)
    status: Mapped[str] = mapped_column(String(30), default="ACTIVE")  # ACTIVE, DEPLETED, EXPIRED, QUARANTINED
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now, onupdate=utc_now)


class StockMovement(Base):
    __tablename__ = "stock_movements"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    movement_no: Mapped[str] = mapped_column(String(50), unique=True, nullable=False, index=True)
    product_id: Mapped[str] = mapped_column(String(36), ForeignKey("products.id"), nullable=False, index=True)
    batch_id: Mapped[Optional[str]] = mapped_column(String(36), ForeignKey("stock_batches.id"), nullable=True, index=True)
    warehouse_id: Mapped[str] = mapped_column(String(36), ForeignKey("warehouses.id"), nullable=False)
    movement_type: Mapped[str] = mapped_column(String(50), nullable=False, index=True)
    # PURCHASE_RECEIPT, DELIVERY_DISPATCH, ADJUSTMENT_IN, ADJUSTMENT_OUT, DAMAGED_EXPIRED, DELIVERY_CANCELLATION
    signed_base_quantity: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False)  # Positive for addition, negative for deduction
    reference_type: Mapped[str] = mapped_column(String(50), nullable=False)  # PURCHASE_RECEIPT, DELIVERY, ADJUSTMENT, WRITE_OFF
    reference_id: Mapped[str] = mapped_column(String(50), nullable=False, index=True)
    remarks: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    created_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now, index=True)
