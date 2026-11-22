from typing import Optional
from datetime import datetime
from decimal import Decimal
from pydantic import BaseModel

class SupplierResponse(BaseModel):
    id: str
    name: str
    contact_person: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None
    gst_number: Optional[str] = None
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True

class SupplierCreate(BaseModel):
    name: str
    contact_person: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None
    gst_number: Optional[str] = None

class CustomerResponse(BaseModel):
    id: str
    name: str
    trade_name: Optional[str] = None
    customer_type: str  # RETAIL_SHOP, INDIVIDUAL
    phone: str
    email: Optional[str] = None
    address: Optional[str] = None
    gst_number: Optional[str] = None
    credit_limit: Decimal
    current_balance: Decimal
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True

class CustomerCreate(BaseModel):
    name: str
    trade_name: Optional[str] = None
    customer_type: str = "RETAIL_SHOP"
    phone: str
    email: Optional[str] = None
    address: Optional[str] = None
    gst_number: Optional[str] = None
    credit_limit: Decimal = Decimal("50000.00")

class CustomerUpdate(BaseModel):
    name: Optional[str] = None
    trade_name: Optional[str] = None
    customer_type: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None
    gst_number: Optional[str] = None
    credit_limit: Optional[Decimal] = None
    is_active: Optional[bool] = None
