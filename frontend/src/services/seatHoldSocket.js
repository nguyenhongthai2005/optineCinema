// src/services/seatHoldSocket.js
// Tham khảo: AstraCine seatHoldSocket.js

import { Client } from '@stomp/stompjs';
import { WS_BASE_URL } from './api';

const WS_URL = WS_BASE_URL;

/**
 * Kết nối STOMP và subscribe seat events cho 1 suất chiếu.
 *
 * @param {string|number} showtimeId
 * @param {(event: { type: 'SEAT_HELD'|'SEAT_RELEASED'|'SEAT_SOLD', showtimeId: number, seatIds: number[], byUserId: number|null, expiresAt: number|null }) => void} onMessage
 * @returns {() => void} cleanup function — gọi khi unmount
 */
export function connectSeatSocket(showtimeId, onMessage) {
  const client = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 3000,
    debug: (str) => {
      // Bật khi cần debug:
      // console.log('[STOMP]', str);
    },
  });

  client.onWebSocketError = (e) => console.log('[WS ERROR]', e);
  client.onWebSocketClose = (e) => console.log('[WS CLOSE]', e);

  client.onConnect = () => {
    client.subscribe(`/topic/showtimes/${showtimeId}/seats`, (message) => {
      try {
        onMessage?.(JSON.parse(message.body));
      } catch (e) {
        console.error('Bad WS message:', message.body, e);
      }
    });
  };

  client.onStompError = (frame) => {
    console.error('[STOMP ERROR]', frame.headers['message'], frame.body);
  };

  client.activate();

  // Cleanup: deactivate khi component unmount
  return () => {
    client.deactivate();
  };
}
