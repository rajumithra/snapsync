import json
import logging
from typing import Dict, List, Any
from fastapi import APIRouter, WebSocket, WebSocketDisconnect

logger = logging.getLogger("snapsync.ws")

router = APIRouter()

class ConnectionManager:
    def __init__(self):
        # Maps event_id -> List[WebSocket]
        self.active_rooms: Dict[str, List[WebSocket]] = {}

    async def connect(self, websocket: WebSocket, event_id: str):
        await websocket.accept()
        if event_id not in self.active_rooms:
            self.active_rooms[event_id] = []
        self.active_rooms[event_id].append(websocket)
        logger.info(f"WebSocket client connected to event room {event_id}. Total: {len(self.active_rooms[event_id])}")

    def disconnect(self, websocket: WebSocket, event_id: str):
        if event_id in self.active_rooms:
            if websocket in self.active_rooms[event_id]:
                self.active_rooms[event_id].remove(websocket)
            if not self.active_rooms[event_id]:
                del self.active_rooms[event_id]
        logger.info(f"WebSocket client disconnected from event room {event_id}")

    async def broadcast_to_event(self, event_id: str, message: Dict[str, Any]):
        if event_id in self.active_rooms:
            dead_sockets = []
            payload = json.dumps(message)
            for ws in self.active_rooms[event_id]:
                try:
                    await ws.send_text(payload)
                except Exception as e:
                    logger.warning(f"Error sending message to websocket: {e}")
                    dead_sockets.append(ws)

            for dead in dead_sockets:
                self.disconnect(dead, event_id)

ws_manager = ConnectionManager()

@router.websocket("/ws/events/{event_id}")
async def event_websocket_endpoint(websocket: WebSocket, event_id: str):
    await ws_manager.connect(websocket, event_id)
    try:
        while True:
            # Keep alive and receive client pings
            data = await websocket.receive_text()
            # Can respond with ack or ping-pong
            if data == "ping":
                await websocket.send_text(json.dumps({"type": "pong"}))
    except WebSocketDisconnect:
        ws_manager.disconnect(websocket, event_id)
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
        ws_manager.disconnect(websocket, event_id)

