import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.core.config import settings
from app.core.logging import setup_logging, logger
from app.api.v1.api import api_router
from app.db.base import Base
from app.db.session import engine, AsyncSessionLocal
from app.db.seed import seed_initial_data

setup_logging()

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting DairyFlow Application Server...")
    os.makedirs("static/invoices", exist_ok=True)
    
    # Initialize DB schema
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
        
    # Seed initial users, warehouse, units, and products
    async with AsyncSessionLocal() as session:
        await seed_initial_data(session)
        
    logger.info("DairyFlow DB schema created and seeded.")
    yield
    logger.info("Shutting down DairyFlow Application Server...")

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url=f"{settings.API_V1_STR}/docs",
    redoc_url=f"{settings.API_V1_STR}/redoc",
    lifespan=lifespan
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount static for PDF invoice files
os.makedirs("static/invoices", exist_ok=True)
app.mount("/static", StaticFiles(directory="static"), name="static")

# Mount API v1 router
app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "app": "DairyFlow",
        "version": settings.VERSION,
        "gst_mode": settings.GST_STATUS
    }
