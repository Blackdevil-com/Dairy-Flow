import uuid
from datetime import datetime
from decimal import Decimal
from typing import Dict, Any, List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, desc

from app.db.session import get_db
from app.models.business import SyncDevice, IdempotencyKey
from app.models.product import Product
from app.models.stock import StockBatch
from app.models.partner import Customer
from app.models.sales import Delivery, Invoice
from app.models.user import User
from app.schemas.sync import SyncPushRequest, SyncPushResponse, SyncPushResult, SyncPullResponse
from app.api.deps import get_current_user

router = APIRouter()

@router.get("/changes", response_model=SyncPullResponse)
async def pull_sync_changes(
    device_id: str,
    since: Optional[datetime] = None,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user)
):
    """Returns database snapshot for Room local caching and offline use."""
    now = datetime.utcnow()
    
    # Update device last_sync_at
    dev_stmt = select(SyncDevice).where(SyncDevice.device_id == device_id)
    dev_res = await db.execute(dev_stmt)
    dev = dev_res.scalar_one_or_none()
    if not dev:
        dev = SyncDevice(device_id=device_id, user_id=user.id, last_sync_at=now)
        db.add(dev)
    else:
        dev.last_sync_at = now
    await db.commit()
    
    # Pull Products
    p_stmt = select(Product).where(Product.is_active == True)
    p_res = await db.execute(p_stmt)
    products = [
        {
            "id": p.id,
            "sku": p.sku,
            "name": p.name,
            "canonical_unit_code": p.canonical_unit_code,
            "default_sale_price": float(p.default_sale_price),
            "default_cost_price": float(p.default_cost_price),
            "min_stock_alert": float(p.min_stock_alert),
        }
        for p in p_res.scalars().all()
    ]
    
    # Pull active Batches
    b_stmt = select(StockBatch).where(and_(StockBatch.status == "ACTIVE", StockBatch.available_base_qty > 0))
    b_res = await db.execute(b_stmt)
    batches = [
        {
            "id": b.id,
            "product_id": b.product_id,
            "batch_no": b.batch_no,
            "available_base_qty": float(b.available_base_qty),
            "expiry_date": b.expiry_date.isoformat(),
        }
        for b in b_res.scalars().all()
    ]
    
    # Pull Customers
    c_stmt = select(Customer).where(Customer.is_active == True)
    c_res = await db.execute(c_stmt)
    customers = [
        {
            "id": c.id,
            "name": c.name,
            "trade_name": c.trade_name,
            "customer_type": c.customer_type,
            "contact_person": c.contact_person,
            "phone": c.phone,
            "alternate_phone": c.alternate_phone,
            "email": c.email,
            "address": c.address,
            "billing_address": c.billing_address,
            "delivery_address": c.delivery_address,
            "area": c.area,
            "city": c.city,
            "district": c.district,
            "state_code": c.state_code,
            "postal_code": c.postal_code,
            "gst_registration_status": c.gst_registration_status,
            "gst_number": c.gst_number,
            "credit_enabled": c.credit_enabled,
            "credit_limit": float(c.credit_limit),
            "current_balance": float(c.current_balance),
            "payment_terms_days": c.payment_terms_days,
            "notes": c.notes,
            "is_active": c.is_active,
        }
        for c in c_res.scalars().all()
    ]
    
    return SyncPullResponse(
        server_time=now,
        products=products,
        batches=batches,
        customers=customers
    )


