import os
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import FileResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, desc
from sqlalchemy.orm import selectinload

from app.db.session import get_db
from app.models.sales import Invoice, InvoiceItem
from app.models.business import BusinessSettings
from app.models.user import User
from app.schemas.invoice import InvoiceResponse, InvoiceItemResponse
from app.services.pdf_service import generate_invoice_pdf
from app.api.deps import get_current_user

router = APIRouter()

@router.get("", response_model=List[InvoiceResponse])
async def list_invoices(
    customer_id: Optional[str] = None,
    payment_status: Optional[str] = None,
    limit: int = 50,
    db: AsyncSession = Depends(get_db)
):
    stmt = (
        select(Invoice)
        .options(selectinload(Invoice.items))
        .order_by(desc(Invoice.created_at))
        .limit(limit)
    )
    if customer_id:
        stmt = stmt.where(Invoice.customer_id == customer_id)
    if payment_status:
        stmt = stmt.where(Invoice.payment_status == payment_status)
        
    res = await db.execute(stmt)
    invoices = res.scalars().all()
    
    return [
        InvoiceResponse(
            id=inv.id,
            invoice_no=inv.invoice_no,
            financial_year=inv.financial_year,
            series_prefix=inv.series_prefix,
            sequence_number=inv.sequence_number,
            doc_type=inv.doc_type,
            delivery_id=inv.delivery_id,
            customer_id=inv.customer_id,
            customer_name_snap=inv.customer_name_snap,
            customer_phone_snap=inv.customer_phone_snap,
            customer_address_snap=inv.customer_address_snap,
            customer_gst_snap=inv.customer_gst_snap,
            invoice_date=inv.invoice_date,
            subtotal=inv.subtotal,
            total_tax=inv.total_tax,
            discount=inv.discount,
            total_amount=inv.total_amount,
            paid_amount=inv.paid_amount,
            outstanding_amount=inv.outstanding_amount,
            payment_status=inv.payment_status,
            is_demo_watermark=inv.is_demo_watermark,
            pdf_path=inv.pdf_path,
            items=[
                InvoiceItemResponse(
                    id=it.id,
                    product_id=it.product_id,
                    product_name_snap=it.product_name_snap,
                    hsn_snap=it.hsn_snap,
                    unit_code=it.unit_code,
                    billed_qty=it.billed_qty,
                    base_qty=it.base_qty,
                    unit_price_snap=it.unit_price_snap,
                    tax_rate_snap=it.tax_rate_snap,
                    cgst_amount=it.cgst_amount,
                    sgst_amount=it.sgst_amount,
                    igst_amount=it.igst_amount,
                    line_total=it.line_total
                )
                for it in inv.items
            ],
            created_at=inv.created_at
        )
        for inv in invoices
    ]


@router.get("/{id}", response_model=InvoiceResponse)
async def get_invoice(id: str, db: AsyncSession = Depends(get_db)):
    stmt = (
        select(Invoice)
        .options(selectinload(Invoice.items))
        .where(Invoice.id == id)
    )
    res = await db.execute(stmt)
    inv = res.scalar_one_or_none()
    if not inv:
        raise HTTPException(status_code=404, detail="Invoice not found")
        
    return InvoiceResponse(
        id=inv.id,
        invoice_no=inv.invoice_no,
        financial_year=inv.financial_year,
        series_prefix=inv.series_prefix,
        sequence_number=inv.sequence_number,
        doc_type=inv.doc_type,
        delivery_id=inv.delivery_id,
        customer_id=inv.customer_id,
        customer_name_snap=inv.customer_name_snap,
        customer_phone_snap=inv.customer_phone_snap,
        customer_address_snap=inv.customer_address_snap,
        customer_gst_snap=inv.customer_gst_snap,
        invoice_date=inv.invoice_date,
        subtotal=inv.subtotal,
        total_tax=inv.total_tax,
        discount=inv.discount,
        total_amount=inv.total_amount,
        paid_amount=inv.paid_amount,
        outstanding_amount=inv.outstanding_amount,
        payment_status=inv.payment_status,
        is_demo_watermark=inv.is_demo_watermark,
        pdf_path=inv.pdf_path,
        items=[
            InvoiceItemResponse(
                id=it.id,
                product_id=it.product_id,
                product_name_snap=it.product_name_snap,
                hsn_snap=it.hsn_snap,
                unit_code=it.unit_code,
                billed_qty=it.billed_qty,
                base_qty=it.base_qty,
                unit_price_snap=it.unit_price_snap,
                tax_rate_snap=it.tax_rate_snap,
                cgst_amount=it.cgst_amount,
                sgst_amount=it.sgst_amount,
                igst_amount=it.igst_amount,
                line_total=it.line_total
            )
            for it in inv.items
        ],
        created_at=inv.created_at
    )


@router.get("/{id}/pdf")
async def get_invoice_pdf(id: str, db: AsyncSession = Depends(get_db)):
    stmt = (
        select(Invoice)
        .options(selectinload(Invoice.items))
        .where(Invoice.id == id)
    )
    res = await db.execute(stmt)
    inv = res.scalar_one_or_none()
    if not inv:
        raise HTTPException(status_code=404, detail="Invoice not found")
        
    biz_stmt = select(BusinessSettings).limit(1)
    b_res = await db.execute(biz_stmt)
    biz = b_res.scalar_one_or_none()
    
    clean_no = inv.invoice_no.replace("/", "_")
    output_dir = os.path.join("static", "invoices")
    output_path = os.path.join(output_dir, f"invoice_{clean_no}.pdf")
    
    generate_invoice_pdf(invoice=inv, business=biz, output_path=output_path)
    
    return FileResponse(
        path=output_path,
        filename=f"Invoice_{clean_no}.pdf",
        media_type="application/pdf"
    )
