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
  const [openMenuPlayerId, setOpenMenuPlayerId] = useState(null);

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

  const handleStunPlayer = (playerId) => {
    console.log('handleStunPlayer called:', { roomCode, playerId });
    if (roomCode) {
      console.log('Calling websocketService.stunPlayer');
      websocketService.stunPlayer(roomCode, playerId);
      setOpenMenuPlayerId(null); // Закрываем меню после действия
    } else {
      console.error('No roomCode available');
    }
  };

  const handleAvatarClick = (e, playerId) => {
    e.stopPropagation();
    // Открываем меню для этого игрока (переключаем)
    setOpenMenuPlayerId(openMenuPlayerId === playerId ? null : playerId);
  };

  // Закрываем меню при клике вне его
  useEffect(() => {
    if (!openMenuPlayerId) return;
    
    const handleClickOutside = (event) => {
      // Проверяем, был ли клик на меню или на аватар
      const clickedMenu = event.target.closest('.player-action-menu');
      const clickedAvatar = event.target.closest('.player-avatar-wrapper');
      
      // Закрываем меню только если клик был вне меню и не на аватаре
      if (!clickedMenu && !clickedAvatar) {
        setOpenMenuPlayerId(null);
      }
    };
    
    // Используем небольшую задержку, чтобы клик на кнопку успел обработаться
    const timeoutId = setTimeout(() => {
      document.addEventListener('mousedown', handleClickOutside);
    }, 10);
    
    return () => {
      clearTimeout(timeoutId);
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [openMenuPlayerId]);

  // Группируем игроков по командам
  const groupPlayersByTeam = () => {
    const teams = {};
    players.forEach(player => {
      const teamId = player.teamId || 0; // Если teamId отсутствует, считаем как команда 0
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

      {/* Players List by Teams */}
      <div className="card">
        <h2 className="mb-3">Игроки ({players.length}/20)</h2>
        
        {players.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">👥</div>
            <p>Пока нет игроков</p>
            <p>Игроки загружаются автоматически из базы данных</p>
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
                          style={{ position: 'relative', display: 'inline-block' }}
                        >
                          <div 
                            className="player-avatar-wrapper"
                            style={{
                              border: `4px solid ${borderColor}`,
                              borderRadius: '50%',
                              padding: '4px',
                              display: 'inline-block',
                              margin: '0.75rem',
                              cursor: 'pointer',
                              transition: 'transform 0.2s',
                              touchAction: 'manipulation' // Для лучшей работы на мобильных
                            }}
                            onClick={(e) => handleAvatarClick(e, player.id)}
                            onTouchEnd={(e) => {
                              e.preventDefault();
                              handleAvatarClick(e, player.id);
                            }}
                            title="Кликните для действий"
                          >
                            <AvatarDisplay avatar={player.avatar} size="6rem" />
                          </div>
                          
                          {/* Всплывающее меню */}
                          {openMenuPlayerId === player.id && (
                            <div 
                              className="player-action-menu"
                              style={{
                                position: 'absolute',
                                left: 'calc(100% + 10px)',
                                top: '50%',
                                transform: 'translateY(-50%)',
                                zIndex: 10000,
                                background: 'var(--card-bg)',
                                border: '1px solid var(--card-border)',
                                borderRadius: '8px',
                                padding: '0.5rem 0',
                                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.5)',
                                minWidth: '150px',
                                touchAction: 'manipulation',
                                whiteSpace: 'nowrap'
                              }}
                              onClick={(e) => {
                                e.stopPropagation();
                              }}
                              onTouchEnd={(e) => {
                                e.stopPropagation();
                              }}
                            >
                              <button
                                className="menu-item"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  console.log('Stun player clicked:', player.id, player.stunned);
                                  if (!player.stunned) {
                                    handleStunPlayer(player.id);
                                  }
                                }}
                                onTouchEnd={(e) => {
                                  e.stopPropagation();
                                  if (!player.stunned) {
                                    handleStunPlayer(player.id);
                                  }
                                }}
                                style={{
                                  width: '100%',
                                  padding: '0.75rem 1rem',
                                  background: 'transparent',
                                  border: 'none',
                                  color: player.stunned ? 'var(--text-muted)' : 'var(--text)',
                                  cursor: player.stunned ? 'not-allowed' : 'pointer',
                                  textAlign: 'left',
                                  fontSize: '1rem',
                                  transition: 'background 0.2s'
                                }}
                                onMouseEnter={(e) => {
                                  if (!player.stunned) {
                                    e.target.style.background = 'rgba(106, 13, 173, 0.2)';
                                  }
                                }}
                                onMouseLeave={(e) => {
                                  e.target.style.background = 'transparent';
                                }}
                                disabled={player.stunned}
                              >
                                {player.stunned ? '✓ Оглушен' : '⚡ Оглушить'}
                              </button>
                            </div>
                          )}
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

export default HostPage;


