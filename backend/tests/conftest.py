import pytest
from app.db.base import Base
from app.db.session import engine, AsyncSessionLocal
from app.db.seed import seed_initial_data

@pytest.fixture(autouse=True)
async def reset_db_per_test():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
        await conn.run_sync(Base.metadata.create_all)
    async with AsyncSessionLocal() as session:
        await seed_initial_data(session)
    yield
