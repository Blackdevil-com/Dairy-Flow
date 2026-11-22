import pytest
from httpx import AsyncClient, ASGITransport
from datetime import date, timedelta
from app.main import app

@pytest.mark.asyncio
async def test_full_acceptance_test_workflow():
    """
    CRITICAL ACCEPTANCE TEST:
    1. Inventory manager adds 100 litres of milk.
    2. Owner logs in on another device and sees 100 litres.
    3. Owner creates and confirms a 20-litre delivery to a retail shop.
    4. Backend creates the invoice, records stock movement of -20 litres and updates available quantity to 80 litres.
    5. Inventory manager refreshes their device and sees 80 litres.
    6. Owner records a partial payment and sees the outstanding balance.
    7. The delivery and payment appear correctly in analytics.
    """
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        # Step 1: Inventory Manager logs in
        mgr_login = await client.post("/api/v1/auth/login", json={"username": "manager", "password": "manager123"})
        mgr_token = mgr_login.json()["access_token"]
        mgr_headers = {"Authorization": f"Bearer {mgr_token}"}
        
        prods = (await client.get("/api/v1/products")).json()
        cow_milk = next(p for p in prods if p["sku"] == "MLK-COW-01")
        supps = (await client.get("/api/v1/customers/suppliers/list")).json()
        
        today = date.today()
        # Add 100 Litres
        receipt_res = await client.post("/api/v1/purchases", json={
            "supplier_id": supps[0]["id"],
            "invoice_reference": "SUPP-ACCEPT-001",
            "items": [
                {
                    "product_id": cow_milk["id"],
                    "batch_no": "BATCH-ACCEPT-100",
                    "quantity_received": "100.00",
                    "unit_code": "LTR",
                    "unit_cost": "45.00",
                    "mfg_date": today.isoformat(),
                    "expiry_date": (today + timedelta(days=5)).isoformat()
                }
            ]
        }, headers=mgr_headers)
        assert receipt_res.status_code == 200
        
        # Step 2: Owner logs in on another device and inspects stock
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        owner_token = owner_login.json()["access_token"]
        owner_headers = {"Authorization": f"Bearer {owner_token}"}
        
        owner_prod = (await client.get(f"/api/v1/products/{cow_milk['id']}", headers=owner_headers)).json()
        current_stock = float(owner_prod["current_stock_base"])
        assert current_stock >= 100.0
        
        # Step 3: Owner creates and confirms a 20-litre delivery to retail shop
        custs = (await client.get("/api/v1/customers", headers=owner_headers)).json()
        retail_shop = custs[0]
        initial_customer_balance = float(retail_shop["current_balance"])
        
        # Create draft delivery
        draft_res = await client.post("/api/v1/deliveries", json={
            "customer_id": retail_shop["id"],
            "items": [
                {
                    "product_id": cow_milk["id"],
                    "unit_code": "LTR",
                    "requested_qty": "20.00",
                    "unit_price": "56.00"
                }
            ]
        }, headers=owner_headers)
        assert draft_res.status_code == 200
        draft_deliv = draft_res.json()
        assert draft_deliv["status"] == "DRAFT"
        assert float(draft_deliv["net_amount"]) == 1120.0
        
        # Confirm delivery
        confirm_res = await client.post(
            f"/api/v1/deliveries/{draft_deliv['id']}/confirm",
            json={"idempotency_key": "IDEMP-ACCEPTANCE-001"},
            headers=owner_headers
        )
        assert confirm_res.status_code == 200
        confirmed_deliv = confirm_res.json()
        assert confirmed_deliv["status"] == "CONFIRMED"
        assert confirmed_deliv["invoice_no"] is not None
        
        # Step 4: Verify stock deduction and ledger -20 Litres
        mgr_prod_after = (await client.get(f"/api/v1/products/{cow_milk['id']}", headers=mgr_headers)).json()
        assert float(mgr_prod_after["current_stock_base"]) == current_stock - 20.0
        
        # Step 5: Check stock movement ledger has -20.00
        ledger = (await client.get(f"/api/v1/inventory/ledger?product_id={cow_milk['id']}", headers=mgr_headers)).json()
        dispatch_mov = next(m for m in ledger if m["reference_id"] == draft_deliv["id"])
        assert float(dispatch_mov["signed_base_quantity"]) == -20.0
        assert dispatch_mov["movement_type"] == "DELIVERY_DISPATCH"
        
        # Step 6: Owner records partial payment of ₹500 against customer bill of ₹1120
        payment_res = await client.post("/api/v1/payments", json={
            "customer_id": retail_shop["id"],
            "payment_mode": "UPI",
            "amount": "500.00",
            "transaction_ref": "UPI-REF-99887766",
            "specific_invoice_id": confirmed_deliv["invoice_id"]
        }, headers=owner_headers)
        assert payment_res.status_code == 200
        
        # Customer outstanding balance should reflect 1120 - 500 = 620
        updated_cust = (await client.get(f"/api/v1/customers/{retail_shop['id']}", headers=owner_headers)).json()
        assert float(updated_cust["current_balance"]) == initial_customer_balance + 620.0
        
        # Step 7: Delivery and Payment appear in Analytics
        analytics = (await client.get("/api/v1/analytics/summary", headers=owner_headers)).json()
        assert analytics["deliveries_count"] >= 1
        assert float(analytics["total_collections_amount"]) >= 500.0
        assert float(analytics["upi_collections"]) >= 500.0


@pytest.mark.asyncio
async def test_insufficient_stock_rejection():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        token = owner_login.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        prods = (await client.get("/api/v1/products")).json()
        ghee = next(p for p in prods if p["sku"] == "GHEE-COW-01")
        custs = (await client.get("/api/v1/customers")).json()
        
        # Try to deliver 999999 KG of Ghee (far exceeding available stock)
        draft = (await client.post("/api/v1/deliveries", json={
            "customer_id": custs[0]["id"],
            "items": [
                {
                    "product_id": ghee["id"],
                    "unit_code": "KG",
                    "requested_qty": "999999.00"
                }
            ]
        }, headers=headers)).json()
        
        confirm_res = await client.post(
            f"/api/v1/deliveries/{draft['id']}/confirm",
            json={},
            headers=headers
        )
        assert confirm_res.status_code == 400
        assert "Insufficient stock" in confirm_res.json()["detail"]
