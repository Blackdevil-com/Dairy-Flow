# DairyFlow — Dairy Inventory, Distribution & Billing Management

DairyFlow is a client-ready, offline-first application engineered for dairy processing plants, cold-storage depots, and milk distribution businesses.

---

## 1. System Architecture

DairyFlow consists of two primary subsystems:
1. **FastAPI Modular Monolithic Backend**: Authoritative source for inventory calculations, signed ledger movements, FEFO batch allocations, sequence-safe billing, partial payment allocations, and PDF generation with ReportLab.
2. **Native Android Application**: Jetpack Compose (Material 3), MVVM, Navigation Compose, Room local database cache, and WorkManager background outbox synchronization.

### Business Roles:
- **OWNER / ADMIN**: Works outside the warehouse. Views available stock, manages retail shop and individual customer accounts, drafts deliveries, confirms dispatches with atomic inventory deductions, generates sequentially numbered invoices, records multi-channel payments, and analyzes financial year reports.
- **INVENTORY_MANAGER**: Works inside the warehouse. Manages products, per-product conversion units, receives incoming shipments with batch numbers and expiry dates, logs manual adjustments, writes off damaged/expired stock, tracks the signed movement ledger, and monitors low-stock alerts.

---

## 2. Technical Stack

- **Backend**: Python 3.13, FastAPI, Pydantic v2, SQLAlchemy 2 (Async), Alembic, ReportLab, Pytest.
- **Database**: PostgreSQL (Production) / SQLite with aiosqlite (Local zero-setup development). 24 normalized tables with NUMERIC precision, foreign keys, and indexes.
- **Android**: Kotlin, Jetpack Compose, Material 3, MVVM, Retrofit, Room, WorkManager, Coroutines.
- **Authentication**: JWT, refresh tokens, server-side RBAC (`require_roles(["OWNER"])`, `require_roles(["INVENTORY_MANAGER", "OWNER"])`).

---

## 3. Core Workflows & Business Logic

### 3.1 Product-Specific Unit Conversions
Mixed units (Litres, Kilograms, Packets, Crates) are **never globally assumed**. Every product specifies its canonical base unit (e.g. Milk $\to$ `LTR`, Ghee $\to$ `KG`) and conversion multipliers are strictly looked up in `product_units`:
$$\text{Base Quantity} = \text{Quantity} \times \text{conversion\_to\_base}$$
*Example:* 1 Crate of 500ml Cow Milk = 12 Litres; 1 Crate of 200g Paneer = 10 kg.

### 3.2 FEFO Batch Allocation & Atomic Dispatches
When an owner confirms a delivery run:
1. Validates idempotency key to prevent double dispatches.
2. Row-locks candidate batches (`available_base_qty > 0`) ordered by `expiry_date ASC, mfg_date ASC, id ASC`.
3. Verifies total stock availability. If insufficient, rolls back immediately with HTTP 400.
4. Decrements batch balances and writes negative entries into the immutable `stock_movements` ledger.
5. Issues sequential commercial receipt/invoice (`DF/2026-27/0001`).
6. All operations are committed within a single PostgreSQL transaction.

### 3.3 GST Compliance & Demo Watermarking
- Business registration status defaults to `UNVERIFIED_DEMO`.
- Invoices are generated as `COMMERCIAL_RECEIPT` and explicitly watermarked:
  $$\textbf{DEMO - NOT A TAX INVOICE}$$
- Prevents issuance of premature tax documents until GSTIN and HSN classifications are legally verified by a tax practitioner.

### 3.4 Multi-Channel Payments & FIFO Allocation
- Supports `CASH`, `UPI`, `BANK_TRANSFER`, and `OTHER`.
- Payments are automatically allocated across open customer invoices using FIFO (oldest unpaid invoice first), updating `payment_status` (`UNPAID` $\to$ `PARTIAL` $\to$ `PAID`) and deducting from the customer's current balance.

---

## 4. Quick Start (Local Development)

### 4.1 Backend Setup
```bash
cd backend
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements.txt

# Run database migrations and seed realistic demo data
alembic upgrade head
python -c "import asyncio; from app.db.session import engine, AsyncSessionLocal; from app.db.base import Base; from app.db.seed import seed_initial_data; asyncio.run(seed_initial_data(AsyncSessionLocal()))"

# Start FastAPI server on port 8080
uvicorn app.main:app --host 0.0.0.0 --port 8080 --reload
```
Interactive Swagger API documentation: `http://localhost:8080/api/v1/docs`.

### Default Demo Credentials:
- **Owner**: `username: owner` | `password: owner123`
- **Warehouse Manager**: `username: manager` | `password: manager123`

### 4.2 Running Backend Tests
```bash
cd backend
.\venv\Scripts\pytest -v
```
All 12 integration tests verify the complete acceptance workflow, FEFO deduction, rollback, RBAC, and idempotency.

### 4.3 Android Setup
1. Open `/android` in Android Studio or build via Gradle:
```bash
cd android
gradle assembleDebug
```
2. If running on Android Emulator, the app connects to `http://10.0.2.2:8080/api/v1/`. For physical devices on local Wi-Fi, tap "Backend Connection Settings" on the login screen and enter your machine's LAN IP (e.g. `http://192.168.1.50:8080/api/v1/`).

---

## 5. Deployment Guide (AWS EC2)

1. Provision an Ubuntu 24.04 EC2 instance (`t3.medium`).
2. Install PostgreSQL 16/17 and Nginx:
```bash
sudo apt update && sudo apt install -y python3-pip python3-venv postgresql nginx
```
3. Create PostgreSQL user and database:
```sql
CREATE DATABASE dairyflow_db;
CREATE USER dairyflow_user WITH ENCRYPTED PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE dairyflow_db TO dairyflow_user;
```
4. Set `.env` on EC2:
```ini
DATABASE_URL=postgresql+asyncpg://dairyflow_user:your_secure_password@localhost:5432/dairyflow_db
SECRET_KEY=generate_high_entropy_production_secret
```
5. Run migrations: `alembic upgrade head`.
6. Run FastAPI with systemd service and Gunicorn/Uvicorn workers. Configure Nginx with SSL (Let's Encrypt).
