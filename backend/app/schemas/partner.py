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
    customer_type: str  # RETAIL_SHOP, INDIVIDUAL, OTHER_BUSINESS
    contact_person: Optional[str] = None
    phone: str
    alternate_phone: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None
    billing_address: Optional[str] = None
    delivery_address: Optional[str] = None
    area: Optional[str] = None
    city: Optional[str] = None
    district: Optional[str] = None
    state_code: Optional[str] = None
    postal_code: Optional[str] = None
    gst_registration_status: str = "UNKNOWN"
    gst_number: Optional[str] = None
    credit_enabled: bool = False
    credit_limit: Decimal = Decimal("0.00")
    current_balance: Decimal = Decimal("0.00")
    payment_terms_days: int = 0
    notes: Optional[str] = None
    is_active: bool
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True

class CustomerCreate(BaseModel):
    name: str
    trade_name: Optional[str] = None
    customer_type: str = "RETAIL_SHOP"
    contact_person: Optional[str] = None
    phone: str
    alternate_phone: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None
    billing_address: Optional[str] = None
    delivery_address: Optional[str] = None
    area: Optional[str] = None
    city: Optional[str] = None
    district: Optional[str] = None
    state_code: Optional[str] = None
    postal_code: Optional[str] = None
    gst_registration_status: str = "UNKNOWN"
    gst_number: Optional[str] = None
    credit_enabled: bool = False
    credit_limit: Decimal = Decimal("0.00")
    payment_terms_days: int = 0
    notes: Optional[str] = None

class CustomerUpdate(BaseModel):
    name: Optional[str] = None
    trade_name: Optional[str] = None
    customer_type: Optional[str] = None
    contact_person: Optional[str] = None
    phone: Optional[str] = None
    alternate_phone: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None
    billing_address: Optional[str] = None
    delivery_address: Optional[str] = None
    area: Optional[str] = None
    city: Optional[str] = None
    district: Optional[str] = None
    state_code: Optional[str] = None
    postal_code: Optional[str] = None
    gst_registration_status: Optional[str] = None
    gst_number: Optional[str] = None
    credit_enabled: Optional[bool] = None
    credit_limit: Optional[Decimal] = None
    payment_terms_days: Optional[int] = None
    notes: Optional[str] = None
    is_active: Optional[bool] = None
