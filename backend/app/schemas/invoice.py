from typing import Optional, List
from datetime import datetime, date
from decimal import Decimal
from pydantic import BaseModel

class InvoiceItemResponse(BaseModel):
    id: str
    product_id: str
    product_name_snap: str
    hsn_snap: str
    unit_code: str
    billed_qty: Decimal
    base_qty: Decimal
    unit_price_snap: Decimal
    tax_rate_snap: Decimal
    cgst_amount: Decimal
    sgst_amount: Decimal
    igst_amount: Decimal
    line_total: Decimal

    class Config:
        from_attributes = True

class InvoiceResponse(BaseModel):
    id: str
    invoice_no: str
    financial_year: str
    series_prefix: str
    sequence_number: int
    doc_type: str  # COMMERCIAL_RECEIPT, TAX_INVOICE, BILL_OF_SUPPLY, PROFORMA
    delivery_id: Optional[str] = None
    customer_id: str
    customer_name_snap: str
    customer_phone_snap: Optional[str] = None
    customer_address_snap: Optional[str] = None
    customer_gst_snap: Optional[str] = None
    invoice_date: date
    subtotal: Decimal
    total_tax: Decimal
    discount: Decimal
    total_amount: Decimal
    paid_amount: Decimal
    outstanding_amount: Decimal
    payment_status: str  # UNPAID, PARTIAL, PAID
    is_demo_watermark: bool
    pdf_path: Optional[str] = None
    items: List[InvoiceItemResponse] = []
    created_at: datetime

    class Config:
        from_attributes = True
