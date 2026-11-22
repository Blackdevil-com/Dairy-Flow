from typing import List, Optional
from datetime import date
from decimal import Decimal
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func, desc

from app.db.session import get_db
from app.models.stock import StockBatch, StockMovement
from app.models.product import Product
from app.models.business import Warehouse
from app.models.user import User
from app.schemas.stock import (
    StockBatchResponse, StockMovementResponse,
    StockAdjustmentCreate, DamagedExpiredCreate, StockSummaryItem
)
from app.services.stock_service import convert_to_base, record_stock_movement
from app.api.deps import get_current_user, require_roles

router = APIRouter()

@router.get("/summary", response_model=List[StockSummaryItem])
async def get_inventory_summary(db: AsyncSession = Depends(get_db)):
    """
    Returns inventory overview: product canonical unit, current stock, alerts and nearest expiry.
    """
    products_stmt = select(Product).where(Product.is_active == True).order_by(Product.name)
    p_res = await db.execute(products_stmt)
    products = p_res.scalars().all()
    
    summary = []
    today = date.today()
    
    for p in products:
        # Sum available stock from active batches
        b_stmt = select(
            func.coalesce(func.sum(StockBatch.available_base_qty), Decimal("0.0000")),
            func.count(StockBatch.id),
            func.min(StockBatch.expiry_date)
        ).where(
            and_(
                StockBatch.product_id == p.id,
                StockBatch.status == "ACTIVE",
                StockBatch.available_base_qty > 0
            )
        )
        b_res = await db.execute(b_stmt)
        avail, batch_count, nearest_exp = b_res.one()
        
        summary.append(StockSummaryItem(
            product_id=p.id,
            product_sku=p.sku,
            product_name=p.name,
            canonical_unit=p.canonical_unit_code,
            available_stock=avail or Decimal("0.0000"),
            min_stock_alert=p.min_stock_alert,
            is_low_stock=(avail or Decimal("0.0000")) <= p.min_stock_alert,
            active_batches_count=batch_count,
            nearest_expiry=nearest_exp
        ))
        
    return summary


@router.get("/stock", response_model=List[StockBatchResponse])
async def list_stock_batches(
    product_id: Optional[str] = None,
    warehouse_id: Optional[str] = None,
    only_active: bool = True,
    db: AsyncSession = Depends(get_db)
):
    stmt = (
        select(StockBatch, Product.name.label("product_name"))
        .join(Product, StockBatch.product_id == Product.id)
    )
    if product_id:
        stmt = stmt.where(StockBatch.product_id == product_id)
    if warehouse_id:
        stmt = stmt.where(StockBatch.warehouse_id == warehouse_id)
    if only_active:
        stmt = stmt.where(and_(StockBatch.status == "ACTIVE", StockBatch.available_base_qty > 0))
    stmt = stmt.order_by(StockBatch.expiry_date.asc())
    
    res = await db.execute(stmt)
    rows = res.all()
    
    today = date.today()
    results = []
    for batch, prod_name in rows:
        days_to_exp = (batch.expiry_date - today).days
        results.append(StockBatchResponse(
            id=batch.id,
            product_id=batch.product_id,
            product_name=prod_name,
            warehouse_id=batch.warehouse_id,
            batch_no=batch.batch_no,
            mfg_date=batch.mfg_date,
            expiry_date=batch.expiry_date,
            initial_base_qty=batch.initial_base_qty,
            available_base_qty=batch.available_base_qty,
            status=batch.status,
            days_to_expiry=days_to_exp
        ))
    return results


@router.get("/ledger", response_model=List[StockMovementResponse])
async def list_stock_ledger(
    product_id: Optional[str] = None,
    movement_type: Optional[str] = None,
    limit: int = 100,
    db: AsyncSession = Depends(get_db)
):
    """Immutable audit trail of signed stock movements."""
    stmt = (
        select(StockMovement, Product.name.label("product_name"), StockBatch.batch_no)
        .join(Product, StockMovement.product_id == Product.id)
        .outerjoin(StockBatch, StockMovement.batch_id == StockBatch.id)
    )
    if product_id:
        stmt = stmt.where(StockMovement.product_id == product_id)
    if movement_type:
        stmt = stmt.where(StockMovement.movement_type == movement_type)
        
    stmt = stmt.order_by(desc(StockMovement.created_at)).limit(limit)
    res = await db.execute(stmt)
    rows = res.all()
    
    return [
        StockMovementResponse(
            id=sm.id,
            movement_no=sm.movement_no,
            product_id=sm.product_id,
            product_name=p_name,
            batch_id=sm.batch_id,
            batch_no=b_no,
            warehouse_id=sm.warehouse_id,
            movement_type=sm.movement_type,
            signed_base_quantity=sm.signed_base_quantity,
            reference_type=sm.reference_type,
            reference_id=sm.reference_id,
            remarks=sm.remarks,
            created_by=sm.created_by,
            created_at=sm.created_at
        )
        for sm, p_name, b_no in rows
    ]


