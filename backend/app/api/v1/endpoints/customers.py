import re
from typing import List, Optional
from decimal import Decimal
from fastapi import APIRouter, Depends, HTTPException, status, Query
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

PHONE_REGEX = re.compile(r"^[+]?[0-9\s-]{7,16}$")
EMAIL_REGEX = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")
GSTIN_REGEX = re.compile(r"^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")

def validate_customer_data(
    name: str,
    phone: str,
    email: Optional[str],
    gst_number: Optional[str],
    credit_enabled: bool,
    credit_limit: Decimal
):
    trimmed_name = name.strip() if name else ""
    if not trimmed_name or len(trimmed_name) < 2:
        raise HTTPException(status_code=400, detail="Customer or Shop Name must be at least 2 characters.")

    trimmed_phone = phone.strip() if phone else ""
    if not trimmed_phone or not PHONE_REGEX.match(trimmed_phone):
        raise HTTPException(status_code=400, detail="Please enter a valid phone number (7-15 digits).")

    if email and email.strip():
        if not EMAIL_REGEX.match(email.strip()):
            raise HTTPException(status_code=400, detail="Please enter a valid email address.")

    if gst_number and gst_number.strip():
        cleaned_gst = gst_number.strip().upper()
        if not GSTIN_REGEX.match(cleaned_gst):
            raise HTTPException(
                status_code=400,
                detail="Invalid GSTIN format. Indian GSTIN must be 15 alphanumeric characters (e.g., 27ABCDE1234F1Z5)."
            )

    if credit_limit < Decimal("0.00"):
        raise HTTPException(status_code=400, detail="Credit limit cannot be negative.")


# --- Customers ---
@router.get("", response_model=List[CustomerResponse])
async def list_customers(
    customer_type: Optional[str] = None,
    has_outstanding: Optional[bool] = None,
    is_active: Optional[bool] = True,
    query: Optional[str] = None,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=500),
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    stmt = select(Customer)
    if is_active is not None:
        stmt = stmt.where(Customer.is_active == is_active)
    if customer_type:
        stmt = stmt.where(Customer.customer_type == customer_type)
    if has_outstanding is True:
        stmt = stmt.where(Customer.current_balance > Decimal("0.00"))
    if query and query.strip():
        q = f"%{query.strip()}%"
        stmt = stmt.where(
            or_(
                Customer.name.ilike(q),
                Customer.trade_name.ilike(q),
                Customer.phone.ilike(q),
                Customer.contact_person.ilike(q),
                Customer.area.ilike(q),
                Customer.city.ilike(q)
            )
        )
    stmt = stmt.order_by(Customer.name).offset(skip).limit(limit)
    res = await db.execute(stmt)
    return res.scalars().all()


