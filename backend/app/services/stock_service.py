import uuid
from datetime import datetime, date
from decimal import Decimal
from typing import List, Tuple, Dict, Any, Optional
from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func, desc

from app.models.product import Product, ProductUnit
from app.models.stock import StockBatch, StockMovement
from app.models.sales import Delivery, DeliveryItem

async def convert_to_base(db: AsyncSession, product_id: str, quantity: Decimal, unit_code: str) -> Tuple[Decimal, Decimal]:
    """
    Converts a given quantity in unit_code to canonical base unit quantity.
    Returns (base_quantity, conversion_factor).
    Never globally assumed; strictly looked up per-product.
    """
    product = await db.get(Product, product_id)
    if not product:
        raise HTTPException(status_code=404, detail=f"Product with id {product_id} not found")
    
    # If the unit requested is already the canonical unit
    if unit_code == product.canonical_unit_code:
        return (quantity, Decimal("1.0000"))
    
    # Query product_units for product-specific conversion factor
    stmt = select(ProductUnit).where(
        and_(ProductUnit.product_id == product_id, ProductUnit.unit_code == unit_code)
    )
    result = await db.execute(stmt)
    prod_unit = result.scalar_one_or_none()
    
    if not prod_unit:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unit '{unit_code}' is not configured for product '{product.name}' (SKU: {product.sku}). Please configure unit conversion first."
        )
    
    conversion_factor = prod_unit.conversion_to_base
    base_qty = quantity * conversion_factor
    return (base_qty, conversion_factor)


async def get_or_create_batch(
    db: AsyncSession,
    product_id: str,
    warehouse_id: str,
    batch_no: str,
    mfg_date: date,
    expiry_date: date,
    initial_base_qty: Decimal
) -> StockBatch:
    stmt = select(StockBatch).where(
        and_(
            StockBatch.product_id == product_id,
            StockBatch.warehouse_id == warehouse_id,
            StockBatch.batch_no == batch_no
        )
    )
    result = await db.execute(stmt)
    batch = result.scalar_one_or_none()
    
    if batch:
        batch.available_base_qty += initial_base_qty
        batch.initial_base_qty += initial_base_qty
        if batch.status == "DEPLETED" and batch.available_base_qty > 0:
            batch.status = "ACTIVE"
        return batch
    
    new_batch = StockBatch(
        product_id=product_id,
        warehouse_id=warehouse_id,
        batch_no=batch_no,
        mfg_date=mfg_date,
        expiry_date=expiry_date,
        initial_base_qty=initial_base_qty,
        available_base_qty=initial_base_qty,
        status="ACTIVE"
    )
    db.add(new_batch)
    await db.flush()
    return new_batch


async def record_stock_movement(
    db: AsyncSession,
    product_id: str,
    warehouse_id: str,
    movement_type: str,
    signed_base_quantity: Decimal,
    reference_type: str,
    reference_id: str,
    created_by: str,
    batch_id: Optional[str] = None,
    remarks: Optional[str] = None
) -> StockMovement:
    movement_no = f"SM-{datetime.utcnow().strftime('%Y%m%d%H%M%S')}-{uuid.uuid4().hex[:6].upper()}"
    movement = StockMovement(
        movement_no=movement_no,
        product_id=product_id,
        batch_id=batch_id,
        warehouse_id=warehouse_id,
        movement_type=movement_type,
        signed_base_quantity=signed_base_quantity,
        reference_type=reference_type,
        reference_id=reference_id,
        remarks=remarks,
        created_by=created_by
    )
    db.add(movement)
    await db.flush()
    return movement


async def allocate_fefo_batches(
    db: AsyncSession,
    product_id: str,
    warehouse_id: str,
    required_base_qty: Decimal
) -> List[Tuple[StockBatch, Decimal]]:
    """
    Allocates stock using FEFO (First Expiry, First Out).
    Returns list of (batch, allocated_base_qty).
    Locks candidate batches with available_base_qty > 0.
    """
    stmt = (
        select(StockBatch)
        .where(
            and_(
                StockBatch.product_id == product_id,
                StockBatch.warehouse_id == warehouse_id,
                StockBatch.status == "ACTIVE",
                StockBatch.available_base_qty > 0
            )
        )
        .order_by(StockBatch.expiry_date.asc(), StockBatch.mfg_date.asc(), StockBatch.id.asc())
        .with_for_update()
    )
    result = await db.execute(stmt)
    batches = result.scalars().all()
    
    total_available = sum(b.available_base_qty for b in batches)
    if total_available < required_base_qty:
        product = await db.get(Product, product_id)
        prod_name = product.name if product else product_id
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Insufficient stock for '{prod_name}'. Required: {required_base_qty:.4f}, Available: {total_available:.4f} {product.canonical_unit_code if product else ''}"
        )
    
    allocations = []
    remaining = required_base_qty
    for batch in batches:
        if remaining <= 0:
            break
        alloc_qty = min(remaining, batch.available_base_qty)
        allocations.append((batch, alloc_qty))
        remaining -= alloc_qty
        
    return allocations
