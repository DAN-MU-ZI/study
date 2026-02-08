
import time
import logging
from contextlib import contextmanager
from typing import Optional
from app.utils.logger import logger

@contextmanager
def Timer(name: str, request_id: Optional[str] = None):
    """
    Context manager to measure execution time of a code block.
    
    Args:
        name (str): The name of the operation being measured (e.g., "db_query", "redis_get").
        request_id (str, optional): The request ID associated with this operation.
    """
    start_time = time.perf_counter()
    try:
        yield
    finally:
        end_time = time.perf_counter()
        duration_ms = (end_time - start_time) * 1000
        
        extra = {
            "event": name,
            "duration_ms": round(duration_ms, 3)
        }
        if request_id:
            extra["request_id"] = request_id
            
        logger.info(f"Finished {name}", extra=extra)
