from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
import time
import asyncio

app = FastAPI()

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # React 프론트엔드가 위치한 도메인을 허용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# 클라이언트 관리용 클래스
class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)

    async def send_message(self, message: str, websocket: WebSocket):
        await websocket.send_text(message)

    async def broadcast(self, message: str):
        for connection in self.active_connections:
            await connection.send_text(message)


manager = ConnectionManager()


# 웹소켓을 통해 프로세스 진행 상황 전송
@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        steps = [
            "Step 1: Initializing",
            "Step 2: Processing",
            "Step 3: Finalizing",
            "Completed",
        ]

        for step in steps:
            await manager.send_message(step, websocket)
            await asyncio.sleep(1)  # 각 단계를 1초마다 시뮬레이션
    except WebSocketDisconnect:
        manager.disconnect(websocket)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
