from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import BaseModel

class SyncDeviceRegister(BaseModel):
    device_id: str
    device_name: Optional[str] = None

class OutboxItem(BaseModel):
    operation_id: str
    entity_type: str  # "PURCHASE_RECEIPT", "DELIVERY_DRAFT", "CUSTOMER", "PAYMENT"
    action: str  # "CREATE", "UPDATE"
    payload: Dict[str, Any]
    client_timestamp: datetime

class SyncPushRequest(BaseModel):
    device_id: str
    items: List[OutboxItem]

class SyncPushResult(BaseModel):
    operation_id: str
    status: str  # "SUCCESS", "REJECTED_CONFLICT", "DUPLICATE_IGNORED"
    server_id: Optional[str] = None
    message: Optional[str] = None

class SyncPushResponse(BaseModel):
    processed_count: int
    results: List[SyncPushResult]
    server_time: datetime

class SyncPullResponse(BaseModel):
    server_time: datetime
    products: List[Dict[str, Any]] = []
    batches: List[Dict[str, Any]] = []
    customers: List[Dict[str, Any]] = []
    recent_deliveries: List[Dict[str, Any]] = []
    recent_invoices: List[Dict[str, Any]] = []
