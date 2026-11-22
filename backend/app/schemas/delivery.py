from typing import Optional, List
from datetime import datetime, date
from decimal import Decimal
from pydantic import BaseModel, Field

class DeliveryItemCreate(BaseModel):
    product_id: str
    unit_code: str
    requested_qty: Decimal = Field(..., gt=0)
    unit_price: Optional[Decimal] = None  # If null, default price from product is used

class DeliveryCreate(BaseModel):
    customer_id: str
    warehouse_id: Optional[str] = None
    delivery_date: Optional[date] = None
    delivery_address: Optional[str] = None
    discount: Decimal = Decimal("0.00")
    notes: Optional[str] = None
    idempotency_key: Optional[str] = None
    items: List[DeliveryItemCreate]

class DeliveryItemResponse(BaseModel):
    id: str
    product_id: str
    product_name: Optional[str] = None
    unit_code: str
    requested_qty: Decimal
    conversion_factor: Decimal
    base_qty: Decimal
    unit_price: Decimal
    tax_rate: Decimal
    tax_amount: Decimal
    line_total: Decimal

    class Config:
        from_attributes = True

class DeliveryResponse(BaseModel):
    id: str
    delivery_no: str
    customer_id: str
    customer_name: Optional[str] = None
    warehouse_id: str
    status: str  # DRAFT, CONFIRMED, CANCELLED
    delivery_date: date
    total_base_qty: Decimal
    subtotal: Decimal
    tax_amount: Decimal
    discount: Decimal
    net_amount: Decimal
    delivery_address: Optional[str] = None
    notes: Optional[str] = None
    idempotency_key: Optional[str] = None
    created_by: str
    confirmed_by: Optional[str] = None
    confirmed_at: Optional[datetime] = None
    invoice_id: Optional[str] = None
    invoice_no: Optional[str] = None
    items: List[DeliveryItemResponse] = []
    created_at: datetime

    class Config:
        from_attributes = True

class DeliveryConfirmRequest(BaseModel):
    idempotency_key: Optional[str] = None
    initial_payment_amount: Optional[Decimal] = None
    payment_mode: Optional[str] = None  # CASH, UPI, BANK_TRANSFER
    payment_ref: Optional[str] = None
