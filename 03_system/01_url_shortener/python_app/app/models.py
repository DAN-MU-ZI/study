from sqlalchemy import String, DateTime
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.sql import func
from app.database import Base

class URLMapping(Base):
    __tablename__ = "url_mappings"

    short_url: Mapped[str] = mapped_column(String, primary_key=True, index=True)
    long_url: Mapped[str] = mapped_column(String, index=True, nullable=False)
    created_at: Mapped[DateTime] = mapped_column(DateTime(timezone=True), server_default=func.now())
