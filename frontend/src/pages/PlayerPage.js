import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import websocketService from '../services/websocket';
import AvatarDisplay from '../components/AvatarDisplay';

function PlayerPage() {
  const { roomCode, playerId } = useParams();
  const [connected, setConnected] = useState(false);
  const [player, setPlayer] = useState(null);
  const [gameState, setGameState] = useState('WAITING');
  const [isWinner, setIsWinner] = useState(false);
  const [hasPressed, setHasPressed] = useState(false);
  const [error, setError] = useState(null);
  const [winnerId, setWinnerId] = useState(null);

  const handleMessage = useCallback((message) => {
    console.log('Player received message:', message);
    
    switch (message.type) {
      case 'ROOM_STATE':
        setGameState(message.gameState);
        if (message.winnerId) {
          setWinnerId(message.winnerId);
          setIsWinner(message.winnerId === playerId);
          setHasPressed(true);
        }
        // Найти себя в списке игроков
        const me = message.players?.find(p => p.id === playerId);
        if (me) {
          setPlayer(me);
        }
        break;
      case 'PLAYER_JOINED':
        setGameState(message.gameState);
        const player = message.players?.find(p => p.id === playerId);
        if (player) {
          setPlayer(player);
        }
        break;
      case 'ROUND_STARTED':
        setGameState('ACTIVE');
        setIsWinner(false);
        setHasPressed(false);
        setWinnerId(null);
        break;
      case 'BUTTON_PRESSED':
        setGameState(message.gameState);
        if (message.winnerId) {
          setWinnerId(message.winnerId);
          setIsWinner(message.winnerId === playerId);
        }
        // Проверяем, нажимал ли этот игрок
        const myPress = message.buttonPresses?.find(p => p.playerId === playerId);
        if (myPress) {
          setHasPressed(true);
        }
        break;
      case 'ROUND_ENDED':
        setGameState('ROUND_ENDED');
        if (message.winnerId) {
          setWinnerId(message.winnerId);
          setIsWinner(message.winnerId === playerId);
        }
        // Проверяем, нажимал ли этот игрок
        const myPressEnded = message.buttonPresses?.find(p => p.playerId === playerId);
        if (myPressEnded) {
          setHasPressed(true);
        }
        break;
      case 'ROUND_RESET':
        setGameState('WAITING');
        setIsWinner(false);
        setHasPressed(false);
        setWinnerId(null);
        break;
      case 'ERROR':
        setError(message.error);
        break;
      default:
        break;
    }
  }, [playerId]);

  useEffect(() => {
    const connect = async () => {
      try {
        await websocketService.connect();
        setConnected(true);
        
        // Подписка на персональные сообщения
        websocketService.subscribe('/user/queue/personal', handleMessage);
        
        // Подписка на события комнаты
        websocketService.subscribe(`/topic/room/${roomCode}`, handleMessage);
        
        // Присоединяемся к комнате
        websocketService.joinRoom(roomCode, playerId);
      } catch (err) {
        console.error('Connection error:', err);
        setError('Не удалось подключиться к серверу');
      }
    };

    connect();

    return () => {
      websocketService.disconnect();
    };
  }, [roomCode, playerId, handleMessage]);

  const handlePressButton = () => {
    if (gameState !== 'ACTIVE' || hasPressed) return;
    
    // Вибрация на мобильных устройствах
    if (navigator.vibrate) {
      navigator.vibrate(50);
    }
    
    websocketService.pressButton(roomCode, playerId);
    setHasPressed(true);
  };

  const getButtonState = () => {
    if (gameState === 'WAITING') return 'waiting';
    if (gameState === 'ACTIVE' && !hasPressed) return 'active';
    if (isWinner) return 'winner';
    if (hasPressed || winnerId) return 'loser';
    return 'waiting';
  };

  const getButtonText = () => {
    const state = getButtonState();
    switch (state) {
      case 'waiting': return 'Ожидайте...';
      case 'active': return 'Жми!';
      case 'winner': return '🎉 Первый!';
      case 'loser': return hasPressed ? 'Не успел...' : 'Поздно!';
      default: return 'Ожидайте...';
    }
  };

  if (error) {
    return (
      <div className="page flex items-center justify-center">
        <div className="card text-center">
          <h2 style={{ color: 'var(--secondary)' }}>Ошибка</h2>
          <p className="mt-2">{error}</p>
        </div>
      </div>
    );
  }

  if (!connected) {
    return (
      <div className="page flex items-center justify-center">
        <div className="text-center">
          <div className="spinner mb-2" style={{ margin: '0 auto' }}></div>
          <p>Подключение...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page" style={{ padding: '1rem' }}>
      {/* Player info */}
      <div className="flex items-center justify-between mb-2">
        {player && (
          <div className="flex items-center gap-2">
            <AvatarDisplay avatar={player.avatar} size="2rem" />
            <span style={{ fontWeight: '700' }}>{player.name}</span>
          </div>
        )}
        <div className="connection-status">
          <span className={`connection-dot ${connected ? 'connected' : 'disconnected'}`}></span>
        </div>
      </div>

      {/* Game State */}
      <div className="text-center mb-2">
        <span className={`game-state ${gameState.toLowerCase()}`}>
          {gameState === 'WAITING' && '⏳ Ожидание вопроса'}
          {gameState === 'ACTIVE' && '🔥 Отвечай!'}
          {gameState === 'ROUND_ENDED' && (isWinner ? '🏆 Ты первый!' : '⏰ Раунд окончен')}
        </span>
      </div>

      {/* Big Button */}
      <div className="big-button-container">
        <button 
          className={`big-button ${getButtonState()}`}
          onClick={handlePressButton}
          disabled={gameState !== 'ACTIVE' || hasPressed}
        >
          {getButtonText()}
        </button>
      </div>

      {/* Room info */}
      <div className="text-center" style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
        Комната: {roomCode}
      </div>
    </div>
  );
}

export default PlayerPage;


