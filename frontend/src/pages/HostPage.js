import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import websocketService from '../services/websocket';

const AVATARS = [
  "🦊", "🐼", "🦁", "🐯", "🐸", "🦉", "🦋", "🐙",
  "🦄", "🐲", "🦖", "🐳", "🦀", "🐝", "🦜", "🐨",
  "🐰", "🐻", "🦈", "🐺"
];

function HostPage() {
  const navigate = useNavigate();
  const [connected, setConnected] = useState(false);
  const [roomCode, setRoomCode] = useState(null);
  const [players, setPlayers] = useState([]);
  const [gameState, setGameState] = useState('WAITING');
  const [winner, setWinner] = useState(null);
  const [showAddPlayer, setShowAddPlayer] = useState(false);
  const [newPlayerName, setNewPlayerName] = useState('');
  const [selectedAvatar, setSelectedAvatar] = useState(AVATARS[0]);
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
        
        // Создаём комнату
        console.log('Creating room...');
        websocketService.createRoom();
      } catch (err) {
        console.error('Connection error:', err);
        setError('Не удалось подключиться к серверу');
      }
    };

    connect();

    return () => {
      websocketService.disconnect();
    };
  }, [handleMessage]);

  useEffect(() => {
    if (roomCode) {
      // Подписка на события комнаты
      websocketService.subscribe(`/topic/room/${roomCode}`, handleMessage);
    }
  }, [roomCode, handleMessage]);

  const handleAddPlayer = () => {
    if (!newPlayerName.trim() || !roomCode) return;
    
    console.log('Adding player to room:', roomCode, newPlayerName.trim(), selectedAvatar);
    websocketService.addPlayer(roomCode, newPlayerName.trim(), selectedAvatar);
    setNewPlayerName('');
    setSelectedAvatar(AVATARS[Math.floor(Math.random() * AVATARS.length)]);
    setShowAddPlayer(false);
  };

  const handleRemovePlayer = (playerId) => {
    websocketService.removePlayer(roomCode, playerId);
  };

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
          <div className="winner-avatar">{winner.avatar}</div>
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
        <button 
          className="btn btn-secondary"
          onClick={() => setShowAddPlayer(true)}
        >
          ➕ Добавить игрока
        </button>
      </div>

      {/* Players List */}
      <div className="card">
        <h2 className="mb-3">Игроки ({players.length}/20)</h2>
        
        {players.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">👥</div>
            <p>Пока нет игроков</p>
            <p>Нажмите "Добавить игрока" чтобы начать</p>
          </div>
        ) : (
          <div className="players-list">
            {players.map(player => (
              <div key={player.id} className="player-card">
                <div className="player-avatar">{player.avatar}</div>
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
                <button 
                  className="btn-remove"
                  onClick={() => handleRemovePlayer(player.id)}
                  title="Удалить игрока"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add Player Modal */}
      {showAddPlayer && (
        <div className="modal-overlay" onClick={() => setShowAddPlayer(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>Добавить игрока</h2>
            {!roomCode && (
              <div style={{ color: 'var(--warning)', marginBottom: '1rem', fontSize: '0.9rem' }}>
                ⏳ Ожидание создания комнаты...
              </div>
            )}
            <div className="form-group">
              <label>Имя игрока</label>
              <input 
                type="text"
                className="input w-full"
                placeholder="Введите имя..."
                value={newPlayerName}
                onChange={e => setNewPlayerName(e.target.value)}
                onKeyPress={e => e.key === 'Enter' && handleAddPlayer()}
                autoFocus
              />
            </div>

            <div className="form-group">
              <label>Аватар</label>
              <div className="avatar-grid">
                {AVATARS.map(avatar => (
                  <button
                    key={avatar}
                    className={`avatar-option ${selectedAvatar === avatar ? 'selected' : ''}`}
                    onClick={() => setSelectedAvatar(avatar)}
                  >
                    {avatar}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex gap-2 mt-3">
              <button 
                className="btn btn-primary flex-1"
                onClick={handleAddPlayer}
                disabled={!newPlayerName.trim() || !roomCode}
              >
                {!roomCode ? 'Ожидание комнаты...' : 'Добавить'}
              </button>
              <button 
                className="btn btn-secondary"
                onClick={() => setShowAddPlayer(false)}
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default HostPage;


