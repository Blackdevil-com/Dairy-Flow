import uuid
from datetime import datetime, date, timezone
from decimal import Decimal
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, desc
from sqlalchemy.orm import selectinload

from app.db.session import get_db
from app.models.stock import PurchaseReceipt, PurchaseReceiptItem, StockBatch
from app.models.partner import Supplier
from app.models.business import Warehouse
from app.models.user import User
from app.schemas.stock import (
    PurchaseReceiptCreate, PurchaseReceiptResponse, PurchaseReceiptItemResponse
)
from app.services.stock_service import convert_to_base, get_or_create_batch, record_stock_movement
from app.api.deps import get_current_user, require_roles

router = APIRouter()

@router.get("", response_model=List[PurchaseReceiptResponse])
async def list_purchases(
    supplier_id: Optional[str] = None,
    limit: int = 50,
    db: AsyncSession = Depends(get_db)
):
    stmt = (
        select(PurchaseReceipt, Supplier.name.label("supplier_name"))
        .join(Supplier, PurchaseReceipt.supplier_id == Supplier.id)
        .options(selectinload(PurchaseReceipt.items))
        .order_by(desc(PurchaseReceipt.created_at))
        .limit(limit)
    )
    if supplier_id:
        stmt = stmt.where(PurchaseReceipt.supplier_id == supplier_id)
        
    res = await db.execute(stmt)
    rows = res.all()
    
    output = []
    for pr, s_name in rows:
        output.append(PurchaseReceiptResponse(
            id=pr.id,
            receipt_no=pr.receipt_no,
            supplier_id=pr.supplier_id,
            supplier_name=s_name,
            warehouse_id=pr.warehouse_id,
            invoice_reference=pr.invoice_reference,
            receipt_date=pr.receipt_date,
            total_amount=pr.total_amount,
            status=pr.status,
            notes=pr.notes,
            items=[
                PurchaseReceiptItemResponse(
                    id=it.id,
                    product_id=it.product_id,
                    batch_no=it.batch_no,
                    quantity_received=it.quantity_received,
                    unit_code=it.unit_code,
                    quantity_base=it.quantity_base,
                    unit_cost=it.unit_cost,
                    total_cost=it.total_cost,
                    mfg_date=it.mfg_date,
                    expiry_date=it.expiry_date
                )
                for it in pr.items
            ],
            created_at=pr.created_at
        ))
    return output


