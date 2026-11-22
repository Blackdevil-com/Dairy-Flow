from typing import List, Optional
from decimal import Decimal
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, func
from sqlalchemy.orm import selectinload

from app.db.session import get_db
from app.models.product import Product, ProductCategory, Unit, ProductUnit, ProductTaxProfile
from app.models.stock import StockBatch
from app.models.user import User
from app.schemas.product import (
    ProductResponse, ProductCreate, ProductUpdate,
    CategoryResponse, CategoryCreate,
    UnitResponse, UnitCreate
)
from app.api.deps import get_current_user, require_roles

router = APIRouter()

# --- Categories ---
@router.get("/categories", response_model=List[CategoryResponse])
async def list_categories(db: AsyncSession = Depends(get_db)):
    res = await db.execute(select(ProductCategory).order_by(ProductCategory.name))
    return res.scalars().all()

@router.post("/categories", response_model=CategoryResponse)
async def create_category(
    cat_in: CategoryCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["INVENTORY_MANAGER", "OWNER"]))
):
    cat = ProductCategory(name=cat_in.name, description=cat_in.description)
    db.add(cat)
    await db.commit()
    await db.refresh(cat)
    return cat

# --- Units ---
@router.get("/units", response_model=List[UnitResponse])
async def list_units(db: AsyncSession = Depends(get_db)):
    res = await db.execute(select(Unit).order_by(Unit.code))
    return res.scalars().all()

@router.post("/units", response_model=UnitResponse)
async def create_unit(
    unit_in: UnitCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["INVENTORY_MANAGER", "OWNER"]))
):
    unit = Unit(code=unit_in.code.upper(), name=unit_in.name, symbol=unit_in.symbol, is_base_standard=unit_in.is_base_standard)
    db.add(unit)
    await db.commit()
    return unit

# --- Products ---
@router.get("", response_model=List[ProductResponse])
async def list_products(
    category_id: Optional[str] = None,
    is_active: Optional[bool] = None,
    db: AsyncSession = Depends(get_db)
):
    stmt = (
        select(Product)
        .options(
            selectinload(Product.category),
            selectinload(Product.units),
            selectinload(Product.tax_profiles)
        )
    )
    if category_id:
        stmt = stmt.where(Product.category_id == category_id)
    if is_active is not None:
        stmt = stmt.where(Product.is_active == is_active)
    stmt = stmt.order_by(Product.name)
    
    res = await db.execute(stmt)
    products = res.scalars().all()
    
    # Calculate live base stock for each product
    response_list = []
    for p in products:
        stock_stmt = select(func.coalesce(func.sum(StockBatch.available_base_qty), Decimal("0.0000"))).where(
            and_(StockBatch.product_id == p.id, StockBatch.status == "ACTIVE")
        )
        s_res = await db.execute(stock_stmt)
        live_stock = s_res.scalar() or Decimal("0.0000")
        
        p_dict = {
            "id": p.id,
            "category_id": p.category_id,
            "category_name": p.category.name if p.category else None,
            "sku": p.sku,
            "name": p.name,
            "description": p.description,
            "canonical_unit_code": p.canonical_unit_code,
            "default_sale_price": p.default_sale_price,
            "default_cost_price": p.default_cost_price,
            "min_stock_alert": p.min_stock_alert,
            "is_active": p.is_active,
            "current_stock_base": live_stock,
            "units": p.units,
            "tax_profiles": p.tax_profiles,
            "created_at": p.created_at
        }
        response_list.append(p_dict)
        
    return response_list


@router.post("", response_model=ProductResponse)
async def create_product(
    prod_in: ProductCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["INVENTORY_MANAGER", "OWNER"]))
):
    # Verify SKU uniqueness
    sku_check = await db.execute(select(Product).where(Product.sku == prod_in.sku))
    if sku_check.scalar_one_or_none():
        raise HTTPException(status_code=400, detail=f"Product with SKU '{prod_in.sku}' already exists")
        
    product = Product(
        category_id=prod_in.category_id,
        sku=prod_in.sku,
        name=prod_in.name,
        description=prod_in.description,
        canonical_unit_code=prod_in.canonical_unit_code,
        default_sale_price=prod_in.default_sale_price,
        default_cost_price=prod_in.default_cost_price,
        min_stock_alert=prod_in.min_stock_alert,
        is_active=True
    )
    db.add(product)
    await db.flush()
    
    # Add units
    for u in prod_in.units:
        p_unit = ProductUnit(
            product_id=product.id,
            unit_code=u.unit_code,
            conversion_to_base=u.conversion_to_base,
            is_purchase_unit=u.is_purchase_unit,
            is_delivery_unit=u.is_delivery_unit
        )
        db.add(p_unit)
        
    # Add tax profile
    tax_prof = ProductTaxProfile(
        product_id=product.id,
        hsn_code=prod_in.hsn_code,
        cgst_rate=prod_in.cgst_rate,
        sgst_rate=prod_in.sgst_rate,
        igst_rate=prod_in.igst_rate,
        is_demo=True
    )
    db.add(tax_prof)
    
    await db.commit()
    
    # Reload with relations
    stmt = (
        select(Product)
        .options(
            selectinload(Product.category),
            selectinload(Product.units),
            selectinload(Product.tax_profiles)
        )
        .where(Product.id == product.id)
    )
    res = await db.execute(stmt)
    full_prod = res.scalar_one()
    return {
        "id": full_prod.id,
        "category_id": full_prod.category_id,
        "category_name": full_prod.category.name if full_prod.category else None,
        "sku": full_prod.sku,
        "name": full_prod.name,
        "description": full_prod.description,
        "canonical_unit_code": full_prod.canonical_unit_code,
        "default_sale_price": full_prod.default_sale_price,
        "default_cost_price": full_prod.default_cost_price,
        "min_stock_alert": full_prod.min_stock_alert,
        "is_active": full_prod.is_active,
        "current_stock_base": Decimal("0.0000"),
        "units": full_prod.units,
        "tax_profiles": full_prod.tax_profiles,
        "created_at": full_prod.created_at
    }


@router.get("/{id}", response_model=ProductResponse)
async def get_product(id: str, db: AsyncSession = Depends(get_db)):
    stmt = (
        select(Product)
        .options(
            selectinload(Product.category),
            selectinload(Product.units),
            selectinload(Product.tax_profiles)
        )
        .where(Product.id == id)
    )
    res = await db.execute(stmt)
    prod = res.scalar_one_or_none()
    if not prod:
        raise HTTPException(status_code=404, detail="Product not found")
        
    stock_stmt = select(func.coalesce(func.sum(StockBatch.available_base_qty), Decimal("0.0000"))).where(
        and_(StockBatch.product_id == prod.id, StockBatch.status == "ACTIVE")
    )
    s_res = await db.execute(stock_stmt)
    live_stock = s_res.scalar() or Decimal("0.0000")
    
    return {
        "id": prod.id,
        "category_id": prod.category_id,
        "category_name": prod.category.name if prod.category else None,
        "sku": prod.sku,
        "name": prod.name,
        "description": prod.description,
        "canonical_unit_code": prod.canonical_unit_code,
        "default_sale_price": prod.default_sale_price,
        "default_cost_price": prod.default_cost_price,
        "min_stock_alert": prod.min_stock_alert,
        "is_active": prod.is_active,
        "current_stock_base": live_stock,
        "units": prod.units,
        "tax_profiles": prod.tax_profiles,
        "created_at": prod.created_at
    }
