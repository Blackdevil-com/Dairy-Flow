import pytest
from httpx import AsyncClient, ASGITransport
from datetime import date, timedelta
from app.main import app

@pytest.mark.asyncio
async def test_post_purchases_with_default_supplier_string():
    """
    Directly reproduces the mobile request:
    POST /api/v1/purchases with supplier_id="default".
    Verifies that this now returns 200 OK rather than 404 Not Found.
    """
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        # Login as Inventory Manager
        login_res = await client.post("/api/v1/auth/login", json={"username": "manager", "password": "manager123"})
        assert login_res.status_code == 200
        token = login_res.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        # Fetch cow milk product
        prods = (await client.get("/api/v1/products")).json()
        cow_milk = next(p for p in prods if p["sku"] == "MLK-COW-01")

        today = date.today()
        payload = {
            "supplier_id": "default",
            "invoice_reference": "PO-SUPP-001",
            "notes": "Received via warehouse mobile app",
            "items": [
                {
                    "product_id": cow_milk["id"],
                    "batch_no": "BATCH-TEST-DEF-01",
                    "quantity_received": 10.0,
                    "unit_code": "CRT",
                    "unit_cost": 45.0,
                    "mfg_date": today.isoformat(),
                    "expiry_date": (today + timedelta(days=4)).isoformat()
                }
            ]
        }

        # Issue POST /api/v1/purchases
        res = await client.post("/api/v1/purchases", json=payload, headers=headers)
        assert res.status_code == 200, f"Expected 200 OK but got {res.status_code}: {res.text}"
        data = res.json()
        assert data["status"] == "RECEIVED"
        assert data["receipt_no"].startswith("REC-")
        assert len(data["items"]) == 1
        assert float(data["items"][0]["quantity_base"]) == 120.0  # 10 crates * 12 LTR


@pytest.mark.asyncio
async def test_post_purchases_with_null_supplier_and_trailing_slash():
    """
    Tests POST /api/v1/purchases/ (trailing slash) with null supplier_id.
    Ensures URL robustness without 404/307 issues.
    """
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        login_res = await client.post("/api/v1/auth/login", json={"username": "manager", "password": "manager123"})
        token = login_res.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        prods = (await client.get("/api/v1/products")).json()
        cow_milk = next(p for p in prods if p["sku"] == "MLK-COW-01")

        today = date.today()
        payload = {
            "supplier_id": None,
            "invoice_reference": "PO-SUPP-002",
            "items": [
                {
                    "product_id": cow_milk["id"],
                    "batch_no": "BATCH-TEST-SLASH-01",
                    "quantity_received": 5.0,
                    "unit_code": "LTR",
                    "unit_cost": 50.0,
                    "mfg_date": today.isoformat(),
                    "expiry_date": (today + timedelta(days=3)).isoformat()
                }
            ]
        }

        # Issue POST /api/v1/purchases/ with trailing slash
        res = await client.post("/api/v1/purchases/", json=payload, headers=headers)
        assert res.status_code == 200, f"Expected 200 OK but got {res.status_code}: {res.text}"
        data = res.json()
        assert data["status"] == "RECEIVED"
        assert float(data["items"][0]["quantity_base"]) == 5.0