@router.post("", response_model=PurchaseReceiptResponse)
@router.post("/", response_model=PurchaseReceiptResponse, include_in_schema=False)
async def create_purchase_receipt(
    purchase_in: PurchaseReceiptCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["INVENTORY_MANAGER", "OWNER"]))
):
    """
    Incoming stock reception workflow:
    1. Validates supplier (auto-resolves "default" or primary if omitted).
    2. Validates warehouse (auto-resolves primary).
    3. Converts incoming quantity to canonical base unit.
    4. Posts purchase receipt.
    5. Updates/creates stock batches.
    6. Records positive signed movement in stock movement ledger.
    """
    # 1. Resolve Supplier
    supplier = None
    if purchase_in.supplier_id and purchase_in.supplier_id not in ["default", "primary", ""]:
        supplier = await db.get(Supplier, purchase_in.supplier_id)
    if not supplier:
        s_stmt = select(Supplier).where(Supplier.is_active == True).order_by(Supplier.created_at.asc()).limit(1)
        s_res = await db.execute(s_stmt)
        supplier = s_res.scalar_one_or_none()
    if not supplier:
        supplier = Supplier(
            name="Sahyadri Farmers Milk Producers Co-op",
            contact_person="Consortium Coordinator",
            phone="+91 9422011223",
            address="Baramati Road, Maharashtra",
            is_active=True
        )
        db.add(supplier)
        await db.flush()
        
    # 2. Resolve Warehouse
    warehouse_id = purchase_in.warehouse_id
    if not warehouse_id or warehouse_id in ["default", "primary", ""]:
        w_stmt = select(Warehouse).where(Warehouse.is_primary == True).limit(1)
        w_res = await db.execute(w_stmt)
        wh = w_res.scalar_one_or_none()
        if not wh:
            w_fallback_stmt = select(Warehouse).where(Warehouse.is_active == True).limit(1)
            wh = (await db.execute(w_fallback_stmt)).scalar_one_or_none()
        warehouse_id = wh.id if wh else None
        if not warehouse_id:
            new_wh = Warehouse(
                code="WH-CENTRAL",
                name="Central Dairy Warehouse Depot",
                is_primary=True,
                is_active=True
            )
            db.add(new_wh)
            await db.flush()
            warehouse_id = new_wh.id
            
    receipt_no = f"REC-{datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S')}-{uuid.uuid4().hex[:4].upper()}"
    
    receipt = PurchaseReceipt(
        receipt_no=receipt_no,
        supplier_id=supplier.id,
        warehouse_id=warehouse_id,
        invoice_reference=purchase_in.invoice_reference,
        receipt_date=purchase_in.receipt_date or date.today(),
        notes=purchase_in.notes,
        created_by=user.id,
        status="RECEIVED"
    )
    db.add(receipt)
    await db.flush()
    
    total_amount = Decimal("0.00")
    response_items = []
    
    for item_data in purchase_in.items:
        base_qty, factor = await convert_to_base(
            db=db,
            product_id=item_data.product_id,
            quantity=item_data.quantity_received,
            unit_code=item_data.unit_code
        )
        
        line_cost = (item_data.quantity_received * item_data.unit_cost).quantize(Decimal("0.01"))
        total_amount += line_cost
        
        receipt_item = PurchaseReceiptItem(
            receipt_id=receipt.id,
            product_id=item_data.product_id,
            batch_no=item_data.batch_no,
            quantity_received=item_data.quantity_received,
            unit_code=item_data.unit_code,
            conversion_factor=factor,
            quantity_base=base_qty,
            unit_cost=item_data.unit_cost,
            total_cost=line_cost,
            mfg_date=item_data.mfg_date,
            expiry_date=item_data.expiry_date
        )
        db.add(receipt_item)
        await db.flush()
        
        # Create or update Batch
        batch = await get_or_create_batch(
            db=db,
            product_id=item_data.product_id,
            warehouse_id=warehouse_id,
            batch_no=item_data.batch_no,
            mfg_date=item_data.mfg_date,
            expiry_date=item_data.expiry_date,
            initial_base_qty=base_qty
        )
        
        # Record positive movement in ledger
        await record_stock_movement(
            db=db,
            product_id=item_data.product_id,
            warehouse_id=warehouse_id,
            movement_type="PURCHASE_RECEIPT",
            signed_base_quantity=base_qty,
            reference_type="PURCHASE_RECEIPT",
            reference_id=receipt.id,
            created_by=user.id,
            batch_id=batch.id,
            remarks=f"Received batch {item_data.batch_no} from supplier {supplier.name}"
        )
        
        response_items.append(PurchaseReceiptItemResponse(
            id=receipt_item.id,
            product_id=receipt_item.product_id,
            batch_no=receipt_item.batch_no,
            quantity_received=receipt_item.quantity_received,
            unit_code=receipt_item.unit_code,
            quantity_base=receipt_item.quantity_base,
            unit_cost=receipt_item.unit_cost,
            total_cost=receipt_item.total_cost,
            mfg_date=receipt_item.mfg_date,
            expiry_date=receipt_item.expiry_date
        ))
        
    receipt.total_amount = total_amount
    await db.commit()
    
    return PurchaseReceiptResponse(
        id=receipt.id,
        receipt_no=receipt.receipt_no,
        supplier_id=receipt.supplier_id,
        supplier_name=supplier.name,
        warehouse_id=receipt.warehouse_id,
        invoice_reference=receipt.invoice_reference,
        receipt_date=receipt.receipt_date,
        total_amount=receipt.total_amount,
        status=receipt.status,
        notes=receipt.notes,
        items=response_items,
        created_at=receipt.created_at
    )
