import uuid
from datetime import datetime, date
from decimal import Decimal
from typing import Optional, List, Dict, Any
from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_

from sqlalchemy.orm import selectinload
from app.models.sales import Delivery, DeliveryItem, Invoice
from app.models.partner import Customer
from app.models.product import Product, ProductTaxProfile
from app.models.business import Warehouse, IdempotencyKey
from app.models.stock import StockBatch
from app.services.stock_service import convert_to_base, allocate_fefo_batches, record_stock_movement
from app.services.invoice_service import create_invoice_from_delivery
from app.services.pdf_service import generate_invoice_pdf
from app.models.business import BusinessSettings

async def create_delivery_draft(
    db: AsyncSession,
    customer_id: str,
    warehouse_id: str,
    items_data: List[Dict[str, Any]],
    created_by: str,
    delivery_date: Optional[date] = None,
    delivery_address: Optional[str] = None,
    discount: Decimal = Decimal("0.00"),
    notes: Optional[str] = None,
    idempotency_key: Optional[str] = None
) -> Delivery:
    # Verify Customer
    customer = await db.get(Customer, customer_id)
    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")
        
    # Verify Warehouse
    warehouse = await db.get(Warehouse, warehouse_id)
    if not warehouse:
        raise HTTPException(status_code=404, detail="Warehouse not found")
        
    delivery_no = f"DEL-{datetime.utcnow().strftime('%Y%m%d%H%M%S')}-{uuid.uuid4().hex[:4].upper()}"
    
    # Snapshot delivery address from parameter or customer defaults
    resolved_address = delivery_address or customer.delivery_address or customer.address

    delivery = Delivery(
        delivery_no=delivery_no,
        customer_id=customer_id,
        warehouse_id=warehouse_id,
        status="DRAFT",
        delivery_date=delivery_date or date.today(),
        delivery_address=resolved_address,
        discount=discount,
        notes=notes,
        idempotency_key=idempotency_key,
        created_by=created_by
    )
    db.add(delivery)
    await db.flush()
    
    total_base_qty = Decimal("0.0000")
    subtotal = Decimal("0.00")
    total_tax = Decimal("0.00")
    
    for item_in in items_data:
        product_id = item_in["product_id"]
        unit_code = item_in["unit_code"]
        requested_qty = Decimal(str(item_in["requested_qty"]))
        
        prod = await db.get(Product, product_id)
        if not prod:
            raise HTTPException(status_code=404, detail=f"Product {product_id} not found")
            
        base_qty, factor = await convert_to_base(db, product_id, requested_qty, unit_code)
        
        # Unit price: passed or product default
        unit_price = Decimal(str(item_in.get("unit_price") or prod.default_sale_price))
        item_subtotal = (requested_qty * unit_price).quantize(Decimal("0.01"))
        
        # Get tax profile
        tax_stmt = select(ProductTaxProfile).where(ProductTaxProfile.product_id == product_id).order_by(ProductTaxProfile.effective_from.desc()).limit(1)
        tax_res = await db.execute(tax_stmt)
        tax_prof = tax_res.scalar_one_or_none()
        tax_rate = (tax_prof.cgst_rate + tax_prof.sgst_rate + tax_prof.igst_rate) if tax_prof else Decimal("0.00")
        
        line_tax = (item_subtotal * (tax_rate / Decimal("100.00"))).quantize(Decimal("0.01"))
        line_total = item_subtotal + line_tax
        
        delivery_item = DeliveryItem(
            delivery_id=delivery.id,
            product_id=product_id,
            unit_code=unit_code,
            requested_qty=requested_qty,
            conversion_factor=factor,
            base_qty=base_qty,
            unit_price=unit_price,
            tax_rate=tax_rate,
            tax_amount=line_tax,
            line_total=line_total
        )
        db.add(delivery_item)
        
        total_base_qty += base_qty
        subtotal += item_subtotal
        total_tax += line_tax
        
    delivery.total_base_qty = total_base_qty
    delivery.subtotal = subtotal
    delivery.tax_amount = total_tax
    delivery.net_amount = (subtotal + total_tax - discount).quantize(Decimal("0.01"))
    
    await db.flush()
    return delivery


