from typing import Optional, List
from datetime import datetime, date
from decimal import Decimal
from pydantic import BaseModel, Field

class PaymentAllocationCreate(BaseModel):
    invoice_id: str
    allocated_amount: Decimal = Field(..., gt=0)

class PaymentAllocationResponse(BaseModel):
    id: str
    invoice_id: str
    invoice_no: Optional[str] = None
    allocated_amount: Decimal
    created_at: datetime

    class Config:
        from_attributes = True

class PaymentCreate(BaseModel):
    customer_id: str
    payment_mode: str = "CASH"  # CASH, UPI, BANK_TRANSFER, OTHER
    transaction_ref: Optional[str] = None
    amount: Decimal = Field(..., gt=0)
    payment_date: Optional[date] = None
    notes: Optional[str] = None
    specific_invoice_id: Optional[str] = None  # If provided, allocate to this invoice first

class PaymentResponse(BaseModel):
    id: str
    payment_no: str
    customer_id: str
    customer_name: Optional[str] = None
    payment_mode: str
    transaction_ref: Optional[str] = None
    amount: Decimal
    payment_date: date
    notes: Optional[str] = None
    received_by: str
    allocations: List[PaymentAllocationResponse] = []
    created_at: datetime

    class Config:
        from_attributes = True

class OutstandingCustomerSummary(BaseModel):
    customer_id: str
    customer_name: str
    customer_type: str
    phone: str
    credit_limit: Decimal
    outstanding_balance: Decimal
    unpaid_invoices_count: int
    oldest_unpaid_invoice_date: Optional[date] = None