@router.post("/adjustments")
async def adjust_stock(
    adj_in: StockAdjustmentCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["INVENTORY_MANAGER", "OWNER"]))
):
    """Manual inventory adjustment (IN/OUT) with signed ledger update."""
    prod = await db.get(Product, adj_in.product_id)
    if not prod:
        raise HTTPException(status_code=404, detail="Product not found")
        
    # Get primary warehouse if not provided
    warehouse_id = adj_in.warehouse_id
    if not warehouse_id:
        w_stmt = select(Warehouse).where(Warehouse.is_primary == True).limit(1)
        w_res = await db.execute(w_stmt)
        wh = w_res.scalar_one_or_none()
        warehouse_id = wh.id if wh else None
        if not warehouse_id:
            raise HTTPException(status_code=400, detail="Primary warehouse not configured")
            
    base_qty, factor = await convert_to_base(db, adj_in.product_id, adj_in.quantity, adj_in.unit_code)
    
    if adj_in.adjustment_type == "IN":
        signed_qty = base_qty
        mtype = "ADJUSTMENT_IN"
        if adj_in.batch_id:
            batch = await db.get(StockBatch, adj_in.batch_id)
            if batch:
                batch.available_base_qty += base_qty
                if batch.status == "DEPLETED":
                    batch.status = "ACTIVE"
    elif adj_in.adjustment_type == "OUT":
        signed_qty = -base_qty
        mtype = "ADJUSTMENT_OUT"
        if adj_in.batch_id:
            batch = await db.get(StockBatch, adj_in.batch_id)
            if batch:
                if batch.available_base_qty < base_qty:
                    raise HTTPException(status_code=400, detail="Batch does not have enough stock to adjust out")
                batch.available_base_qty -= base_qty
                if batch.available_base_qty == Decimal("0.0000"):
                    batch.status = "DEPLETED"
    else:
        raise HTTPException(status_code=400, detail="adjustment_type must be IN or OUT")
        
    movement = await record_stock_movement(
        db=db,
        product_id=adj_in.product_id,
        warehouse_id=warehouse_id,
        movement_type=mtype,
        signed_base_quantity=signed_qty,
        reference_type="ADJUSTMENT",
        reference_id=f"ADJ-{date.today().isoformat()}",
        created_by=user.id,
        batch_id=adj_in.batch_id,
        remarks=adj_in.reason
    )
    await db.commit()
    return {"message": "Stock adjusted successfully", "movement_no": movement.movement_no}


@router.post("/damaged-expired")
async def record_damaged_expired(
    writeoff_in: DamagedExpiredCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["INVENTORY_MANAGER", "OWNER"]))
):
    """Writes off damaged or expired stock with negative movement."""
    batch = await db.get(StockBatch, writeoff_in.batch_id)
    if not batch:
        raise HTTPException(status_code=404, detail="Batch not found")
        
    base_qty, factor = await convert_to_base(db, writeoff_in.product_id, writeoff_in.quantity, writeoff_in.unit_code)
    if batch.available_base_qty < base_qty:
        raise HTTPException(status_code=400, detail=f"Cannot write off {base_qty:.2f}: batch only has {batch.available_base_qty:.2f} available")
        
    batch.available_base_qty -= base_qty
    if batch.available_base_qty == Decimal("0.0000"):
        batch.status = "DEPLETED"
        
    movement = await record_stock_movement(
        db=db,
        product_id=writeoff_in.product_id,
        warehouse_id=batch.warehouse_id,
        movement_type="DAMAGED_EXPIRED",
        signed_base_quantity=-base_qty,
        reference_type="WRITE_OFF",
        reference_id=batch.id,
        created_by=user.id,
        batch_id=batch.id,
        remarks=f"Write-off: {writeoff_in.reason}"
    )
    await db.commit()
    return {"message": "Damaged/expired stock recorded successfully", "movement_no": movement.movement_no}
