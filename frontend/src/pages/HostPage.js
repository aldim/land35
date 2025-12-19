import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import websocketService from '../services/websocket';
import AvatarDisplay from '../components/AvatarDisplay';

function HostPage() {
  const navigate = useNavigate();
  const [connected, setConnected] = useState(false);
  const [roomCode, setRoomCode] = useState(null);
  const [players, setPlayers] = useState([]);
  const [gameState, setGameState] = useState('WAITING');
  const [winner, setWinner] = useState(null);
  const [error, setError] = useState(null);

  const handleMessage = useCallback((message) => {
    console.log('Received message:', message);
    
    switch (message.type) {
      case 'ROOM_CREATED':
        setRoomCode(message.roomCode);
        break;
      case 'PLAYER_JOINED':
        setPlayers(message.players || []);
        setGameState(message.gameState);
        break;
      case 'PLAYER_LEFT':
        setPlayers(message.players || []);
        break;
      case 'ROUND_STARTED':
        setGameState('ACTIVE');
        setWinner(null);
        break;
      case 'BUTTON_PRESSED':
        setGameState(message.gameState);
        if (message.winnerId) {
          setWinner({
            id: message.winnerId,
            name: message.winnerName,
            avatar: message.winnerAvatar
          });
        }
        break;
      case 'ROUND_ENDED':
        setGameState('ROUND_ENDED');
        if (message.winnerId) {
          setWinner({
            id: message.winnerId,
            name: message.winnerName,
            avatar: message.winnerAvatar
          });
        }
        break;
      case 'ROUND_RESET':
        setGameState('WAITING');
        setWinner(null);
        setPlayers(message.players || []);
        break;
      case 'ROOM_STATE':
        setPlayers(message.players || []);
        setGameState(message.gameState);
        if (message.winnerId) {
          setWinner({
            id: message.winnerId,
            name: message.winnerName,
            avatar: message.winnerAvatar
          });
        }
        break;
      case 'ERROR':
        setError(message.error);
        setTimeout(() => setError(null), 5000);
        break;
      default:
        break;
    }
  }, []);

  useEffect(() => {
    // Проверяем авторизацию
    const userId = localStorage.getItem('userId');
    const userRole = localStorage.getItem('userRole');
    
    if (!userId) {
      navigate('/login');
      return;
    }
    
    if (userRole !== 'ADMIN') {
      setError('Только администратор может создавать комнаты');
      setTimeout(() => navigate('/'), 3000);
      return;
    }
    
    const connect = async () => {
      try {
        await websocketService.connect();
        setConnected(true);
        
        // Подписка на персональные сообщения
        websocketService.subscribe('/user/queue/personal', (message) => {
          console.log('Personal message received:', message);
          handleMessage(message);
        });
        
        // Небольшая задержка чтобы подписка успела установиться
        await new Promise(resolve => setTimeout(resolve, 100));
        
        // Создаём комнату с userId
        console.log('Creating room...');
        websocketService.createRoom(userId);
      } catch (err) {
        console.error('Connection error:', err);
        setError('Не удалось подключиться к серверу');
      }
    };

    connect();

    return () => {
      websocketService.disconnect();
    };
  }, [handleMessage, navigate]);

  useEffect(() => {
    if (roomCode) {
      // Подписка на события комнаты
      websocketService.subscribe(`/topic/room/${roomCode}`, handleMessage);
    }
  }, [roomCode, handleMessage]);

  // Функционал ручного добавления/удаления игроков отключен
  // Все игроки загружаются автоматически из базы данных

  const handleStartRound = () => {
    websocketService.startRound(roomCode);
  };

  const handleResetRound = () => {
    websocketService.resetRound(roomCode);
  };

  const getPlayerUrl = (player) => {
    const baseUrl = window.location.origin;
    return `${baseUrl}/play/${roomCode}/${player.id}`;
  };

  const copyPlayerUrl = (player) => {
    navigator.clipboard.writeText(getPlayerUrl(player));
  };

  if (!connected) {
    return (
      <div className="page flex items-center justify-center">
        <div className="text-center">
          <div className="spinner mb-2" style={{ margin: '0 auto' }}></div>
          <p>Подключение к серверу...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      {error && (
        <div className="card mb-3" style={{ background: 'rgba(255, 51, 102, 0.2)', borderColor: 'var(--secondary)' }}>
          {error}
        </div>
      )}

      <div className="header">
        <div>
          <button className="btn btn-secondary" onClick={() => navigate('/')}>
            ← На главную
          </button>
        </div>
        <div className="connection-status">
          <span className={`connection-dot ${connected ? 'connected' : 'disconnected'}`}></span>
          {connected ? 'Подключено' : 'Отключено'}
        </div>
      </div>

      {/* Room Code Display */}
      {roomCode && (
        <div className="text-center mb-4">
          <p className="mb-1" style={{ color: 'var(--text-muted)' }}>Код комнаты:</p>
          <div className="room-code">{roomCode}</div>
        </div>
      )}

      {/* Winner Display */}
      {winner && gameState === 'ROUND_ENDED' && (
        <div className="winner-display card mb-4">
          <div className="winner-label">🎉 Первый нажал!</div>
          <div className="winner-avatar">
            <AvatarDisplay avatar={winner.avatar} size="8rem" />
          </div>
          <div className="winner-name">{winner.name}</div>
        </div>
      )}

      {/* Game State */}
      <div className="text-center mb-4">
        <span className={`game-state ${gameState.toLowerCase()}`}>
          {gameState === 'WAITING' && '⏳ Ожидание'}
          {gameState === 'ACTIVE' && '🔥 Раунд активен!'}
          {gameState === 'ROUND_ENDED' && '✅ Раунд завершён'}
        </span>
      </div>

      {/* Control Panel */}
      <div className="control-panel mb-4">
        <button 
          className="btn btn-primary"
          onClick={handleStartRound}
          disabled={gameState === 'ACTIVE' || players.length === 0}
        >
          ▶ Старт раунда
        </button>
        <button 
          className="btn btn-warning"
          onClick={handleResetRound}
          disabled={gameState === 'WAITING'}
        >
          🔄 Новый вопрос
        </button>
      </div>

      {/* Players List */}
      <div className="card">
        <h2 className="mb-3">Игроки ({players.length}/20)</h2>
        
        {players.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">👥</div>
            <p>Пока нет игроков</p>
            <p>Игроки загружаются автоматически из базы данных</p>
          </div>
        ) : (
          <div className="players-list">
            {players.map(player => (
              <div key={player.id} className="player-card">
                <div className="player-avatar">
                  <AvatarDisplay avatar={player.avatar} size="2.5rem" />
                </div>
                <div>
                  <div className="player-name">{player.name}</div>
                  <div className={`player-status ${player.connected ? 'connected' : ''}`}>
                    {player.connected ? '🟢 Онлайн' : '⚪ Не подключён'}
                  </div>
                  <button 
                    className="btn btn-secondary mt-1"
                    style={{ padding: '0.3rem 0.8rem', fontSize: '0.8rem' }}
                    onClick={() => copyPlayerUrl(player)}
                  >
                    📋 Копировать ссылку
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

    </div>
  );
}

export default HostPage;


