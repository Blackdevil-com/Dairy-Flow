import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from app.main import app

@pytest.mark.asyncio
async def test_login_owner_success():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        res = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        assert res.status_code == 200
        data = res.json()
        assert data["role"] == "OWNER"
        assert "access_token" in data
        assert "refresh_token" in data

@pytest.mark.asyncio
async def test_login_manager_success():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        res = await client.post("/api/v1/auth/login", json={"username": "manager", "password": "manager123"})
        assert res.status_code == 200
        data = res.json()
        assert data["role"] == "INVENTORY_MANAGER"

@pytest.mark.asyncio
async def test_login_invalid_password():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        res = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "wrongpassword"})
        assert res.status_code == 401

@pytest.mark.asyncio
async def test_rbac_restriction_manager_cannot_create_delivery():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        login_res = await client.post("/api/v1/auth/login", json={"username": "manager", "password": "manager123"})
        token = login_res.json()["access_token"]
        
        headers = {"Authorization": f"Bearer {token}"}
        res = await client.post("/api/v1/deliveries", json={"customer_id": "cust1", "items": []}, headers=headers)
        assert res.status_code == 403  # Forbidden for INVENTORY_MANAGER
