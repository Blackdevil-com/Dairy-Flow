from typing import List, Optional
from decimal import Decimal
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_

from app.db.session import get_db
from app.models.partner import Customer, Supplier
from app.models.user import User
from app.schemas.partner import (
    CustomerResponse, CustomerCreate, CustomerUpdate,
    SupplierResponse, SupplierCreate
)
from app.api.deps import get_current_user, require_roles

router = APIRouter()

# --- Customers ---
@router.get("", response_model=List[CustomerResponse])
async def list_customers(
    customer_type: Optional[str] = None,
    has_outstanding: Optional[bool] = None,
    query: Optional[str] = None,
    db: AsyncSession = Depends(get_db)
):
    stmt = select(Customer).where(Customer.is_active == True)
    if customer_type:
        stmt = stmt.where(Customer.customer_type == customer_type)
    if has_outstanding is True:
        stmt = stmt.where(Customer.current_balance > Decimal("0.00"))
    if query:
        stmt = stmt.where(
            or_(
                Customer.name.ilike(f"%{query}%"),
                Customer.phone.ilike(f"%{query}%"),
                Customer.trade_name.ilike(f"%{query}%")
            )
        )
    stmt = stmt.order_by(Customer.name)
    res = await db.execute(stmt)
    return res.scalars().all()


@router.post("", response_model=CustomerResponse)
async def create_customer(
    cust_in: CustomerCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    cust = Customer(
        name=cust_in.name,
        trade_name=cust_in.trade_name,
        customer_type=cust_in.customer_type,
        phone=cust_in.phone,
        email=cust_in.email,
        address=cust_in.address,
        gst_number=cust_in.gst_number,
        credit_limit=cust_in.credit_limit,
        current_balance=Decimal("0.00"),
        is_active=True
    )
    db.add(cust)
    await db.commit()
    await db.refresh(cust)
    return cust


@router.get("/{id}", response_model=CustomerResponse)
async def get_customer(id: str, db: AsyncSession = Depends(get_db)):
    cust = await db.get(Customer, id)
    if not cust:
        raise HTTPException(status_code=404, detail="Customer not found")
    return cust


@router.put("/{id}", response_model=CustomerResponse)
async def update_customer(
    id: str,
    cust_in: CustomerUpdate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    cust = await db.get(Customer, id)
    if not cust:
        raise HTTPException(status_code=404, detail="Customer not found")
        
    for field, val in cust_in.model_dump(exclude_unset=True).items():
        setattr(cust, field, val)
        
    await db.commit()
    await db.refresh(cust)
    return cust

# --- Suppliers ---
@router.get("/suppliers/list", response_model=List[SupplierResponse])
async def list_suppliers(db: AsyncSession = Depends(get_db)):
    stmt = select(Supplier).where(Supplier.is_active == True).order_by(Supplier.name)
    res = await db.execute(stmt)
    return res.scalars().all()

@router.post("/suppliers/create", response_model=SupplierResponse)
async def create_supplier(
    supp_in: SupplierCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["INVENTORY_MANAGER", "OWNER"]))
):
    supp = Supplier(
        name=supp_in.name,
        contact_person=supp_in.contact_person,
        phone=supp_in.phone,
        email=supp_in.email,
        address=supp_in.address,
        gst_number=supp_in.gst_number,
        is_active=True
    )
    db.add(supp)
    await db.commit()
    await db.refresh(supp)
    return supp
