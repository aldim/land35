import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// Динамически определяем URL WebSocket сервера на основе текущего хоста
// Это позволяет работать как с localhost, так и с IP адресами в локальной сети
const getWebSocketUrl = () => {
  const host = window.location.hostname;
  const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
  // Бэкенд работает на порту 8080
  const wsUrl = `${protocol}//${host}:8080/ws`;
  console.log('WebSocket URL:', wsUrl);
  return wsUrl;
};

// Используем переменную окружения только если она явно задана, иначе используем динамическое определение
// Это важно для работы с разных устройств (localhost, IP адреса в локальной сети)
const getSocketUrl = () => {
  const envUrl = process.env.REACT_APP_WS_URL;
  if (envUrl && typeof envUrl === 'string' && envUrl.trim() !== '') {
    return envUrl;
  }
  return getWebSocketUrl();
};

const SOCKET_URL = getSocketUrl();

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = new Map();
    this.connected = false;
    this.onConnectCallbacks = [];
    this.onDisconnectCallbacks = [];
  }

  connect() {
    return new Promise((resolve, reject) => {
      if (this.connected) {
        resolve();
        return;
      }

      this.client = new Client({
        webSocketFactory: () => new SockJS(SOCKET_URL),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: (str) => {
          console.log('STOMP: ' + str);
        },
        onConnect: () => {
          console.log('WebSocket connected');
          this.connected = true;
          this.onConnectCallbacks.forEach(cb => cb());
          resolve();
        },
        onDisconnect: () => {
          console.log('WebSocket disconnected');
          this.connected = false;
          this.onDisconnectCallbacks.forEach(cb => cb());
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          reject(new Error('STOMP error'));
        }
      });

      this.client.activate();
    });
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
      this.subscriptions.clear();
    }
  }

  subscribe(destination, callback) {
    if (!this.client || !this.connected) {
      console.error('Not connected to WebSocket');
      return null;
    }

    const subscription = this.client.subscribe(destination, (message) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set(destination, subscription);
    return subscription;
  }

  unsubscribe(destination) {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
    }
  }

  send(destination, body) {
    if (!this.client || !this.connected) {
      console.error('Not connected to WebSocket');
      return;
    }

    this.client.publish({
      destination: destination,
      body: JSON.stringify(body)
    });
  }

  // Методы для игры
  createRoom(userId, forceNew = false) {
    this.send('/app/create-room', { userId, forceNew });
  }

  addPlayer(roomCode, playerName, avatar) {
    this.send('/app/add-player', { roomCode, playerName, avatar });
  }

  joinRoom(roomCode, playerId) {
    this.send('/app/join-room', { roomCode, playerId });
  }

  removePlayer(roomCode, playerId) {
    this.send('/app/remove-player', { roomCode, playerId });
  }

  startRound(roomCode) {
    this.send('/app/start-round', { roomCode });
  }

  pressButton(roomCode, playerId) {
    const clientTimestamp = Date.now(); // Фиксируем время нажатия на клиенте
    this.send('/app/press-button', { 
      roomCode, 
      playerId,
      clientTimestamp  // Добавляем timestamp клиента
    });
  }

  resetRound(roomCode) {
    this.send('/app/reset-round', { roomCode });
  }

  getRoomState(roomCode) {
    this.send('/app/get-room-state', { roomCode });
  }

  stunPlayer(roomCode, playerId) {
    this.send('/app/stun-player', { roomCode, playerId });
  }

  updateChapter(roomCode, chapter, part) {
    this.send('/app/update-chapter', { roomCode, chapter, part });
  }

  onConnect(callback) {
    this.onConnectCallbacks.push(callback);
  }

  onDisconnect(callback) {
    this.onDisconnectCallbacks.push(callback);
  }

  isConnected() {
    return this.connected;
  }
}

const websocketService = new WebSocketService();
export default websocketService;


