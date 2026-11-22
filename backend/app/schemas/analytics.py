from typing import Optional, List
from datetime import date
from decimal import Decimal
from pydantic import BaseModel

class ProductSalesMetric(BaseModel):
    product_id: str
    product_name: str
    canonical_unit: str
    total_quantity_base: Decimal
    total_sales_amount: Decimal
    orders_count: int

class CustomerSalesMetric(BaseModel):
    customer_id: str
    customer_name: str
    phone: str
    total_deliveries: int
    total_billed_amount: Decimal
    total_paid_amount: Decimal
    outstanding_balance: Decimal

class StockReportItem(BaseModel):
    product_id: str
    product_name: str
    canonical_unit: str
    opening_stock: Decimal
    incoming_stock: Decimal
    outgoing_stock: Decimal
    adjustment_stock: Decimal
    closing_stock: Decimal

class DailySummaryResponse(BaseModel):
    date: date
    deliveries_count: int
    total_sales_amount: Decimal
    total_collections_amount: Decimal
    cash_collections: Decimal
    upi_collections: Decimal
    bank_collections: Decimal
    total_stock_dispatched_litres: Decimal

class MonthlySummaryResponse(BaseModel):
    year: int
    month: int
    month_name: str
    total_deliveries: int
    total_sales_amount: Decimal
    total_collected_amount: Decimal
    total_outstanding_created: Decimal
    top_products: List[ProductSalesMetric] = []
    top_customers: List[CustomerSalesMetric] = []

class YearlySummaryResponse(BaseModel):
    financial_year: str  # e.g., "2026-27"
    start_date: date
    end_date: date
    total_deliveries: int
    total_turnover: Decimal
    total_collected: Decimal
    total_outstanding: Decimal
    gst_demo_tax_summary: dict = {}
    product_breakdown: List[ProductSalesMetric] = []
    monthly_trends: List[dict] = []
