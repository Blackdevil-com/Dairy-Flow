from typing import Optional, List
from datetime import datetime, date
from decimal import Decimal
from pydantic import BaseModel, Field

class UnitResponse(BaseModel):
    code: str
    name: str
    symbol: str
    is_base_standard: bool

    class Config:
        from_attributes = True

class UnitCreate(BaseModel):
    code: str
    name: str
    symbol: str
    is_base_standard: bool = False

class CategoryResponse(BaseModel):
    id: str
    name: str
    description: Optional[str] = None

    class Config:
        from_attributes = True

class CategoryCreate(BaseModel):
    name: str
    description: Optional[str] = None

class ProductUnitResponse(BaseModel):
    id: str
    unit_code: str
    conversion_to_base: Decimal
    is_purchase_unit: bool
    is_delivery_unit: bool

    class Config:
        from_attributes = True

class ProductUnitCreate(BaseModel):
    unit_code: str
    conversion_to_base: Decimal = Field(..., gt=0, description="Conversion multiplier to canonical base unit")
    is_purchase_unit: bool = True
    is_delivery_unit: bool = True

class TaxProfileResponse(BaseModel):
    id: str
    hsn_code: str
    cgst_rate: Decimal
    sgst_rate: Decimal
    igst_rate: Decimal
    is_demo: bool
    effective_from: date

    class Config:
        from_attributes = True

class ProductResponse(BaseModel):
    id: str
    category_id: str
    category_name: Optional[str] = None
    sku: str
    name: str
    description: Optional[str] = None
    canonical_unit_code: str
    default_sale_price: Decimal
    default_cost_price: Decimal
    min_stock_alert: Decimal
    is_active: bool
    current_stock_base: Decimal = Decimal("0.0000")
    units: List[ProductUnitResponse] = []
    tax_profiles: List[TaxProfileResponse] = []
    created_at: datetime

    class Config:
        from_attributes = True

class ProductCreate(BaseModel):
    category_id: str
    sku: str
    name: str
    description: Optional[str] = None
    canonical_unit_code: str  # LTR, KG, PKT, etc.
    default_sale_price: Decimal = Decimal("0.00")
    default_cost_price: Decimal = Decimal("0.00")
    min_stock_alert: Decimal = Decimal("20.000")
    hsn_code: str = "0401"
    cgst_rate: Decimal = Decimal("0.00")
    sgst_rate: Decimal = Decimal("0.00")
    igst_rate: Decimal = Decimal("0.00")
    units: List[ProductUnitCreate] = []

class ProductUpdate(BaseModel):
    name: Optional[str] = None
    category_id: Optional[str] = None
    default_sale_price: Optional[Decimal] = None
    default_cost_price: Optional[Decimal] = None
    min_stock_alert: Optional[Decimal] = None
    is_active: Optional[bool] = None
