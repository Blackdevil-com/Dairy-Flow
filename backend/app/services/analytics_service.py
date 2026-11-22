from datetime import date, datetime, timedelta
from decimal import Decimal
from typing import Dict, Any, List, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func, desc

from app.models.sales import Delivery, DeliveryItem, Invoice
from app.models.payment import Payment
from app.models.product import Product
from app.models.partner import Customer
from app.models.stock import StockMovement
from app.services.invoice_service import get_financial_year

async def get_daily_summary(db: AsyncSession, target_date: date) -> Dict[str, Any]:
    # Deliveries on date
    deliv_stmt = select(
        func.count(Delivery.id).label("count"),
        func.coalesce(func.sum(Delivery.net_amount), Decimal("0.00")).label("total_net"),
        func.coalesce(func.sum(Delivery.total_base_qty), Decimal("0.0000")).label("total_qty")
    ).where(
        and_(
            Delivery.delivery_date == target_date,
            Delivery.status == "CONFIRMED"
        )
    )
    d_res = await db.execute(deliv_stmt)
    deliv_data = d_res.one()
    
    # Payments on date
    pay_stmt = select(
        func.coalesce(func.sum(Payment.amount), Decimal("0.00")).label("total_amount"),
        Payment.payment_mode
    ).where(Payment.payment_date == target_date).group_by(Payment.payment_mode)
    
    p_res = await db.execute(pay_stmt)
    pay_modes = p_res.all()
    
    cash_amt = Decimal("0.00")
    upi_amt = Decimal("0.00")
    bank_amt = Decimal("0.00")
    total_coll = Decimal("0.00")
    
    for row in pay_modes:
        amt = row.total_amount
        total_coll += amt
        if row.payment_mode == "CASH":
            cash_amt += amt
        elif row.payment_mode == "UPI":
            upi_amt += amt
        elif row.payment_mode in ["BANK_TRANSFER", "CHEQUE"]:
            bank_amt += amt
            
    return {
        "date": target_date,
        "deliveries_count": deliv_data.count,
        "total_sales_amount": deliv_data.total_net,
        "total_collections_amount": total_coll,
        "cash_collections": cash_amt,
        "upi_collections": upi_amt,
        "bank_collections": bank_amt,
        "total_stock_dispatched_litres": deliv_data.total_qty
    }


async def get_monthly_summary(db: AsyncSession, year: int, month: int) -> Dict[str, Any]:
    # Determine start and end date of month
    start_date = date(year, month, 1)
    if month == 12:
        end_date = date(year + 1, 1, 1) - timedelta(days=1)
    else:
        end_date = date(year, month + 1, 1) - timedelta(days=1)
        
    deliv_stmt = select(
        func.count(Delivery.id).label("count"),
        func.coalesce(func.sum(Delivery.net_amount), Decimal("0.00")).label("total_net")
    ).where(
        and_(
            Delivery.delivery_date >= start_date,
            Delivery.delivery_date <= end_date,
            Delivery.status == "CONFIRMED"
        )
    )
    d_res = await db.execute(deliv_stmt)
    deliv_data = d_res.one()
    
    # Total collections in month
    pay_stmt = select(
        func.coalesce(func.sum(Payment.amount), Decimal("0.00"))
    ).where(
        and_(
            Payment.payment_date >= start_date,
            Payment.payment_date <= end_date
        )
    )
    p_res = await db.execute(pay_stmt)
    total_collected = p_res.scalar() or Decimal("0.00")
    
    # Top products by sales
    top_prod_stmt = select(
        DeliveryItem.product_id,
        Product.name.label("product_name"),
        Product.canonical_unit_code.label("unit"),
        func.sum(DeliveryItem.base_qty).label("total_qty"),
        func.sum(DeliveryItem.line_total).label("total_amount"),
        func.count(DeliveryItem.id).label("orders_count")
    ).join(
        Product, DeliveryItem.product_id == Product.id
    ).join(
        Delivery, DeliveryItem.delivery_id == Delivery.id
    ).where(
        and_(
            Delivery.delivery_date >= start_date,
            Delivery.delivery_date <= end_date,
            Delivery.status == "CONFIRMED"
        )
    ).group_by(DeliveryItem.product_id, Product.name, Product.canonical_unit_code).order_by(desc("total_amount")).limit(10)
    
    tp_res = await db.execute(top_prod_stmt)
    top_products = [
        {
            "product_id": r.product_id,
            "product_name": r.product_name,
            "canonical_unit": r.unit,
            "total_quantity_base": r.total_qty or Decimal("0.0000"),
            "total_sales_amount": r.total_amount or Decimal("0.00"),
            "orders_count": r.orders_count
        }
        for r in tp_res.all()
    ]
    
    # Top customers
    top_cust_stmt = select(
        Customer.id.label("customer_id"),
        Customer.name.label("customer_name"),
        Customer.phone,
        Customer.current_balance,
        func.count(Delivery.id).label("total_deliveries"),
        func.coalesce(func.sum(Delivery.net_amount), Decimal("0.00")).label("total_billed")
    ).join(
        Delivery, Customer.id == Delivery.customer_id
    ).where(
        and_(
            Delivery.delivery_date >= start_date,
            Delivery.delivery_date <= end_date,
            Delivery.status == "CONFIRMED"
        )
    ).group_by(Customer.id, Customer.name, Customer.phone, Customer.current_balance).order_by(desc("total_billed")).limit(10)
    
    tc_res = await db.execute(top_cust_stmt)
    top_customers = [
        {
            "customer_id": r.customer_id,
            "customer_name": r.customer_name,
            "phone": r.phone,
            "total_deliveries": r.total_deliveries,
            "total_billed_amount": r.total_billed,
            "total_paid_amount": Decimal("0.00"),
            "outstanding_balance": r.current_balance
        }
        for r in tc_res.all()
    ]
    
    month_name = start_date.strftime("%B")
    return {
        "year": year,
        "month": month,
        "month_name": month_name,
        "total_deliveries": deliv_data.count,
        "total_sales_amount": deliv_data.total_net,
        "total_collected_amount": total_collected,
        "total_outstanding_created": max(Decimal("0.00"), deliv_data.total_net - total_collected),
        "top_products": top_products,
        "top_customers": top_customers
    }


