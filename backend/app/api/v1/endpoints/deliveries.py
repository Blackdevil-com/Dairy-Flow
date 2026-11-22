from typing import List, Optional
from datetime import date
from decimal import Decimal
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, desc
from sqlalchemy.orm import selectinload

from app.db.session import get_db
from app.models.sales import Delivery, DeliveryItem, Invoice
from app.models.partner import Customer
from app.models.business import Warehouse
from app.models.product import Product
from app.models.user import User
from app.schemas.delivery import (
    DeliveryCreate, DeliveryResponse, DeliveryItemResponse, DeliveryConfirmRequest
)
from app.services.delivery_service import create_delivery_draft, confirm_delivery_transaction
from app.api.deps import get_current_user, require_roles

router = APIRouter()

@router.get("", response_model=List[DeliveryResponse])
async def list_deliveries(
    customer_id: Optional[str] = None,
    delivery_status: Optional[str] = None,
    delivery_date: Optional[date] = None,
    limit: int = 50,
    db: AsyncSession = Depends(get_db)
):
    stmt = (
        select(Delivery, Customer.name.label("customer_name"), Invoice.id.label("inv_id"), Invoice.invoice_no.label("inv_no"))
        .join(Customer, Delivery.customer_id == Customer.id)
        .outerjoin(Invoice, Delivery.id == Invoice.delivery_id)
        .options(selectinload(Delivery.items))
        .order_by(desc(Delivery.created_at))
        .limit(limit)
    )
    if customer_id:
        stmt = stmt.where(Delivery.customer_id == customer_id)
    if delivery_status:
        stmt = stmt.where(Delivery.status == delivery_status)
    if delivery_date:
        stmt = stmt.where(Delivery.delivery_date == delivery_date)
        
    res = await db.execute(stmt)
    rows = res.all()
    
    output = []
    for d, c_name, inv_id, inv_no in rows:
        # Load item details
        item_responses = []
        for it in d.items:
            p = await db.get(Product, it.product_id)
            item_responses.append(DeliveryItemResponse(
                id=it.id,
                product_id=it.product_id,
                product_name=p.name if p else None,
                unit_code=it.unit_code,
                requested_qty=it.requested_qty,
                conversion_factor=it.conversion_factor,
                base_qty=it.base_qty,
                unit_price=it.unit_price,
                tax_rate=it.tax_rate,
                tax_amount=it.tax_amount,
                line_total=it.line_total
            ))
            
        output.append(DeliveryResponse(
            id=d.id,
            delivery_no=d.delivery_no,
            customer_id=d.customer_id,
            customer_name=c_name,
            warehouse_id=d.warehouse_id,
            status=d.status,
            delivery_date=d.delivery_date,
            total_base_qty=d.total_base_qty,
            subtotal=d.subtotal,
            tax_amount=d.tax_amount,
            discount=d.discount,
            net_amount=d.net_amount,
            delivery_address=d.delivery_address,
            notes=d.notes,
            idempotency_key=d.idempotency_key,
            created_by=d.created_by,
            confirmed_by=d.confirmed_by,
            confirmed_at=d.confirmed_at,
            invoice_id=inv_id,
            invoice_no=inv_no,
            items=item_responses,
            created_at=d.created_at
        ))
    return output


