"""add_customer_management_fields

Revision ID: c8f9e1234567
Revises: 00560605fa32
Create Date: 2026-11-22 15:35:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'c8f9e1234567'
down_revision: Union[str, None] = '00560605fa32'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Add new customer fields safely
    with op.batch_alter_table('customers', schema=None) as batch_op:
        batch_op.add_column(sa.Column('contact_person', sa.String(length=100), nullable=True))
        batch_op.add_column(sa.Column('alternate_phone', sa.String(length=30), nullable=True))
        batch_op.add_column(sa.Column('billing_address', sa.Text(), nullable=True))
        batch_op.add_column(sa.Column('delivery_address', sa.Text(), nullable=True))
        batch_op.add_column(sa.Column('area', sa.String(length=100), nullable=True))
        batch_op.add_column(sa.Column('city', sa.String(length=100), nullable=True))
        batch_op.add_column(sa.Column('district', sa.String(length=100), nullable=True))
        batch_op.add_column(sa.Column('postal_code', sa.String(length=20), nullable=True))
        batch_op.add_column(sa.Column('gst_registration_status', sa.String(length=30), server_default='UNKNOWN', nullable=False))
        batch_op.add_column(sa.Column('credit_enabled', sa.Boolean(), server_default='0', nullable=False))
        batch_op.add_column(sa.Column('payment_terms_days', sa.Integer(), server_default='0', nullable=False))
        batch_op.add_column(sa.Column('notes', sa.Text(), nullable=True))


def downgrade() -> None:
    with op.batch_alter_table('customers', schema=None) as batch_op:
        batch_op.drop_column('notes')
        batch_op.drop_column('payment_terms_days')
        batch_op.drop_column('credit_enabled')
        batch_op.drop_column('gst_registration_status')
        batch_op.drop_column('postal_code')
        batch_op.drop_column('district')
        batch_op.drop_column('city')
        batch_op.drop_column('area')
        batch_op.drop_column('delivery_address')
        batch_op.drop_column('billing_address')
        batch_op.drop_column('alternate_phone')
        batch_op.drop_column('contact_person')
