import uuid
from datetime import datetime, date
from decimal import Decimal
from typing import Optional, List
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Text, DateTime, Date, Boolean, ForeignKey, Numeric, Index, Integer, UniqueConstraint
from app.db.base import Base, utc_now

class Delivery(Base):
    __tablename__ = "deliveries"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    delivery_no: Mapped[str] = mapped_column(String(50), unique=True, nullable=False, index=True)
    customer_id: Mapped[str] = mapped_column(String(36), ForeignKey("customers.id"), nullable=False, index=True)
    warehouse_id: Mapped[str] = mapped_column(String(36), ForeignKey("warehouses.id"), nullable=False)
    status: Mapped[str] = mapped_column(String(30), default="DRAFT", nullable=False, index=True)  # DRAFT, CONFIRMED, CANCELLED
    delivery_date: Mapped[date] = mapped_column(Date, default=date.today)
    total_base_qty: Mapped[Decimal] = mapped_column(Numeric(14, 4), default=Decimal("0.0000"), nullable=False)
    subtotal: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    tax_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    discount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    net_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    delivery_address: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    idempotency_key: Mapped[Optional[str]] = mapped_column(String(100), unique=True, nullable=True, index=True)
    created_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    confirmed_by: Mapped[Optional[str]] = mapped_column(String(36), ForeignKey("users.id"), nullable=True)
    confirmed_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now, onupdate=utc_now)

    items: Mapped[List["DeliveryItem"]] = relationship("DeliveryItem", back_populates="delivery", cascade="all, delete-orphan")
    invoice: Mapped[Optional["Invoice"]] = relationship("Invoice", back_populates="delivery", uselist=False)


class DeliveryItem(Base):
    __tablename__ = "delivery_items"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    delivery_id: Mapped[str] = mapped_column(String(36), ForeignKey("deliveries.id"), nullable=False, index=True)
    product_id: Mapped[str] = mapped_column(String(36), ForeignKey("products.id"), nullable=False, index=True)
    unit_code: Mapped[str] = mapped_column(String(20), ForeignKey("units.code"), nullable=False)
    requested_qty: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False)
    conversion_factor: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False)
    base_qty: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False)
    unit_price: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=False)
    tax_rate: Mapped[Decimal] = mapped_column(Numeric(5, 2), default=Decimal("0.00"), nullable=False)
    tax_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    line_total: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)

    delivery: Mapped["Delivery"] = relationship("Delivery", back_populates="items")


class InvoiceSequence(Base):
    __tablename__ = "invoice_sequences"
    __table_args__ = (
        UniqueConstraint("financial_year", "series_prefix", name="uq_fy_series"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    financial_year: Mapped[str] = mapped_column(String(20), nullable=False)  # e.g., "2026-27"
    series_prefix: Mapped[str] = mapped_column(String(20), default="DF", nullable=False)
    current_sequence: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now, onupdate=utc_now)


class Invoice(Base):
    __tablename__ = "invoices"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    invoice_no: Mapped[str] = mapped_column(String(60), unique=True, nullable=False, index=True)
    financial_year: Mapped[str] = mapped_column(String(20), nullable=False, index=True)
    series_prefix: Mapped[str] = mapped_column(String(20), default="DF", nullable=False)
    sequence_number: Mapped[int] = mapped_column(Integer, nullable=False)
    doc_type: Mapped[str] = mapped_column(String(50), default="COMMERCIAL_RECEIPT", nullable=False)
    # COMMERCIAL_RECEIPT, TAX_INVOICE, BILL_OF_SUPPLY, PROFORMA
    delivery_id: Mapped[Optional[str]] = mapped_column(String(36), ForeignKey("deliveries.id"), nullable=True, unique=True)
    customer_id: Mapped[str] = mapped_column(String(36), ForeignKey("customers.id"), nullable=False, index=True)
    
    # Customer snapshots (immutable)
    customer_name_snap: Mapped[str] = mapped_column(String(150), nullable=False)
    customer_phone_snap: Mapped[Optional[str]] = mapped_column(String(30), nullable=True)
    customer_address_snap: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    customer_gst_snap: Mapped[Optional[str]] = mapped_column(String(20), nullable=True)

    invoice_date: Mapped[date] = mapped_column(Date, default=date.today)
    subtotal: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=False)
    total_tax: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    discount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    total_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=False)
    paid_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    outstanding_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=False)
    payment_status: Mapped[str] = mapped_column(String(30), default="UNPAID", nullable=False, index=True)  # UNPAID, PARTIAL, PAID
    
    is_demo_watermark: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    pdf_path: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    created_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)

    delivery: Mapped[Optional["Delivery"]] = relationship("Delivery", back_populates="invoice")
    items: Mapped[List["InvoiceItem"]] = relationship("InvoiceItem", back_populates="invoice", cascade="all, delete-orphan")
    allocations: Mapped[List["PaymentAllocation"]] = relationship("PaymentAllocation", back_populates="invoice")


class InvoiceItem(Base):
    __tablename__ = "invoice_items"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    invoice_id: Mapped[str] = mapped_column(String(36), ForeignKey("invoices.id"), nullable=False, index=True)
    product_id: Mapped[str] = mapped_column(String(36), ForeignKey("products.id"), nullable=False)
    product_name_snap: Mapped[str] = mapped_column(String(200), nullable=False)
    hsn_snap: Mapped[str] = mapped_column(String(20), default="0401", nullable=False)
    unit_code: Mapped[str] = mapped_column(String(20), nullable=False)
    billed_qty: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False)
    base_qty: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False)
    unit_price_snap: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=False)
    tax_rate_snap: Mapped[Decimal] = mapped_column(Numeric(5, 2), default=Decimal("0.00"), nullable=False)
    cgst_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    sgst_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    igst_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    line_total: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)

    invoice: Mapped["Invoice"] = relationship("Invoice", back_populates="items")
