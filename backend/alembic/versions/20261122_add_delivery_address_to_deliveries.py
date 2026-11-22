"""add_delivery_address_to_deliveries

Revision ID: d4e8a9123456
Revises: c8f9e1234567
Create Date: 2026-11-22 16:15:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'd4e8a9123456'
down_revision: Union[str, None] = 'c8f9e1234567'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    with op.batch_alter_table('deliveries', schema=None) as batch_op:
        batch_op.add_column(sa.Column('delivery_address', sa.Text(), nullable=True))


def downgrade() -> None:
    with op.batch_alter_table('deliveries', schema=None) as batch_op:
        batch_op.drop_column('delivery_address')
