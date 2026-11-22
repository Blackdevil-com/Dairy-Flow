from fastapi import APIRouter
from app.api.v1.endpoints import (
    auth, products, inventory, purchases, customers,
    deliveries, invoices, payments, analytics, sync
)

api_router = APIRouter()

api_router.include_router(auth.router, prefix="/auth", tags=["Authentication"])
api_router.include_router(products.router, prefix="/products", tags=["Products"])
api_router.include_router(inventory.router, prefix="/inventory", tags=["Inventory & Ledger"])
api_router.include_router(purchases.router, prefix="/purchases", tags=["Purchases & Inward Stock"])
api_router.include_router(customers.router, prefix="/customers", tags=["Customers & Suppliers"])
api_router.include_router(deliveries.router, prefix="/deliveries", tags=["Deliveries"])
api_router.include_router(invoices.router, prefix="/invoices", tags=["Invoices & Billing"])
api_router.include_router(payments.router, prefix="/payments", tags=["Payments"])
api_router.include_router(analytics.router, prefix="/analytics", tags=["Analytics & Reports"])
api_router.include_router(sync.router, prefix="/sync", tags=["Offline Sync"])
