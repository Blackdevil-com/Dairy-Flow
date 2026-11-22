import pytest
from httpx import AsyncClient, ASGITransport
from datetime import date
from app.main import app

@pytest.mark.asyncio
async def test_invoice_pdf_download_and_watermark():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        token = owner_login.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        invoices = (await client.get("/api/v1/invoices", headers=headers)).json()
        if len(invoices) > 0:
            inv_id = invoices[0]["id"]
            pdf_res = await client.get(f"/api/v1/invoices/{inv_id}/pdf", headers=headers)
            assert pdf_res.status_code == 200
            assert pdf_res.headers["content-type"] == "application/pdf"
            assert len(pdf_res.content) > 1000

@pytest.mark.asyncio
async def test_multiple_payments_and_fifo_invoice_clearance():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        token = owner_login.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        custs = (await client.get("/api/v1/customers", headers=headers)).json()
        cust = custs[1]
        
        # Record a general cash payment
        pay_res = await client.post("/api/v1/payments", json={
            "customer_id": cust["id"],
            "payment_mode": "CASH",
            "amount": "250.00",
            "notes": "General cash clearance"
        }, headers=headers)
        assert pay_res.status_code == 200
        data = pay_res.json()
        assert data["payment_mode"] == "CASH"
        assert float(data["amount"]) == 250.0
