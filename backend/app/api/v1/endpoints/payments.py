from typing import List, Optional
from decimal import Decimal
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, desc, func
from sqlalchemy.orm import selectinload

from app.db.session import get_db
from app.models.payment import Payment, PaymentAllocation
from app.models.sales import Invoice
from app.models.partner import Customer
from app.models.user import User
from app.schemas.payment import (
    PaymentCreate, PaymentResponse, PaymentAllocationResponse, OutstandingCustomerSummary
)
from app.services.payment_service import record_payment_internal
from app.api.deps import get_current_user, require_roles

router = APIRouter()

@router.get("", response_model=List[PaymentResponse])
async def list_payments(
    customer_id: Optional[str] = None,
    limit: int = 50,
    db: AsyncSession = Depends(get_db)
):
    stmt = (
        select(Payment, Customer.name.label("customer_name"))
        .join(Customer, Payment.customer_id == Customer.id)
        .options(selectinload(Payment.allocations))
        .order_by(desc(Payment.created_at))
        .limit(limit)
    )
    if customer_id:
        stmt = stmt.where(Payment.customer_id == customer_id)
        
    res = await db.execute(stmt)
    rows = res.all()
    
    output = []
    for pay, c_name in rows:
        alloc_responses = []
        for a in pay.allocations:
            inv = await db.get(Invoice, a.invoice_id)
            alloc_responses.append(PaymentAllocationResponse(
                id=a.id,
                invoice_id=a.invoice_id,
                invoice_no=inv.invoice_no if inv else None,
                allocated_amount=a.allocated_amount,
                created_at=a.created_at
            ))
            
        output.append(PaymentResponse(
            id=pay.id,
            payment_no=pay.payment_no,
            customer_id=pay.customer_id,
            customer_name=c_name,
            payment_mode=pay.payment_mode,
            transaction_ref=pay.transaction_ref,
            amount=pay.amount,
            payment_date=pay.payment_date,
            notes=pay.notes,
            received_by=pay.received_by,
            allocations=alloc_responses,
            created_at=pay.created_at
        ))
    return output


@router.post("", response_model=PaymentResponse)
async def create_payment(
    payment_in: PaymentCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    """
    Owner records payment (CASH, UPI, BANK_TRANSFER, OTHER).
    Allocates to outstanding invoices and reduces customer current_balance.
    """
    payment = await record_payment_internal(
        db=db,
        customer_id=payment_in.customer_id,
        amount=payment_in.amount,
        payment_mode=payment_in.payment_mode,
        received_by=user.id,
        transaction_ref=payment_in.transaction_ref,
        payment_date=payment_in.payment_date,
        notes=payment_in.notes,
        specific_invoice_id=payment_in.specific_invoice_id
    )
    await db.commit()
    
    # Reload for response
    cust = await db.get(Customer, payment.customer_id)
    stmt = (
        select(Payment)
        .options(selectinload(Payment.allocations))
        .where(Payment.id == payment.id)
    )
    res = await db.execute(stmt)
    full_pay = res.scalar_one()
    
    alloc_responses = []
    for a in full_pay.allocations:
        inv = await db.get(Invoice, a.invoice_id)
        alloc_responses.append(PaymentAllocationResponse(
            id=a.id,
            invoice_id=a.invoice_id,
            invoice_no=inv.invoice_no if inv else None,
            allocated_amount=a.allocated_amount,
            created_at=a.created_at
        ))
        
    return PaymentResponse(
        id=full_pay.id,
        payment_no=full_pay.payment_no,
        customer_id=full_pay.customer_id,
        customer_name=cust.name if cust else None,
        payment_mode=full_pay.payment_mode,
        transaction_ref=full_pay.transaction_ref,
        amount=full_pay.amount,
        payment_date=full_pay.payment_date,
        notes=full_pay.notes,
        received_by=full_pay.received_by,
        allocations=alloc_responses,
        created_at=full_pay.created_at
    )


@router.get("/outstanding", response_model=List[OutstandingCustomerSummary])
async def list_outstanding_customers(db: AsyncSession = Depends(get_db)):
    """Lists customers with open balances and unpaid invoices."""
    stmt = (
        select(Customer)
        .where(and_(Customer.is_active == True, Customer.current_balance > Decimal("0.00")))
        .order_by(desc(Customer.current_balance))
    )
    res = await db.execute(stmt)
    customers = res.scalars().all()
    
    summary = []
    for c in customers:
        inv_stmt = select(
            func.count(Invoice.id),
            func.min(Invoice.invoice_date)
        ).where(
            and_(
                Invoice.customer_id == c.id,
                Invoice.payment_status.in_(["UNPAID", "PARTIAL"])
            )
        )
        inv_res = await db.execute(inv_stmt)
        unpaid_count, oldest_date = inv_res.one()
        
        summary.append(OutstandingCustomerSummary(
            customer_id=c.id,
            customer_name=c.trade_name or c.name,
            customer_type=c.customer_type,
            phone=c.phone,
            credit_limit=c.credit_limit,
            outstanding_balance=c.current_balance,
            unpaid_invoices_count=unpaid_count or 0,
            oldest_unpaid_invoice_date=oldest_date
        ))
    return summary
