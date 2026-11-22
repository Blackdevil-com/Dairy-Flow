import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app

@pytest.mark.asyncio
async def test_owner_create_and_fetch_retail_shop():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        # Owner login
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        owner_token = owner_login.json()["access_token"]
        headers = {"Authorization": f"Bearer {owner_token}"}
        
        # 1. Create a retail shop with full customer details
        payload = {
            "name": "Sri Lakshmi Stores",
            "trade_name": "Lakshmi Super Bazaar",
            "customer_type": "RETAIL_SHOP",
            "contact_person": "Venkatesh Rao",
            "phone": "+91 9845012345",
            "alternate_phone": "+91 9845012346",
            "email": "contact@lakshmistores.in",
            "billing_address": "Shop #4, Near Temple, Main Market, Shivajinagar",
            "delivery_address": "Shop #4, Near Temple, Main Market, Shivajinagar",
            "area": "Shivajinagar",
            "city": "Pune",
            "district": "Pune",
            "state_code": "27",
            "postal_code": "411005",
            "gst_registration_status": "REGISTERED",
            "gst_number": "27AAAAA0000A1Z5",
            "credit_enabled": True,
            "credit_limit": 25000.00,
            "payment_terms_days": 7,
            "notes": "Deliver early morning before 6:30 AM"
        }
        
        resp = await client.post("/api/v1/customers", json=payload, headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["name"] == "Sri Lakshmi Stores"
        assert data["trade_name"] == "Lakshmi Super Bazaar"
        assert data["contact_person"] == "Venkatesh Rao"
        assert data["phone"] == "+91 9845012345"
        assert data["area"] == "Shivajinagar"
        assert data["credit_enabled"] is True
        assert float(data["credit_limit"]) == 25000.0
        cust_id = data["id"]
        
        # 2. Fetch customer by ID
        get_resp = await client.get(f"/api/v1/customers/{cust_id}", headers=headers)
        assert get_resp.status_code == 200
        get_data = get_resp.json()
        assert get_data["id"] == cust_id
        assert get_data["notes"] == "Deliver early morning before 6:30 AM"


@pytest.mark.asyncio
async def test_search_and_filters_customers():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        owner_token = owner_login.json()["access_token"]
        headers = {"Authorization": f"Bearer {owner_token}"}
        
        # Create a customer
        await client.post("/api/v1/customers", json={
            "name": "Kalyani Dairy Mart",
            "trade_name": "Kalyani Provisions",
            "customer_type": "RETAIL_SHOP",
            "contact_person": "Kalyani Joshi",
            "phone": "+91 9876543210",
            "area": "Kothrud"
        }, headers=headers)

        # Search by name
        res1 = await client.get("/api/v1/customers?query=Kalyani", headers=headers)
        assert res1.status_code == 200
        assert any(c["name"] == "Kalyani Dairy Mart" for c in res1.json())
        
        # Search by phone
        res2 = await client.get("/api/v1/customers?query=9876543210", headers=headers)
        assert res2.status_code == 200
        assert any(c["phone"] == "+91 9876543210" for c in res2.json())
        
        # Filter by customer_type
        res3 = await client.get("/api/v1/customers?customer_type=RETAIL_SHOP", headers=headers)
        assert res3.status_code == 200
        assert all(c["customer_type"] == "RETAIL_SHOP" for c in res3.json())


@pytest.mark.asyncio
async def test_customer_update():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        owner_token = owner_login.json()["access_token"]
        headers = {"Authorization": f"Bearer {owner_token}"}
        
        # Create
        create_resp = await client.post("/api/v1/customers", json={
            "name": "Balaji Dairy Center",
            "customer_type": "RETAIL_SHOP",
            "phone": "+91 9823098765",
            "area": "Hadapsar"
        }, headers=headers)
        assert create_resp.status_code == 200
        cust_id = create_resp.json()["id"]
        
        # Update phone and credit limit
        update_payload = {
            "contact_person": "Balaji Patil",
            "credit_enabled": True,
            "credit_limit": 15000.00,
            "area": "Magarpatta, Hadapsar"
        }
        update_resp = await client.put(f"/api/v1/customers/{cust_id}", json=update_payload, headers=headers)
        assert update_resp.status_code == 200
        updated = update_resp.json()
        assert updated["contact_person"] == "Balaji Patil"
        assert updated["credit_enabled"] is True
        assert float(updated["credit_limit"]) == 15000.0
        assert updated["area"] == "Magarpatta, Hadapsar"


@pytest.mark.asyncio
async def test_duplicate_customer_detection():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        owner_token = owner_login.json()["access_token"]
        headers = {"Authorization": f"Bearer {owner_token}"}
        
        payload = {
            "name": "Unique Dairy Mart",
            "customer_type": "RETAIL_SHOP",
            "phone": "+91 9998887771"
        }
        r1 = await client.post("/api/v1/customers", json=payload, headers=headers)
        assert r1.status_code == 200
        
        # Second submission with same name and phone must be rejected
        r2 = await client.post("/api/v1/customers", json=payload, headers=headers)
        assert r2.status_code == 400
        assert "already exists" in r2.json()["detail"]


@pytest.mark.asyncio
async def test_invalid_gstin_and_credit_validation():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        owner_token = owner_login.json()["access_token"]
        headers = {"Authorization": f"Bearer {owner_token}"}
        
        # Invalid GSTIN format
        bad_gst = {
            "name": "Bad GST Shop",
            "phone": "+91 9988776655",
            "gst_number": "INVALID_GST_123"
        }
        r1 = await client.post("/api/v1/customers", json=bad_gst, headers=headers)
        assert r1.status_code == 400
        assert "Invalid GSTIN format" in r1.json()["detail"]

        # Negative credit limit
        bad_credit = {
            "name": "Bad Credit Shop",
            "phone": "+91 9988776656",
            "credit_enabled": True,
            "credit_limit": -500.00
        }
        r2 = await client.post("/api/v1/customers", json=bad_credit, headers=headers)
        assert r2.status_code == 400


@pytest.mark.asyncio
async def test_customer_security_rbac():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        # Manager token should NOT have access to customer endpoints
        mgr_login = await client.post("/api/v1/auth/login", json={"username": "manager", "password": "manager123"})
        mgr_token = mgr_login.json()["access_token"]
        mgr_headers = {"Authorization": f"Bearer {mgr_token}"}
        
        # GET list
        r_list = await client.get("/api/v1/customers", headers=mgr_headers)
        assert r_list.status_code == 403

        # POST create
        r_create = await client.post("/api/v1/customers", json={
            "name": "Hacked Customer",
            "phone": "+91 9123456789"
        }, headers=mgr_headers)
        assert r_create.status_code == 403

        # Anonymous request (no token)
        r_anon = await client.get("/api/v1/customers")
        assert r_anon.status_code == 401


@pytest.mark.asyncio
async def test_deactivate_customer():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        owner_token = owner_login.json()["access_token"]
        headers = {"Authorization": f"Bearer {owner_token}"}
        
        create_resp = await client.post("/api/v1/customers", json={
            "name": "Deactivate Me Shop",
            "phone": "+91 9112233445"
        }, headers=headers)
        cust_id = create_resp.json()["id"]
        
        del_resp = await client.delete(f"/api/v1/customers/{cust_id}", headers=headers)
        assert del_resp.status_code == 200
        assert del_resp.json()["status"] == "deactivated"
        
        # Default active list should no longer include this customer
        list_resp = await client.get("/api/v1/customers?is_active=true", headers=headers)
        ids = [c["id"] for c in list_resp.json()]
        assert cust_id not in ids
