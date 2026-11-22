import uuid
from datetime import datetime, date
from decimal import Decimal
from typing import Optional
from fastapi import HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_

from app.models.sales import InvoiceSequence, Invoice, InvoiceItem, Delivery
from app.models.partner import Customer
from app.models.business import BusinessSettings
from app.models.product import Product, ProductTaxProfile
from app.core.config import settings

def get_financial_year(dt: Optional[date] = None) -> str:
    if dt is None:
        dt = date.today()
    year = dt.year
    if dt.month >= 4:
        return f"{year}-{str(year + 1)[-2:]}"
    else:
        return f"{year - 1}-{str(year)[-2:]}"


async def get_next_invoice_number(db: AsyncSession, series_prefix: str = "DF", dt: Optional[date] = None) -> tuple[str, str, int]:
    fy = get_financial_year(dt)
    
    # Lock row for monotonic sequence
    stmt = (
        select(InvoiceSequence)
        .where(
            and_(
                InvoiceSequence.financial_year == fy,
                InvoiceSequence.series_prefix == series_prefix
            )
        )
        .with_for_update()
    )
    result = await db.execute(stmt)
    seq_record = result.scalar_one_or_none()
    
    if not seq_record:
        seq_record = InvoiceSequence(
            financial_year=fy,
            series_prefix=series_prefix,
            current_sequence=1
        )
        db.add(seq_record)
        await db.flush()
        next_val = 1
    else:
        seq_record.current_sequence += 1
        next_val = seq_record.current_sequence
        await db.flush()
        
    formatted_no = f"{series_prefix}/{fy}/{str(next_val).zfill(4)}"
    return formatted_no, fy, next_val


async def create_invoice_from_delivery(
    db: AsyncSession,
    delivery: Delivery,
    created_by: str,
    doc_type: Optional[str] = None
) -> Invoice:
    customer = await db.get(Customer, delivery.customer_id)
    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")
        
    # Check Business Settings for GST Mode
    biz_stmt = select(BusinessSettings).limit(1)
    biz_res = await db.execute(biz_stmt)
    biz = biz_res.scalar_one_or_none()
    
    resolved_doc_type = doc_type or (biz.default_doc_type if biz else "COMMERCIAL_RECEIPT")
    is_demo_watermark = True if (not biz or biz.gst_status == "UNVERIFIED_DEMO") else False
    
    invoice_no, fy, seq_val = await get_next_invoice_number(db, series_prefix="DF", dt=delivery.delivery_date)
    
    invoice = Invoice(
        invoice_no=invoice_no,
        financial_year=fy,
        series_prefix="DF",
        sequence_number=seq_val,
        doc_type=resolved_doc_type,
        delivery_id=delivery.id,
        customer_id=customer.id,
        customer_name_snap=customer.trade_name or customer.name,
        customer_phone_snap=customer.phone,
        customer_address_snap=customer.address,
        customer_gst_snap=customer.gst_number,
        invoice_date=delivery.delivery_date or date.today(),
        subtotal=delivery.subtotal,
        total_tax=delivery.tax_amount,
        discount=delivery.discount,
        total_amount=delivery.net_amount,
        paid_amount=Decimal("0.00"),
        outstanding_amount=delivery.net_amount,
        payment_status="UNPAID",
        is_demo_watermark=is_demo_watermark,
        created_by=created_by
    )
    db.add(invoice)
    await db.flush()
    
    # Create invoice items snapshot
    for d_item in delivery.items:
        prod = await db.get(Product, d_item.product_id)
        prod_name = prod.name if prod else f"Product {d_item.product_id}"
        
        # Get tax profile
        tax_stmt = select(ProductTaxProfile).where(ProductTaxProfile.product_id == d_item.product_id).order_by(ProductTaxProfile.effective_from.desc()).limit(1)
        tax_res = await db.execute(tax_stmt)
        tax_prof = tax_res.scalar_one_or_none()
        
        hsn = tax_prof.hsn_code if tax_prof else "0401"
        cgst_rate = tax_prof.cgst_rate if tax_prof else Decimal("0.00")
        sgst_rate = tax_prof.sgst_rate if tax_prof else Decimal("0.00")
        igst_rate = tax_prof.igst_rate if tax_prof else Decimal("0.00")
        
        line_tax = d_item.tax_amount
        # Intrastate split vs Interstate
        is_interstate = (customer.state_code != (biz.state_code if biz else "27")) if customer.state_code else False
        if is_interstate:
            cgst_amt = Decimal("0.00")
            sgst_amt = Decimal("0.00")
            igst_amt = line_tax
        else:
            cgst_amt = (line_tax / Decimal("2.00")).quantize(Decimal("0.01"))
            sgst_amt = line_tax - cgst_amt
            igst_amt = Decimal("0.00")
            
        inv_item = InvoiceItem(
            invoice_id=invoice.id,
            product_id=d_item.product_id,
            product_name_snap=prod_name,
            hsn_snap=hsn,
            unit_code=d_item.unit_code,
            billed_qty=d_item.requested_qty,
            base_qty=d_item.base_qty,
            unit_price_snap=d_item.unit_price,
            tax_rate_snap=d_item.tax_rate,
            cgst_amount=cgst_amt,
            sgst_amount=sgst_amt,
            igst_amount=igst_amt,
            line_total=d_item.line_total
        )
        db.add(inv_item)
        
    await db.flush()
    return invoice
