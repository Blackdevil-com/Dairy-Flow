import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app

@pytest.mark.asyncio
async def test_product_listing_and_canonical_units():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        res = await client.get("/api/v1/products")
        assert res.status_code == 200
        products = res.json()
        assert len(products) >= 5
        
        # Verify Fresh Cow Milk has LTR as canonical and has per-product conversion for CRT (12 LTR)
        cow_milk = next((p for p in products if p["sku"] == "MLK-COW-01"), None)
        assert cow_milk is not None
        assert cow_milk["canonical_unit_code"] == "LTR"
        
        crt_unit = next((u for u in cow_milk["units"] if u["unit_code"] == "CRT"), None)
        assert crt_unit is not None
        assert float(crt_unit["conversion_to_base"]) == 12.0

@pytest.mark.asyncio
async def test_create_custom_product_with_conversions():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        login_res = await client.post("/api/v1/auth/login", json={"username": "manager", "password": "manager123"})
        token = login_res.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        cats = (await client.get("/api/v1/products/categories")).json()
        cat_id = cats[0]["id"]
        
        new_prod = {
            "category_id": cat_id,
            "sku": "BUTTER-SALT-01",
            "name": "Pasteurized Salted Butter (500g Tub)",
            "canonical_unit_code": "KG",
            "default_sale_price": "280.00",
            "default_cost_price": "220.00",
            "min_stock_alert": "10.00",
            "hsn_code": "0405",
            "cgst_rate": "6.00",
            "sgst_rate": "6.00",
            "igst_rate": "12.00",
            "units": [
                {"unit_code": "PKT", "conversion_to_base": "0.5000", "is_purchase_unit": True, "is_delivery_unit": True},
                {"unit_code": "CRT", "conversion_to_base": "10.0000", "is_purchase_unit": True, "is_delivery_unit": True}
            ]
        }
        res = await client.post("/api/v1/products", json=new_prod, headers=headers)
        assert res.status_code == 200
        data = res.json()
        assert data["sku"] == "BUTTER-SALT-01"
        assert len(data["units"]) == 2
