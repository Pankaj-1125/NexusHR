import { Client } from "@stomp/stompjs";

function getSavedUser() {
  try {
    const value =
      window.localStorage.getItem(
        "nexushr-user"
      );

    return value
      ? JSON.parse(value)
      : null;
  } catch {
    return null;
  }
}

function websocketUrl() {
  if (import.meta.env.VITE_WS_URL) {
    return import.meta.env.VITE_WS_URL;
  }

  const protocol =
    window.location.protocol === "https:"
      ? "wss:"
      : "ws:";

  return `${protocol}//${window.location.host}/ws`;
}

export function connectRealtimeNotifications(
  onNotification,
  onConnectionChange
) {
  const user = getSavedUser();
  const token = user?.token;

  if (!token) {
    return () => {};
  }

  const client = new Client({
    brokerURL: websocketUrl(),

    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },

    reconnectDelay: 3000,

    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,

    onConnect: () => {
      onConnectionChange?.(true);

      client.subscribe(
        "/user/queue/notifications",
        (message) => {
          try {
            const notification =
              JSON.parse(message.body);

            onNotification?.(
              notification
            );
          } catch (error) {
            console.error(
              "Invalid real-time notification:",
              error
            );
          }
        }
      );
    },

    onDisconnect: () => {
      onConnectionChange?.(false);
    },

    onWebSocketClose: () => {
      onConnectionChange?.(false);
    },

    onWebSocketError: (error) => {
      console.error(
        "NexusHR WebSocket error:",
        error
      );
    },

    onStompError: (frame) => {
      console.error(
        "NexusHR STOMP error:",
        frame.headers?.message,
        frame.body
      );
    },
  });

  client.activate();

  return () => {
    client.deactivate();
  };
}