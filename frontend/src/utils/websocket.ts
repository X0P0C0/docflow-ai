import { ref, onMounted, onUnmounted } from "vue";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs.min.js";
import { getToken } from "./auth";

export interface WsNotification {
  type: string;
  title: string;
  content: string;
  timestamp: number;
}

const notifications = ref<WsNotification[]>([]);
const connected = ref(false);
let stompClient: Client | null = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 3;

export function useWebSocket() {
  function connect() {
    const token = getToken();
    if (!token) return;

    // Skip if already connected or max attempts reached
    if (connected.value || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return;

    const tokenStr = token?.accessToken || "";

    stompClient = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      connectHeaders: { Authorization: `Bearer ${tokenStr}` },
      reconnectDelay: 0,  // Don't auto-reconnect
      connectionTimeout: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        connected.value = true;
        reconnectAttempts = 0;
        
        // Subscribe to user-specific notifications
        try {
          const userId = tokenStr ? JSON.parse(atob(tokenStr.split(".")[1])).userId || "0" : "0";
          stompClient?.subscribe(`/user/${userId}/queue/notifications`, (message) => {
            try {
              const notification: WsNotification = JSON.parse(message.body);
              notifications.value.unshift(notification);
              if (notifications.value.length > 50) {
                notifications.value = notifications.value.slice(0, 50);
              }
            } catch (e) {
              console.debug("Failed to parse notification:", e);
            }
          });
          
          // Subscribe to broadcast
          stompClient?.subscribe("/topic/broadcast", (message) => {
            try {
              const notification: WsNotification = JSON.parse(message.body);
              notifications.value.unshift(notification);
            } catch (e) {
              console.debug("Failed to parse broadcast:", e);
            }
          });
        } catch (e) {
          console.debug("Failed to subscribe:", e);
        }
      },
      onDisconnect: () => {
        connected.value = false;
      },
      onStompError: (frame) => {
        connected.value = false;
        reconnectAttempts++;
        console.debug(`WebSocket error (attempt ${reconnectAttempts}):`, frame.headers?.message || "connection failed");
      }
    });

    try {
      stompClient.activate();
    } catch (e) {
      console.debug("Failed to activate WebSocket:", e);
      reconnectAttempts++;
    }
  }

  function disconnect() {
    if (stompClient) {
      try {
        stompClient.deactivate();
      } catch (e) {
        // Ignore errors on disconnect
      }
    }
    connected.value = false;
    reconnectAttempts = 0;
  }

  function clearNotifications() {
    notifications.value = [];
  }

  return {
    notifications,
    connected,
    connect,
    disconnect,
    clearNotifications
  };
}
