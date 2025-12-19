import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import websocketService from '../services/websocket';
import AvatarDisplay from '../components/AvatarDisplay';
import { getApiUrl } from '../utils/api';

function ScreenPage() {
  const { roomCode } = useParams();
  const [connected, setConnected] = useState(false);
  const [players, setPlayers] = useState([]);
  const [gameState, setGameState] = useState('WAITING');
  const [winner, setWinner] = useState(null);
  const [error, setError] = useState(null);

  const handleMessage = useCallback((message) => {
    console.log('Screen received message:', message);
    
    switch (message.type) {
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
        setPlayers(message.players || []);
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
    if (!roomCode) {
      setError('Код комнаты не указан');
      return;
    }

    // Загружаем начальное состояние через REST API
    const loadInitialState = async () => {
      try {
        const response = await fetch(`${getApiUrl()}/api/room/${roomCode}/state`);
        if (response.ok) {
          const data = await response.json();
          // Обрабатываем как ROOM_STATE сообщение
          handleMessage(data);
        } else if (response.status === 404) {
          setError('Комната не найдена');
        }
      } catch (err) {
        console.error('Error loading initial state:', err);
      }
    };

    const connect = async () => {
      try {
        await websocketService.connect();
        setConnected(true);
        
        // Загружаем начальное состояние
        await loadInitialState();
        
        // Подписка на события комнаты
        websocketService.subscribe(`/topic/room/${roomCode}`, handleMessage);
      } catch (err) {
        console.error('Connection error:', err);
        setError('Не удалось подключиться к серверу');
      }
    };

    connect();

    return () => {
      websocketService.unsubscribe(`/topic/room/${roomCode}`);
      // Не отключаем WebSocket полностью, так как он может использоваться другими компонентами
    };
  }, [roomCode, handleMessage]);

  // Группируем игроков по командам
  const groupPlayersByTeam = () => {
    const teams = {};
    players.forEach(player => {
      const teamId = player.teamId || 0;
      if (!teams[teamId]) {
        teams[teamId] = [];
      }
      teams[teamId].push(player);
    });
    return teams;
  };

  const teams = groupPlayersByTeam();
  
  // Названия команд
  const teamNames = {
    1: 'Ведьмачий ковеант',
    2: 'Тифлинги',
    3: 'Орда Братва',
    4: 'Лесной союз'
  };
  
  // Получаем команды в порядке: 1, 2, 3, 4 (для размещения по углам)
  const orderedTeams = [1, 2, 3, 4].filter(id => teams[id]).map(id => ({ id, players: teams[id] }));

  if (!connected) {
    return (
      <div className="page flex items-center justify-center">
        <div className="text-center">
          <div className="spinner mb-2" style={{ margin: '0 auto' }}></div>
          <p>Подключение к комнате {roomCode}...</p>
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

      {/* Room Code Display */}
      <div className="text-center mb-4">
        <p className="mb-1" style={{ color: 'var(--text-muted)' }}>Комната:</p>
        <div className="room-code">{roomCode}</div>
      </div>

      {/* Winner Display */}
      {winner && gameState === 'ROUND_ENDED' && (
        <div className="winner-display card mb-4">
          <div className="winner-avatar">
            <AvatarDisplay avatar={winner.avatar} size="16rem" />
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

      {/* Players List by Teams */}
      <div className="card">
        <h2 className="mb-3">Игроки ({players.length}/20)</h2>
        
        {players.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">👥</div>
            <p>Пока нет игроков</p>
            <p>Ожидание подключения игроков...</p>
          </div>
        ) : (
          <div className="teams-corners">
            {orderedTeams.map(({ id, players: teamPlayers }, index) => {
              // Определяем позицию команды по углам: 0-верх-левый, 1-верх-правый, 2-низ-левый, 3-низ-правый
              const cornerClass = index === 0 ? 'corner-top-left' : 
                                  index === 1 ? 'corner-top-right' : 
                                  index === 2 ? 'corner-bottom-left' : 
                                  'corner-bottom-right';
              
              return (
                <div key={id} className={`team-corner ${cornerClass}`}>
                  <h3 className="team-name">{teamNames[id] || `Команда ${id}`}</h3>
                  <div className="team-players">
                    {teamPlayers.map(player => {
                      // Определяем цвет рамки: темно-фиолетовый для оглушенных, зеленый для подключенных, серый для отключенных
                      let borderColor = '#888'; // По умолчанию серый (не подключен)
                      if (player.stunned) {
                        borderColor = '#6a0dad'; // Темно-фиолетовый для оглушенных
                      } else if (player.connected) {
                        borderColor = '#00ff88'; // Зеленый для подключенных
                      }
                      
                      return (
                        <div 
                          key={player.id} 
                          className="player-avatar-wrapper"
                          style={{
                            border: `4px solid ${borderColor}`,
                            borderRadius: '50%',
                            padding: '4px',
                            display: 'inline-block',
                            margin: '0.75rem'
                          }}
                        >
                          <AvatarDisplay avatar={player.avatar} size="6rem" />
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

    </div>
  );
}

export default ScreenPage;