@router.post("", response_model=CustomerResponse)
async def create_customer(
    cust_in: CustomerCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    validate_customer_data(
        name=cust_in.name,
        phone=cust_in.phone,
        email=cust_in.email,
        gst_number=cust_in.gst_number,
        credit_enabled=cust_in.credit_enabled,
        credit_limit=cust_in.credit_limit
    )

    clean_name = cust_in.name.strip()
    clean_phone = cust_in.phone.strip()

    # Duplicate check: same name and phone
    dup_stmt = select(Customer).where(
        and_(
            Customer.phone == clean_phone,
            Customer.name.ilike(clean_name),
            Customer.is_active == True
        )
    )
    dup_res = await db.execute(dup_stmt)
    if dup_res.scalar_one_or_none():
        raise HTTPException(
            status_code=400,
            detail=f"An active customer '{clean_name}' with phone {clean_phone} already exists."
        )

    # GSTIN uniqueness check if provided
    clean_gst = cust_in.gst_number.strip().upper() if cust_in.gst_number and cust_in.gst_number.strip() else None
    if clean_gst:
        gst_stmt = select(Customer).where(
            and_(Customer.gst_number == clean_gst, Customer.is_active == True)
        )
        gst_res = await db.execute(gst_stmt)
        if gst_res.scalar_one_or_none():
            raise HTTPException(
                status_code=400,
                detail=f"Another active customer is already registered with GSTIN {clean_gst}."
            )

    # Address synchronization
    effective_address = cust_in.address.strip() if cust_in.address and cust_in.address.strip() else None
    effective_billing = cust_in.billing_address.strip() if cust_in.billing_address and cust_in.billing_address.strip() else None
    effective_delivery = cust_in.delivery_address.strip() if cust_in.delivery_address and cust_in.delivery_address.strip() else None

    if not effective_address:
        effective_address = effective_delivery or effective_billing
    if not effective_delivery:
        effective_delivery = effective_address

    effective_credit_limit = cust_in.credit_limit if cust_in.credit_enabled else Decimal("0.00")

    cust = Customer(
        name=clean_name,
        trade_name=cust_in.trade_name.strip() if cust_in.trade_name and cust_in.trade_name.strip() else clean_name,
        customer_type=cust_in.customer_type,
        contact_person=cust_in.contact_person.strip() if cust_in.contact_person and cust_in.contact_person.strip() else None,
        phone=clean_phone,
        alternate_phone=cust_in.alternate_phone.strip() if cust_in.alternate_phone and cust_in.alternate_phone.strip() else None,
        email=cust_in.email.strip() if cust_in.email and cust_in.email.strip() else None,
        address=effective_address,
        billing_address=effective_billing or effective_address,
        delivery_address=effective_delivery or effective_address,
        area=cust_in.area.strip() if cust_in.area and cust_in.area.strip() else None,
        city=cust_in.city.strip() if cust_in.city and cust_in.city.strip() else None,
        district=cust_in.district.strip() if cust_in.district and cust_in.district.strip() else None,
        state_code=cust_in.state_code.strip() if cust_in.state_code and cust_in.state_code.strip() else None,
        postal_code=cust_in.postal_code.strip() if cust_in.postal_code and cust_in.postal_code.strip() else None,
        gst_registration_status=cust_in.gst_registration_status,
        gst_number=clean_gst,
        credit_enabled=cust_in.credit_enabled,
        credit_limit=effective_credit_limit,
        current_balance=Decimal("0.00"),
        payment_terms_days=cust_in.payment_terms_days,
        notes=cust_in.notes.strip() if cust_in.notes and cust_in.notes.strip() else None,
        is_active=True
    )
    db.add(cust)
    await db.commit()
    await db.refresh(cust)
    return cust


@router.get("/{id}", response_model=CustomerResponse)
async def get_customer(
    id: str,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
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

    dumped = cust_in.model_dump(exclude_unset=True)

    # If updating name or phone, validate
    new_name = dumped.get("name", cust.name)
    new_phone = dumped.get("phone", cust.phone)
    new_email = dumped.get("email", cust.email)
    new_gst = dumped.get("gst_number", cust.gst_number)
    new_credit_enabled = dumped.get("credit_enabled", cust.credit_enabled)
    new_credit_limit = dumped.get("credit_limit", cust.credit_limit)

    validate_customer_data(
        name=new_name,
        phone=new_phone,
        email=new_email,
        gst_number=new_gst,
        credit_enabled=new_credit_enabled,
        credit_limit=new_credit_limit
    )

    # Check GSTIN uniqueness if changing
    if "gst_number" in dumped and dumped["gst_number"]:
        clean_gst = dumped["gst_number"].strip().upper()
        dumped["gst_number"] = clean_gst
        gst_stmt = select(Customer).where(
            and_(Customer.gst_number == clean_gst, Customer.id != id, Customer.is_active == True)
        )
        gst_res = await db.execute(gst_stmt)
        if gst_res.scalar_one_or_none():
            raise HTTPException(
                status_code=400,
                detail=f"Another active customer is already registered with GSTIN {clean_gst}."
            )

    # Credit limit rule: if credit not enabled, limit must be 0
    if not new_credit_enabled:
        dumped["credit_limit"] = Decimal("0.00")

    for field, val in dumped.items():
        if isinstance(val, str):
            val = val.strip()
        setattr(cust, field, val)

    await db.commit()
    await db.refresh(cust)
    return cust


@router.delete("/{id}")
async def deactivate_customer(
    id: str,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    """Safely deactivates customer instead of hard-deleting."""
    cust = await db.get(Customer, id)
    if not cust:
        raise HTTPException(status_code=404, detail="Customer not found")

    cust.is_active = False
    await db.commit()
    return {"status": "deactivated", "customer_id": id, "name": cust.name}


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