async def confirm_delivery_transaction(
    db: AsyncSession,
    delivery_id: str,
    confirmed_by: str,
    idempotency_key: Optional[str] = None,
    initial_payment_amount: Optional[Decimal] = None,
    payment_mode: Optional[str] = "CASH",
    payment_ref: Optional[str] = None
) -> tuple[Delivery, Invoice]:
    """
    Confirms delivery atomically:
    1. Check idempotency.
    2. Lock delivery record, verify DRAFT status.
    3. FEFO allocate and deduct stock batches.
    4. Write signed stock movement entries (- quantity).
    5. Generate Invoice and line items snapshot.
    6. Record initial payment if provided.
    7. Update customer outstanding balance.
    8. Mark delivery CONFIRMED.
    All enclosed in transactional consistency.
    """
    # 1. Idempotency verification
    if idempotency_key:
        idemp_stmt = select(IdempotencyKey).where(IdempotencyKey.key == idempotency_key)
        idemp_res = await db.execute(idemp_stmt)
        existing_key = idemp_res.scalar_one_or_none()
        if existing_key:
            # Already processed, return current delivery and invoice
            deliv = await db.get(Delivery, delivery_id)
            inv_stmt = select(Invoice).where(Invoice.delivery_id == delivery_id)
            inv_res = await db.execute(inv_stmt)
            inv = inv_res.scalar_one_or_none()
            return deliv, inv

    # 2. Lock Delivery record with items eagerly loaded
    stmt = (
        select(Delivery)
        .options(selectinload(Delivery.items))
        .where(Delivery.id == delivery_id)
        .with_for_update()
    )
    result = await db.execute(stmt)
    delivery = result.scalar_one_or_none()
    
    if not delivery:
        raise HTTPException(status_code=404, detail="Delivery not found")
        
    if delivery.status == "CONFIRMED":
        inv_stmt = select(Invoice).where(Invoice.delivery_id == delivery_id)
        inv_res = await db.execute(inv_stmt)
        inv = inv_res.scalar_one_or_none()
        return delivery, inv
        
    if delivery.status == "CANCELLED":
        raise HTTPException(status_code=400, detail="Cannot confirm a cancelled delivery")

    # Load delivery items
    items_stmt = select(DeliveryItem).where(DeliveryItem.delivery_id == delivery.id)
    items_res = await db.execute(items_stmt)
    delivery_items = items_res.scalars().all()

    # 3. FEFO Stock Allocation and batch deduction
    for item in delivery_items:
        allocations = await allocate_fefo_batches(
            db=db,
            product_id=item.product_id,
            warehouse_id=delivery.warehouse_id,
            required_base_qty=item.base_qty
        )
        
        for batch, alloc_qty in allocations:
            # Deduct from batch
            batch.available_base_qty -= alloc_qty
            if batch.available_base_qty == Decimal("0.0000"):
                batch.status = "DEPLETED"
            
            # Record negative stock movement
            await record_stock_movement(
                db=db,
                product_id=item.product_id,
                warehouse_id=delivery.warehouse_id,
                movement_type="DELIVERY_DISPATCH",
                signed_base_quantity=-alloc_qty,
                reference_type="DELIVERY",
                reference_id=delivery.id,
                created_by=confirmed_by,
                batch_id=batch.id,
                remarks=f"Dispatch for delivery {delivery.delivery_no} to customer {delivery.customer_id}"
            )

    # 4. Generate Invoice and line snapshots
    invoice = await create_invoice_from_delivery(db, delivery, created_by=confirmed_by)
    
    # 5. Customer balance update & Initial Payment
    customer = await db.get(Customer, delivery.customer_id)
    customer.current_balance += invoice.total_amount
    
    if initial_payment_amount and initial_payment_amount > Decimal("0.00"):
        from app.services.payment_service import record_payment_internal
        await record_payment_internal(
            db=db,
            customer_id=customer.id,
            amount=initial_payment_amount,
            payment_mode=payment_mode or "CASH",
            transaction_ref=payment_ref,
            received_by=confirmed_by,
            notes=f"Initial payment for delivery {delivery.delivery_no}",
            specific_invoice_id=invoice.id
        )

    # 6. Mark delivery CONFIRMED
    delivery.status = "CONFIRMED"
    delivery.confirmed_by = confirmed_by
    delivery.confirmed_at = datetime.utcnow()
    
    # Generate PDF Invoice asynchronously or prepare path
    pdf_filename = f"invoice_{invoice.invoice_no.replace('/', '_')}.pdf"
    pdf_dir = "static/invoices"
    pdf_path = f"{pdf_dir}/{pdf_filename}"
    invoice.pdf_path = pdf_path
    
    # Record Idempotency Key if passed
    if idempotency_key:
        idemp = IdempotencyKey(
            key=idempotency_key,
            user_id=confirmed_by,
            endpoint=f"/api/v1/deliveries/{delivery_id}/confirm",
            response_code=200,
            response_body=f"Confirmed delivery {delivery.delivery_no}"
        )
        db.add(idemp)

    await db.flush()
    return delivery, invoice
