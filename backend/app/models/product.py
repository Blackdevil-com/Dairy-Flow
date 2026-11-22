import uuid
from datetime import datetime, date
from decimal import Decimal
from typing import Optional, List
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Text, DateTime, Date, Boolean, ForeignKey, Numeric, Index, UniqueConstraint
from app.db.base import Base, utc_now

class ProductCategory(Base):
    __tablename__ = "product_categories"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    name: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)

    products: Mapped[List["Product"]] = relationship("Product", back_populates="category")


class Unit(Base):
    __tablename__ = "units"

    code: Mapped[str] = mapped_column(String(20), primary_key=True)  # LTR, KG, PKT, CRT, GM, ML
    name: Mapped[str] = mapped_column(String(50), nullable=False)
    symbol: Mapped[str] = mapped_column(String(20), nullable=False)
    is_base_standard: Mapped[bool] = mapped_column(Boolean, default=False)  # True for canonical types (LTR, KG, PKT)


class Product(Base):
    __tablename__ = "products"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    category_id: Mapped[str] = mapped_column(String(36), ForeignKey("product_categories.id"), nullable=False, index=True)
    sku: Mapped[str] = mapped_column(String(50), unique=True, nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(200), nullable=False, index=True)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    canonical_unit_code: Mapped[str] = mapped_column(String(20), ForeignKey("units.code"), nullable=False)
    
    # Pricing & Alert thresholds
    default_sale_price: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    default_cost_price: Mapped[Decimal] = mapped_column(Numeric(12, 2), default=Decimal("0.00"), nullable=False)
    min_stock_alert: Mapped[Decimal] = mapped_column(Numeric(12, 3), default=Decimal("20.000"), nullable=False)
    
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now, onupdate=utc_now)

    # Relationships
    category: Mapped["ProductCategory"] = relationship("ProductCategory", back_populates="products")
    units: Mapped[List["ProductUnit"]] = relationship("ProductUnit", back_populates="product", cascade="all, delete-orphan")
    tax_profiles: Mapped[List["ProductTaxProfile"]] = relationship("ProductTaxProfile", back_populates="product", cascade="all, delete-orphan")


class ProductUnit(Base):
    __tablename__ = "product_units"
    __table_args__ = (
        UniqueConstraint("product_id", "unit_code", name="uq_product_unit"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    product_id: Mapped[str] = mapped_column(String(36), ForeignKey("products.id"), nullable=False, index=True)
    unit_code: Mapped[str] = mapped_column(String(20), ForeignKey("units.code"), nullable=False)
    conversion_to_base: Mapped[Decimal] = mapped_column(Numeric(14, 4), nullable=False)  # e.g., 1 CRT of 500ml milk = 12.0000 LTR
    is_purchase_unit: Mapped[bool] = mapped_column(Boolean, default=True)
    is_delivery_unit: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)

    product: Mapped["Product"] = relationship("Product", back_populates="units")


class ProductTaxProfile(Base):
    __tablename__ = "product_tax_profiles"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    product_id: Mapped[str] = mapped_column(String(36), ForeignKey("products.id"), nullable=False, index=True)
    hsn_code: Mapped[str] = mapped_column(String(20), default="0401", nullable=False)  # Dairy HSN e.g. 0401 (Fresh Milk)
    cgst_rate: Mapped[Decimal] = mapped_column(Numeric(5, 2), default=Decimal("0.00"), nullable=False)
    sgst_rate: Mapped[Decimal] = mapped_column(Numeric(5, 2), default=Decimal("0.00"), nullable=False)
    igst_rate: Mapped[Decimal] = mapped_column(Numeric(5, 2), default=Decimal("0.00"), nullable=False)
    is_demo: Mapped[bool] = mapped_column(Boolean, default=True)  # Demo tax rate until verified
    effective_from: Mapped[date] = mapped_column(Date, default=date.today)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=utc_now)

    product: Mapped["Product"] = relationship("Product", back_populates="tax_profiles")
