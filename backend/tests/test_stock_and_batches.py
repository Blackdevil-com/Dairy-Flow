import pytest
from httpx import AsyncClient, ASGITransport
from datetime import date, timedelta
from app.main import app

@pytest.mark.asyncio
async def test_inward_purchase_and_stock_increase():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        # Login as Inventory Manager
        login_res = await client.post("/api/v1/auth/login", json={"username": "manager", "password": "manager123"})
        token = login_res.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        # Get products and suppliers
        prods = (await client.get("/api/v1/products")).json()
        cow_milk = next(p for p in prods if p["sku"] == "MLK-COW-01")
        supps = (await client.get("/api/v1/customers/suppliers/list")).json()
        supplier_id = supps[0]["id"]
        
        initial_stock = float(cow_milk["current_stock_base"])
        
        # Receive 10 Crates of Cow Milk (1 Crate = 12 Litres -> 120 Litres total)
        today = date.today()
        purchase_data = {
            "supplier_id": supplier_id,
            "invoice_reference": "INV-SUPP-101",
            "items": [
                {
                    "product_id": cow_milk["id"],
                    "batch_no": "BATCH-CM-TEST-1",
                    "quantity_received": "10.00",
                    "unit_code": "CRT",
                    "unit_cost": "500.00",
                    "mfg_date": today.isoformat(),
                    "expiry_date": (today + timedelta(days=4)).isoformat()
                }
            ]
        }
        res = await client.post("/api/v1/purchases", json=purchase_data, headers=headers)
        assert res.status_code == 200
        rec = res.json()
        assert rec["status"] == "RECEIVED"
        assert float(rec["items"][0]["quantity_base"]) == 120.0
        
        # Verify product stock increased by 120 Litres
        updated_prod = (await client.get(f"/api/v1/products/{cow_milk['id']}")).json()
        assert float(updated_prod["current_stock_base"]) == initial_stock + 120.0
        
        # Verify ledger has positive signed movement
        ledger_res = await client.get(f"/api/v1/inventory/ledger?product_id={cow_milk['id']}", headers=headers)
        assert ledger_res.status_code == 200
        movements = ledger_res.json()
        latest_movement = movements[0]
        assert float(latest_movement["signed_base_quantity"]) == 120.0
        assert latest_movement["movement_type"] == "PURCHASE_RECEIPT"
