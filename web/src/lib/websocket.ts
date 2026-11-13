import { useEffect, useRef, useState } from "react";
import { getWsBaseUrl, WS_BASE_URL } from "./api";

export interface WebSocketMessage {
  type: string;
  event_id: string;
  data: any;
}

export function useEventWebSocket(eventId: string | null, onMessage?: (msg: WebSocketMessage) => void) {
  const [isConnected, setIsConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    if (!eventId) return;

    const wsUrl = `${getWsBaseUrl()}/events/${eventId}`;
    let reconnectTimeout: NodeJS.Timeout;

    function connect() {
      try {
        const socket = new WebSocket(wsUrl);
        wsRef.current = socket;

        socket.onopen = () => {
          setIsConnected(true);
        };

        socket.onmessage = (event) => {
          try {
            const data: WebSocketMessage = JSON.parse(event.data);
            if (onMessage) {
              onMessage(data);
            }
          } catch (e) {
            // Ignore non-json ping/pong
          }
        };

        socket.onclose = () => {
          setIsConnected(false);
          // Reconnect after 3 seconds
          reconnectTimeout = setTimeout(connect, 3000);
        };

        socket.onerror = () => {
          socket.close();
        };
      } catch (err) {
        reconnectTimeout = setTimeout(connect, 3000);
      }
    }

    connect();

    return () => {
      clearTimeout(reconnectTimeout);
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [eventId]);

  return { isConnected };
}

