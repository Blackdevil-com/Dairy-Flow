from decimal import Decimal
from datetime import date
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.models.business import BusinessSettings, Warehouse
from app.models.user import User
from app.models.product import ProductCategory, Unit, Product, ProductUnit, ProductTaxProfile
from app.models.partner import Customer, Supplier
from app.core.security import get_password_hash

async def seed_initial_data(db: AsyncSession):
    # 1. Business Settings
    b_stmt = select(BusinessSettings).limit(1)
    b_res = await db.execute(b_stmt)
    if not b_res.scalar_one_or_none():
        biz = BusinessSettings(
            business_name="DairyFlow Co-operative & Milk Processing",
            trade_name="DairyFlow Depot",
            state_code="27",
            state_name="Maharashtra",
            gst_status="UNVERIFIED_DEMO",
            currency_symbol="₹",
            default_doc_type="COMMERCIAL_RECEIPT",
            demo_watermark=True,
            address="Plot No. 42, MIDC Industrial Area, Pune, Maharashtra 411019",
            phone="+91 9822001122",
            email="contact@dairyflow.local"
        )
        db.add(biz)

    # 2. Primary Warehouse
    w_stmt = select(Warehouse).where(Warehouse.code == "WH-CENTRAL")
    w_res = await db.execute(w_stmt)
    wh = w_res.scalar_one_or_none()
    if not wh:
        wh = Warehouse(
            code="WH-CENTRAL",
            name="DairyFlow Central Cold Storage Depot",
            address="Plot No. 42, MIDC, Pune",
            is_primary=True,
            is_active=True
        )
        db.add(wh)
        await db.flush()

    # 3. Units
    units_data = [
        ("LTR", "Litres", "L", True),
        ("KG", "Kilograms", "kg", True),
        ("PKT", "Packets", "pkt", False),
        ("CRT", "Crates", "crt", False),
        ("GM", "Grams", "g", False),
        ("ML", "Millilitres", "ml", False),
    ]
    for code, name, sym, is_base in units_data:
        u_check = await db.get(Unit, code)
        if not u_check:
            db.add(Unit(code=code, name=name, symbol=sym, is_base_standard=is_base))
    await db.flush()

    # 4. Users (Role 1: OWNER, Role 2: INVENTORY_MANAGER)
    users_data = [
        ("owner", "owner@dairyflow.local", "owner123", "Rajesh Deshmukh (Dairy Owner)", "OWNER", "+91 9822000001"),
        ("manager", "manager@dairyflow.local", "manager123", "Suresh Kulkarni (Warehouse Manager)", "INVENTORY_MANAGER", "+91 9822000002")
    ]
    for uname, email, pwd, fname, role, phone in users_data:
        u_stmt = select(User).where(User.username == uname)
        u_res = await db.execute(u_stmt)
        if not u_res.scalar_one_or_none():
            db.add(User(
                username=uname,
                email=email,
                hashed_password=get_password_hash(pwd),
                full_name=fname,
                role=role,
                phone=phone,
                is_active=True
            ))
    await db.flush()

    # 5. Product Categories
    categories_data = [
        ("Liquid Milk", "Fresh pasteurized whole, toned, and standardized milk"),
        ("Fermented & Fresh", "Fresh curd, paneer, and buttermilk products"),
        ("Ghee & Traditional", "Pure desi cow ghee and traditional butter")
    ]
    cats = {}
    for c_name, c_desc in categories_data:
        c_stmt = select(ProductCategory).where(ProductCategory.name == c_name)
        c_res = await db.execute(c_stmt)
        cat = c_res.scalar_one_or_none()
        if not cat:
            cat = ProductCategory(name=c_name, description=c_desc)
            db.add(cat)
            await db.flush()
        cats[c_name] = cat.id

    # 6. Products with per-product unit conversions & tax profiles
    products_seed = [
        {
            "sku": "MLK-COW-01",
            "name": "Fresh Cow Milk (Pouch 500ml / Crate)",
            "category": "Liquid Milk",
            "canonical_unit": "LTR",
            "sale_price": Decimal("56.00"),
            "cost_price": Decimal("44.00"),
            "min_alert": Decimal("50.000"),
            "hsn": "0401",
            "cgst": Decimal("0.00"),
            "sgst": Decimal("0.00"),
            "units": [
                ("CRT", Decimal("12.0000")),  # 1 crate of 24 pouches (500ml) = 12 Litres
                ("PKT", Decimal("0.5000")),   # 1 pouch = 0.5 Litres
            ]
        },
        {
            "sku": "MLK-BUF-01",
            "name": "Special Buffalo Milk (Full Cream)",
            "category": "Liquid Milk",
            "canonical_unit": "LTR",
            "sale_price": Decimal("72.00"),
            "cost_price": Decimal("58.00"),
            "min_alert": Decimal("40.000"),
            "hsn": "0401",
            "cgst": Decimal("0.00"),
            "sgst": Decimal("0.00"),
            "units": [
                ("CRT", Decimal("12.0000")),
                ("PKT", Decimal("0.5000")),
            ]
        },
        {
            "sku": "CURD-STD-01",
            "name": "DairyFlow Pure Standard Curd",
            "category": "Fermented & Fresh",
            "canonical_unit": "KG",
            "sale_price": Decimal("75.00"),
            "cost_price": Decimal("55.00"),
            "min_alert": Decimal("25.000"),
            "hsn": "0403",
            "cgst": Decimal("2.50"),  # Demo tax rate 5% total
            "sgst": Decimal("2.50"),
            "units": [
                ("PKT", Decimal("0.4000")),   # 400g tub = 0.4000 KG
                ("CRT", Decimal("9.6000")),   # Crate of 24 tubs = 9.6 KG
            ]
        },
        {
            "sku": "PNR-MAL-01",
            "name": "Fresh Malai Paneer Block",
            "category": "Fermented & Fresh",
            "canonical_unit": "KG",
            "sale_price": Decimal("390.00"),
            "cost_price": Decimal("310.00"),
            "min_alert": Decimal("15.000"),
            "hsn": "0406",
            "cgst": Decimal("2.50"),
            "sgst": Decimal("2.50"),
            "units": [
                ("PKT", Decimal("0.2000")),   # 200g pack = 0.2000 KG
                ("CRT", Decimal("10.0000")),  # Crate of 50 packs = 10 KG
            ]
        },
        {
            "sku": "GHEE-COW-01",
            "name": "Pure Desi Cow Ghee (Jar)",
            "category": "Ghee & Traditional",
            "canonical_unit": "KG",
            "sale_price": Decimal("680.00"),
            "cost_price": Decimal("520.00"),
            "min_alert": Decimal("10.000"),
            "hsn": "0405",
            "cgst": Decimal("6.00"),  # Demo 12% total
            "sgst": Decimal("6.00"),
            "units": [
                ("PKT", Decimal("1.0000")),   # 1kg jar = 1 KG
                ("CRT", Decimal("12.0000")),  # Carton of 12 jars = 12 KG
            ]
        }
    ]

    for p_info in products_seed:
        p_stmt = select(Product).where(Product.sku == p_info["sku"])
        p_res = await db.execute(p_stmt)
        if not p_res.scalar_one_or_none():
            prod = Product(
                category_id=cats[p_info["category"]],
                sku=p_info["sku"],
                name=p_info["name"],
                canonical_unit_code=p_info["canonical_unit"],
                default_sale_price=p_info["sale_price"],
                default_cost_price=p_info["cost_price"],
                min_stock_alert=p_info["min_alert"],
                is_active=True
            )
            db.add(prod)
            await db.flush()

            for u_code, conv in p_info["units"]:
                db.add(ProductUnit(
                    product_id=prod.id,
                    unit_code=u_code,
                    conversion_to_base=conv
                ))

            db.add(ProductTaxProfile(
                product_id=prod.id,
                hsn_code=p_info["hsn"],
                cgst_rate=p_info["cgst"],
                sgst_rate=p_info["sgst"],
                igst_rate=p_info["cgst"] + p_info["sgst"],
                is_demo=True
            ))

    # 7. Suppliers
    suppliers_data = [
        ("Sahyadri Farmers Milk Producers Co-op", "Babanrao Shinde", "+91 9422011223", "Baramati Road, Pune"),
        ("Krishna Valley Dairy Consortium", "Anil Patil", "+91 9422088990", "Sangli Highway, Maharashtra")
    ]
    for s_name, contact, phone, addr in suppliers_data:
        s_stmt = select(Supplier).where(Supplier.name == s_name)
        s_res = await db.execute(s_stmt)
        if not s_res.scalar_one_or_none():
            db.add(Supplier(
                name=s_name,
                contact_person=contact,
                phone=phone,
                address=addr,
                is_active=True
            ))

    # 8. Customers
    customers_data = [
        ("Shree Ganesh Dairy & Sweets", "Ganesh Dairy", "RETAIL_SHOP", "+91 9822110011", "Shop 12, Station Road, Pune", Decimal("40000.00")),
        ("Mauli Supermarket & Provisions", "Mauli Mart", "RETAIL_SHOP", "+91 9822110022", "Kothrud Main Road, Pune", Decimal("60000.00")),
        ("Anand Sharma (Monthly Milk Subscription)", "A. Sharma", "INDIVIDUAL", "+91 9822110033", "Flat 402, Green Acres, Aundh, Pune", Decimal("10000.00"))
    ]
    for c_name, t_name, c_type, phone, addr, limit in customers_data:
        c_stmt = select(Customer).where(Customer.phone == phone)
        c_res = await db.execute(c_stmt)
        if not c_res.scalar_one_or_none():
            db.add(Customer(
                name=c_name,
                trade_name=t_name,
                customer_type=c_type,
                phone=phone,
                address=addr,
                credit_limit=limit,
                current_balance=Decimal("0.00"),
                is_active=True
            ))

    await db.commit()
