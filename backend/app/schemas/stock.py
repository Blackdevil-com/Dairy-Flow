from typing import Optional, List
from datetime import datetime, date
from decimal import Decimal
from pydantic import BaseModel, Field

class PurchaseReceiptItemCreate(BaseModel):
    product_id: str
    batch_no: str
    quantity_received: Decimal = Field(..., gt=0)
    unit_code: str
    unit_cost: Decimal = Decimal("0.00")
    mfg_date: date
    expiry_date: date

class PurchaseReceiptCreate(BaseModel):
    supplier_id: Optional[str] = None
    warehouse_id: Optional[str] = None
    invoice_reference: Optional[str] = None
    receipt_date: Optional[date] = None
    notes: Optional[str] = None
    items: List[PurchaseReceiptItemCreate]

class PurchaseReceiptItemResponse(BaseModel):
    id: str
    product_id: str
    product_name: Optional[str] = None
    batch_no: str
    quantity_received: Decimal
    unit_code: str
    quantity_base: Decimal
    unit_cost: Decimal
    total_cost: Decimal
    mfg_date: date
    expiry_date: date

    class Config:
        from_attributes = True

class PurchaseReceiptResponse(BaseModel):
    id: str
    receipt_no: str
    supplier_id: str
    supplier_name: Optional[str] = None
    warehouse_id: str
    invoice_reference: Optional[str] = None
    receipt_date: date
    total_amount: Decimal
    status: str
    notes: Optional[str] = None
    items: List[PurchaseReceiptItemResponse] = []
    created_at: datetime

    class Config:
        from_attributes = True

class StockBatchResponse(BaseModel):
    id: str
    product_id: str
    product_name: Optional[str] = None
    warehouse_id: str
    batch_no: str
    mfg_date: date
    expiry_date: date
    initial_base_qty: Decimal
    available_base_qty: Decimal
    status: str
    days_to_expiry: int = 0

    class Config:
        from_attributes = True

class StockMovementResponse(BaseModel):
    id: str
    movement_no: str
    product_id: str
    product_name: Optional[str] = None
    batch_id: Optional[str] = None
    batch_no: Optional[str] = None
    warehouse_id: str
    movement_type: str
    signed_base_quantity: Decimal
    reference_type: str
    reference_id: str
    remarks: Optional[str] = None
    created_by: str
    created_at: datetime

    class Config:
        from_attributes = True

class StockAdjustmentCreate(BaseModel):
    product_id: str
    batch_id: Optional[str] = None
    warehouse_id: Optional[str] = None
    adjustment_type: str  # "IN" (increase) or "OUT" (decrease)
    quantity: Decimal = Field(..., gt=0)
    unit_code: str
    reason: str

class DamagedExpiredCreate(BaseModel):
    product_id: str
    batch_id: str
    quantity: Decimal = Field(..., gt=0)
    unit_code: str
    reason: str  # DAMAGED, EXPIRED, LEAKAGE, SPOILAGE

class StockSummaryItem(BaseModel):
    product_id: str
    product_sku: str
    product_name: str
    canonical_unit: str
    available_stock: Decimal
    min_stock_alert: Decimal
    is_low_stock: bool
    active_batches_count: int
    nearest_expiry: Optional[date] = None