async def get_yearly_summary(db: AsyncSession, fy: Optional[str] = None) -> Dict[str, Any]:
    if not fy:
        fy = get_financial_year()
        
    start_year = int(fy.split("-")[0])
    start_date = date(start_year, 4, 1)
    end_date = date(start_year + 1, 3, 31)
    
    # Invoices & turnover in FY
    inv_stmt = select(
        func.count(Invoice.id).label("count"),
        func.coalesce(func.sum(Invoice.total_amount), Decimal("0.00")).label("turnover"),
        func.coalesce(func.sum(Invoice.total_tax), Decimal("0.00")).label("tax"),
        func.coalesce(func.sum(Invoice.paid_amount), Decimal("0.00")).label("paid"),
        func.coalesce(func.sum(Invoice.outstanding_amount), Decimal("0.00")).label("outstanding")
    ).where(
        and_(
            Invoice.invoice_date >= start_date,
            Invoice.invoice_date <= end_date
        )
    )
    inv_res = await db.execute(inv_stmt)
    inv_data = inv_res.one()
    
    # Monthly breakdown within FY
    monthly_trends = []
    curr = start_date
    while curr <= min(end_date, date.today()):
        m_year = curr.year
        m_month = curr.month
        m_summary = await get_monthly_summary(db, m_year, m_month)
        monthly_trends.append({
            "year": m_year,
            "month": m_month,
            "month_name": m_summary["month_name"],
            "sales": float(m_summary["total_sales_amount"]),
            "collections": float(m_summary["total_collected_amount"])
        })
        if m_month == 12:
            curr = date(m_year + 1, 1, 1)
        else:
            curr = date(m_year, m_month + 1, 1)
            
    return {
        "financial_year": fy,
        "start_date": start_date,
        "end_date": end_date,
        "total_deliveries": inv_data.count,
        "total_turnover": inv_data.turnover,
        "total_collected": inv_data.paid,
        "total_outstanding": inv_data.outstanding,
        "gst_demo_tax_summary": {
            "demo_watermarked": True,
            "total_demo_tax_calculated": float(inv_data.tax),
            "cgst_calculated": float(inv_data.tax / Decimal("2.00")),
            "sgst_calculated": float(inv_data.tax / Decimal("2.00")),
            "note": "Watermarked DEMO calculation only. Subject to verified GSTIN registration."
        },
        "monthly_trends": monthly_trends
    }
