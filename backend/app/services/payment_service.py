import uuid
from datetime import datetime, date
from decimal import Decimal
from typing import Optional, List
from fastapi import HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, desc

from app.models.payment import Payment, PaymentAllocation
from app.models.sales import Invoice
from app.models.partner import Customer

async def record_payment_internal(
    db: AsyncSession,
    customer_id: str,
    amount: Decimal,
    payment_mode: str,
    received_by: str,
    transaction_ref: Optional[str] = None,
    payment_date: Optional[date] = None,
    notes: Optional[str] = None,
    specific_invoice_id: Optional[str] = None
) -> Payment:
    customer = await db.get(Customer, customer_id)
    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")
        
    payment_no = f"PAY-{datetime.utcnow().strftime('%Y%m%d%H%M%S')}-{uuid.uuid4().hex[:4].upper()}"
    
    payment = Payment(
        payment_no=payment_no,
        customer_id=customer_id,
        payment_mode=payment_mode,
        transaction_ref=transaction_ref,
        amount=amount,
        payment_date=payment_date or date.today(),
        notes=notes,
        received_by=received_by
    )
    db.add(payment)
    await db.flush()
    
    # Deduct from customer's overall current outstanding balance
    customer.current_balance = max(Decimal("0.00"), customer.current_balance - amount)
    
    remaining = amount
    
    # If specific invoice requested, allocate first
    if specific_invoice_id:
        inv = await db.get(Invoice, specific_invoice_id)
        if inv and inv.outstanding_amount > Decimal("0.00"):
            alloc_amt = min(remaining, inv.outstanding_amount)
            inv.paid_amount += alloc_amt
            inv.outstanding_amount -= alloc_amt
            inv.payment_status = "PAID" if inv.outstanding_amount == Decimal("0.00") else "PARTIAL"
            
            allocation = PaymentAllocation(
                payment_id=payment.id,
                invoice_id=inv.id,
                allocated_amount=alloc_amt
            )
            db.add(allocation)
            remaining -= alloc_amt
            
    # Allocate remainder to open invoices chronologically (FIFO)
    if remaining > Decimal("0.00"):
        stmt = (
            select(Invoice)
            .where(
                and_(
                    Invoice.customer_id == customer_id,
                    Invoice.payment_status.in_(["UNPAID", "PARTIAL"])
                )
            )
            .order_by(Invoice.invoice_date.asc(), Invoice.sequence_number.asc())
        )
        res = await db.execute(stmt)
        open_invoices = res.scalars().all()
        
        for inv in open_invoices:
            if remaining <= Decimal("0.00"):
                break
            if specific_invoice_id and inv.id == specific_invoice_id:
                continue
                
            alloc_amt = min(remaining, inv.outstanding_amount)
            inv.paid_amount += alloc_amt
            inv.outstanding_amount -= alloc_amt
            inv.payment_status = "PAID" if inv.outstanding_amount == Decimal("0.00") else "PARTIAL"
            
            allocation = PaymentAllocation(
                payment_id=payment.id,
                invoice_id=inv.id,
                allocated_amount=alloc_amt
            )
            db.add(allocation)
            remaining -= alloc_amt

    await db.flush()
    return payment
