
import time
import uuid
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint
from starlette.requests import Request
from starlette.responses import Response
from app.utils.logger import logger
from app.utils.timer import Timer

class PerformanceMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        request_id = str(uuid.uuid4())
        request.state.request_id = request_id
        
        start_time = time.perf_counter()
        
        # Log request start
        logger.info(
            f"Request Start: {request.method} {request.url.path}",
            extra={
                "event": "request_start",
                "request_id": request_id,
                "metadata": {
                    "method": request.method,
                    "url": str(request.url),
                    "client": request.client.host if request.client else None
                }
            }
        )
        
        try:
            response = await call_next(request)
            
            end_time = time.perf_counter()
            process_time = (end_time - start_time) * 1000
            
            # Add process time to header
            response.headers["X-Process-Time"] = str(process_time)
            
            # Log request end with total duration
            logger.info(
                f"Request End: {response.status_code}",
                extra={
                    "event": "request_end",
                    "request_id": request_id,
                    "duration_ms": round(process_time, 3),
                    "metadata": {
                        "status_code": response.status_code
                    }
                }
            )
            
            return response
            
        except Exception as e:
            end_time = time.perf_counter()
            process_time = (end_time - start_time) * 1000
            
            logger.error(
                f"Request Failed: {str(e)}",
                extra={
                    "event": "request_failed",
                    "request_id": request_id,
                    "duration_ms": round(process_time, 3),
                    "metadata": {
                        "error": str(e)
                    }
                }
            )
            raise e
