import logging
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine

logger = logging.getLogger(__name__)

CUSTOMER_NEW_COLUMNS = [
    ("contact_person", "VARCHAR(100)"),
    ("alternate_phone", "VARCHAR(30)"),
    ("billing_address", "TEXT"),
    ("delivery_address", "TEXT"),
    ("area", "VARCHAR(100)"),
    ("city", "VARCHAR(100)"),
    ("district", "VARCHAR(100)"),
    ("postal_code", "VARCHAR(20)"),
    ("gst_registration_status", "VARCHAR(30) DEFAULT 'UNKNOWN'"),
    ("credit_enabled", "BOOLEAN DEFAULT 0"),
    ("payment_terms_days", "INTEGER DEFAULT 0"),
    ("notes", "TEXT"),
]

DELIVERY_NEW_COLUMNS = [
    ("delivery_address", "TEXT"),
]

async def apply_customer_schema_updates(engine: AsyncEngine):
    """
    Ensures missing columns in customers and deliveries tables are added dynamically to
    existing databases (SQLite/Postgres) without dropping tables or losing data.
    """
    async with engine.begin() as conn:
        dialect = engine.dialect.name
        
        # 1. Update customers table
        existing_cols = set()
        if dialect == "sqlite":
            res = await conn.execute(text("PRAGMA table_info(customers)"))
            for row in res.fetchall():
                existing_cols.add(row[1])
        elif dialect == "postgresql":
            res = await conn.execute(text(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'customers'"
            ))
            for row in res.fetchall():
                existing_cols.add(row[0])

        if existing_cols:
            for col_name, col_type in CUSTOMER_NEW_COLUMNS:
                if col_name not in existing_cols:
                    logger.info(f"Adding missing column '{col_name}' to customers table...")
                    try:
                        await conn.execute(text(f"ALTER TABLE customers ADD COLUMN {col_name} {col_type}"))
                    except Exception as e:
                        logger.warning(f"Column addition note for customers.{col_name}: {e}")

        # 2. Update deliveries table
        existing_deliv_cols = set()
        if dialect == "sqlite":
            res = await conn.execute(text("PRAGMA table_info(deliveries)"))
            for row in res.fetchall():
                existing_deliv_cols.add(row[1])
        elif dialect == "postgresql":
            res = await conn.execute(text(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'deliveries'"
            ))
            for row in res.fetchall():
                existing_deliv_cols.add(row[0])

        if existing_deliv_cols:
            for col_name, col_type in DELIVERY_NEW_COLUMNS:
                if col_name not in existing_deliv_cols:
                    logger.info(f"Adding missing column '{col_name}' to deliveries table...")
                    try:
                        await conn.execute(text(f"ALTER TABLE deliveries ADD COLUMN {col_name} {col_type}"))
                    except Exception as e:
                        logger.warning(f"Column addition note for deliveries.{col_name}: {e}")