@router.post("/push", response_model=SyncPushResponse)
async def push_outbox(
    push_req: SyncPushRequest,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user)
):
    """
    Processes local offline outbox items pushed from Android WorkManager.
    Re-validates stock, ensures idempotency, and safely creates drafts or records.
    """
    results = []
    
    for item in push_req.items:
        op_id = item.operation_id
        
        # Idempotency check
        idemp_stmt = select(IdempotencyKey).where(IdempotencyKey.key == op_id)
        idemp_res = await db.execute(idemp_stmt)
        if idemp_res.scalar_one_or_none():
            results.append(SyncPushResult(
                operation_id=op_id,
                status="DUPLICATE_IGNORED",
                message="Operation was previously synced and processed."
            ))
            continue
            
        try:
            if item.entity_type == "DELIVERY_DRAFT":
                from app.services.delivery_service import create_delivery_draft
                payload = item.payload
                deliv = await create_delivery_draft(
                    db=db,
                    customer_id=payload["customer_id"],
                    warehouse_id=payload.get("warehouse_id", "primary"),
                    items_data=payload["items"],
                    created_by=user.id,
                    notes=payload.get("notes"),
                    idempotency_key=op_id
                )
                await db.commit()
                results.append(SyncPushResult(
                    operation_id=op_id,
                    status="SUCCESS",
                    server_id=deliv.id,
                    message="Draft delivery created on server"
                ))
            elif item.entity_type == "CUSTOMER":
                if user.role != "OWNER":
                    results.append(SyncPushResult(
                        operation_id=op_id,
                        status="REJECTED_CONFLICT",
                        message="Unauthorized: Only OWNER role can create or synchronize customers"
                    ))
                    continue

                payload = item.payload
                # Check duplicate by phone and name
                clean_name = payload["name"].strip()
                clean_phone = payload["phone"].strip()
                dup_stmt = select(Customer).where(
                    and_(Customer.name.ilike(clean_name), Customer.phone == clean_phone, Customer.is_active == True)
                )
                dup_res = await db.execute(dup_stmt)
                existing_cust = dup_res.scalar_one_or_none()
                if existing_cust:
                    # Idempotently return existing customer server_id
                    results.append(SyncPushResult(
                        operation_id=op_id,
                        status="SUCCESS",
                        server_id=existing_cust.id,
                        message="Existing customer record matched and synced"
                    ))
                    continue

                credit_enabled = bool(payload.get("credit_enabled", False))
                credit_limit = Decimal(str(payload.get("credit_limit", 0.0))) if credit_enabled else Decimal("0.00")
                addr = payload.get("address")
                deliv_addr = payload.get("delivery_address") or addr
                bill_addr = payload.get("billing_address") or addr

                cust = Customer(
                    name=clean_name,
                    trade_name=payload.get("trade_name") or clean_name,
                    customer_type=payload.get("customer_type", "RETAIL_SHOP"),
                    contact_person=payload.get("contact_person"),
                    phone=clean_phone,
                    alternate_phone=payload.get("alternate_phone"),
                    email=payload.get("email"),
                    address=deliv_addr or bill_addr,
                    billing_address=bill_addr,
                    delivery_address=deliv_addr,
                    area=payload.get("area"),
                    city=payload.get("city"),
                    district=payload.get("district"),
                    state_code=payload.get("state_code"),
                    postal_code=payload.get("postal_code"),
                    gst_registration_status=payload.get("gst_registration_status", "UNKNOWN"),
                    gst_number=payload.get("gst_number"),
                    credit_enabled=credit_enabled,
                    credit_limit=credit_limit,
                    current_balance=Decimal("0.00"),
                    payment_terms_days=int(payload.get("payment_terms_days", 0)),
                    notes=payload.get("notes"),
                    is_active=True
                )
                db.add(cust)
                # Record idempotency
                db.add(IdempotencyKey(
                    key=op_id,
                    user_id=user.id,
                    endpoint="/sync/push/customer",
                    response_code=200,
                    response_body=cust.name
                ))
                await db.commit()
                results.append(SyncPushResult(
                    operation_id=op_id,
                    status="SUCCESS",
                    server_id=cust.id,
                    message="Customer synced successfully"
                ))
            else:
                results.append(SyncPushResult(
                    operation_id=op_id,
                    status="REJECTED_CONFLICT",
                    message=f"Entity type {item.entity_type} not supported for offline direct push"
                ))
        except Exception as e:
            await db.rollback()
            results.append(SyncPushResult(
                operation_id=op_id,
                status="REJECTED_CONFLICT",
                message=str(e)
            ))
            
    return SyncPushResponse(
        processed_count=len(results),
        results=results,
        server_time=datetime.utcnow()
    )