@router.post("", response_model=DeliveryResponse)
async def create_delivery(
    deliv_in: DeliveryCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    """Owner creates delivery draft."""
    warehouse_id = deliv_in.warehouse_id
    if not warehouse_id:
        w_stmt = select(Warehouse).where(Warehouse.is_primary == True).limit(1)
        w_res = await db.execute(w_stmt)
        wh = w_res.scalar_one_or_none()
        warehouse_id = wh.id if wh else None
        if not warehouse_id:
            raise HTTPException(status_code=400, detail="Primary warehouse not configured")
            
    items_dicts = [item.model_dump() for item in deliv_in.items]
    
    delivery = await create_delivery_draft(
        db=db,
        customer_id=deliv_in.customer_id,
        warehouse_id=warehouse_id,
        items_data=items_dicts,
        created_by=user.id,
        delivery_date=deliv_in.delivery_date,
        discount=deliv_in.discount,
        delivery_address=deliv_in.delivery_address,
        notes=deliv_in.notes,
        idempotency_key=deliv_in.idempotency_key
    )
    await db.commit()
    
    # Reload for response
    cust = await db.get(Customer, delivery.customer_id)
    items_stmt = select(DeliveryItem).where(DeliveryItem.delivery_id == delivery.id)
    i_res = await db.execute(items_stmt)
    d_items = i_res.scalars().all()
    
    item_responses = []
    for it in d_items:
        p = await db.get(Product, it.product_id)
        item_responses.append(DeliveryItemResponse(
            id=it.id,
            product_id=it.product_id,
            product_name=p.name if p else None,
            unit_code=it.unit_code,
            requested_qty=it.requested_qty,
            conversion_factor=it.conversion_factor,
            base_qty=it.base_qty,
            unit_price=it.unit_price,
            tax_rate=it.tax_rate,
            tax_amount=it.tax_amount,
            line_total=it.line_total
        ))
        
    return DeliveryResponse(
        id=delivery.id,
        delivery_no=delivery.delivery_no,
        customer_id=delivery.customer_id,
        customer_name=cust.name if cust else None,
        warehouse_id=delivery.warehouse_id,
        status=delivery.status,
        delivery_date=delivery.delivery_date,
        total_base_qty=delivery.total_base_qty,
        subtotal=delivery.subtotal,
        tax_amount=delivery.tax_amount,
        discount=delivery.discount,
        net_amount=delivery.net_amount,
        delivery_address=delivery.delivery_address,
        notes=delivery.notes,
        idempotency_key=delivery.idempotency_key,
        created_by=delivery.created_by,
        items=item_responses,
        created_at=delivery.created_at
    )


@router.post("/{id}/confirm", response_model=DeliveryResponse)
async def confirm_delivery(
    id: str,
    req: DeliveryConfirmRequest,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    """
    Owner confirms delivery:
    FEFO batch allocation, signed stock deduction (- quantity),
    invoice creation snapshot, initial payment recording, customer balance update.
    """
    delivery, invoice = await confirm_delivery_transaction(
        db=db,
        delivery_id=id,
        confirmed_by=user.id,
        idempotency_key=req.idempotency_key,
        initial_payment_amount=req.initial_payment_amount,
        payment_mode=req.payment_mode or "CASH",
        payment_ref=req.payment_ref
    )
    await db.commit()
    
    # Reload for response
    cust = await db.get(Customer, delivery.customer_id)
    items_stmt = select(DeliveryItem).where(DeliveryItem.delivery_id == delivery.id)
    i_res = await db.execute(items_stmt)
    d_items = i_res.scalars().all()
    
    item_responses = []
    for it in d_items:
        p = await db.get(Product, it.product_id)
        item_responses.append(DeliveryItemResponse(
            id=it.id,
            product_id=it.product_id,
            product_name=p.name if p else None,
            unit_code=it.unit_code,
            requested_qty=it.requested_qty,
            conversion_factor=it.conversion_factor,
            base_qty=it.base_qty,
            unit_price=it.unit_price,
            tax_rate=it.tax_rate,
            tax_amount=it.tax_amount,
            line_total=it.line_total
        ))
        
    return DeliveryResponse(
        id=delivery.id,
        delivery_no=delivery.delivery_no,
        customer_id=delivery.customer_id,
        customer_name=cust.name if cust else None,
        warehouse_id=delivery.warehouse_id,
        status=delivery.status,
        delivery_date=delivery.delivery_date,
        total_base_qty=delivery.total_base_qty,
        subtotal=delivery.subtotal,
        tax_amount=delivery.tax_amount,
        discount=delivery.discount,
        net_amount=delivery.net_amount,
        delivery_address=delivery.delivery_address,
        notes=delivery.notes,
        idempotency_key=delivery.idempotency_key,
        created_by=delivery.created_by,
        confirmed_by=delivery.confirmed_by,
        confirmed_at=delivery.confirmed_at,
        invoice_id=invoice.id if invoice else None,
        invoice_no=invoice.invoice_no if invoice else None,
        items=item_responses,
        created_at=delivery.created_at
    )
