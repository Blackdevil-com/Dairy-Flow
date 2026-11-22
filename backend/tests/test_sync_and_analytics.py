import pytest
from httpx import AsyncClient, ASGITransport
from datetime import datetime
from app.main import app

@pytest.mark.asyncio
async def test_sync_pull_and_push_idempotent():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        owner_login = await client.post("/api/v1/auth/login", json={"username": "owner", "password": "owner123"})
        token = owner_login.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}
        
        # Test Sync Pull
        pull_res = await client.get("/api/v1/sync/changes?device_id=ANDROID-DEVICE-TEST-001", headers=headers)
        assert pull_res.status_code == 200
        pull_data = pull_res.json()
        assert "products" in pull_data
        assert "batches" in pull_data
        assert "customers" in pull_data
        
        # Test Sync Push (Offline outbox item)
        push_payload = {
            "device_id": "ANDROID-DEVICE-TEST-001",
            "items": [
                {
                    "operation_id": "OP-SYNC-TEST-9999",
                    "entity_type": "CUSTOMER",
                    "action": "CREATE",
                    "payload": {
                        "name": "Offline Synced Groceries",
                        "trade_name": "Synced Groceries",
                        "customer_type": "RETAIL_SHOP",
                        "phone": "+91 9999888877",
                        "credit_limit": 30000.00
                    },
                    "client_timestamp": datetime.utcnow().isoformat()
                }
            ]
        }
        push_res = await client.post("/api/v1/sync/push", json=push_payload, headers=headers)
        assert push_res.status_code == 200
        results = push_res.json()["results"]
        assert results[0]["status"] == "SUCCESS", results[0]["message"]
        
        # Duplicate Push of same operation_id should be ignored idempotently
        push_dup_res = await client.post("/api/v1/sync/push", json=push_payload, headers=headers)
        assert push_dup_res.status_code == 200
        dup_results = push_dup_res.json()["results"]
        assert dup_results[0]["status"] == "DUPLICATE_IGNORED"
