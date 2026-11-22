
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
    ("gst_registration_status", "VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN'"),
    ("credit_enabled", "BOOLEAN NOT NULL DEFAULT FALSE"),
    ("payment_terms_days", "INTEGER NOT NULL DEFAULT 0"),
    ("notes", "TEXT"),
]

DELIVERY_NEW_COLUMNS = [
    ("delivery_address", "TEXT"),
]


async def get_table_columns(conn, dialect, table_name):
    """Return existing column names for a table."""

    if dialect == "sqlite":
        result = await conn.execute(
            text(f"PRAGMA table_info({table_name})")
        )
        return {row[1] for row in result.fetchall()}

    if dialect == "postgresql":
        result = await conn.execute(
            text("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                AND table_name = :table_name
            """),
            {"table_name": table_name},
        )
        return {row[0] for row in result.fetchall()}

    raise RuntimeError(f"Unsupported database dialect: {dialect}")


async def migrate_table(conn, dialect, table_name, new_columns):
    """Add missing columns without dropping existing data."""

    existing_columns = await get_table_columns(
        conn, dialect, table_name
    )

    if not existing_columns:
        raise RuntimeError(
            f"Required table '{table_name}' does not exist."
        )

    for column_name, column_type in new_columns:
        if column_name in existing_columns:
            continue

        logger.info(
            "Adding missing column %s.%s",
            table_name,
            column_name,
        )

        await conn.execute(
            text(
                f"ALTER TABLE {table_name} "
                f"ADD COLUMN {column_name} {column_type}"
            )
        )

        existing_columns.add(column_name)

    # Verify that every required column now exists.
    final_columns = await get_table_columns(
        conn, dialect, table_name
    )

    missing = [
        name for name, _ in new_columns
        if name not in final_columns
    ]

    if missing:
        raise RuntimeError(
            f"Migration incomplete for {table_name}: {missing}"
        )

    logger.info("Schema verified for table '%s'.", table_name)


async def apply_customer_schema_updates(engine: AsyncEngine):
    """
    Safely upgrade existing customer and delivery schemas.

    Existing records are preserved. Repeated execution is safe.
    """

    dialect = engine.dialect.name

    async with engine.begin() as conn:
        await migrate_table(
            conn,
            dialect,
            "customers",
            CUSTOMER_NEW_COLUMNS,
        )

        await migrate_table(
            conn,
            dialect,
            "deliveries",
            DELIVERY_NEW_COLUMNS,
        )

    logger.info("Database schema migration completed successfully.")
