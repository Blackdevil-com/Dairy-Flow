from datetime import date
from typing import Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_db
from app.schemas.analytics import DailySummaryResponse, MonthlySummaryResponse, YearlySummaryResponse
from app.services.analytics_service import get_daily_summary, get_monthly_summary, get_yearly_summary
from app.api.deps import require_roles, User

router = APIRouter()

@router.get("/summary", response_model=DailySummaryResponse)
async def daily_analytics(
    target_date: Optional[date] = None,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    dt = target_date or date.today()
    data = await get_daily_summary(db, dt)
    return data


@router.get("/monthly", response_model=MonthlySummaryResponse)
async def monthly_analytics(
    year: int = Query(default=2026),
    month: int = Query(default=11),
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    data = await get_monthly_summary(db, year, month)
    return data


@router.get("/yearly", response_model=YearlySummaryResponse)
async def yearly_analytics(
    fy: Optional[str] = Query(default=None, description="e.g. 2026-27"),
    db: AsyncSession = Depends(get_db),
    user: User = Depends(require_roles(["OWNER"]))
):
    data = await get_yearly_summary(db, fy)
    return data
