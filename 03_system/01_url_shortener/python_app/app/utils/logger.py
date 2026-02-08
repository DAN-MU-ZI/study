
import json
import logging
import time
from typing import Any, Dict

class JSONFormatter(logging.Formatter):
    def format(self, record):
        log_record = {
            "timestamp": self.formatTime(record, self.datefmt),
            "level": record.levelname,
            "message": record.getMessage(),
            "name": record.name,
        }
        
        # Add extra fields if they exist
        if hasattr(record, "request_id"):
            log_record["request_id"] = record.request_id
        if hasattr(record, "event"):
            log_record["event"] = record.event
        if hasattr(record, "duration_ms"):
            log_record["duration_ms"] = record.duration_ms
        if hasattr(record, "metadata"):
            log_record["metadata"] = record.metadata

        return json.dumps(log_record)

def setup_logger(name: str = "app", log_file: str = "app.log", level: int = logging.INFO) -> logging.Logger:
    logger = logging.getLogger(name)
    logger.setLevel(level)
    
    # Avoid adding multiple handlers if logger is already configured
    if not logger.handlers:
        # File Handler
        file_handler = logging.FileHandler(log_file)
        file_handler.setFormatter(JSONFormatter())
        logger.addHandler(file_handler)
        
        # Console Handler (Optional, for Docker logs)
        console_handler = logging.StreamHandler()
        console_handler.setFormatter(JSONFormatter())
        logger.addHandler(console_handler)
        
    return logger

# Global logger instance
logger = setup_logger()
