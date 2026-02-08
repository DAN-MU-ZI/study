from sqlalchemy import Column, Integer, String, Text
from app.database import Base

class UrlMapping(Base):
    __tablename__ = "url_mappings"

    id = Column(Integer, primary_key=True, index=True)
    short_url = Column(String(7), unique=True, index=True, nullable=False)
    long_url = Column(Text, index=True, nullable=False)
