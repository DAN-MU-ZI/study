# Gunicorn configuration for FastAPI with Uvicorn workers

# Number of worker processes
workers = 4

# Worker class for async support
worker_class = "uvicorn.workers.UvicornWorker"

# Bind address
bind = "0.0.0.0:8000"

# Timeout for worker processes
timeout = 120

# Access log
accesslog = "-"

# Error log
errorlog = "-"

# Log level
loglevel = "info"
