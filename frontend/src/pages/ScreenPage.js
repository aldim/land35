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
  const [currentChapter, setCurrentChapter] = useState(null);
  const [currentPart, setCurrentPart] = useState(null);
  const [chapterNames, setChapterNames] = useState({});

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
        if (message.chapter !== undefined) {
          setCurrentChapter(message.chapter);
        }
        if (message.part !== undefined) {
          setCurrentPart(message.part);
        }
        break;
      case 'CHAPTER_UPDATED':
        if (message.chapter !== undefined) {
          setCurrentChapter(message.chapter);
        }
        if (message.part !== undefined) {
          setCurrentPart(message.part);
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

  // Загружаем названия глав с бэкенда
  useEffect(() => {
    const loadChapterNames = async () => {
      try {
        const response = await fetch(`${getApiUrl()}/api/chapters/names`);
        if (response.ok) {
          const data = await response.json();
          setChapterNames(data.chapters || {});
        }
      } catch (err) {
        console.error('Error loading chapter names:', err);
      }
    };
    
    loadChapterNames();
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

  // Получаем путь к картинке главы
  const getChapterImagePath = () => {
    if (currentChapter !== null && currentPart !== null) {
      // Используем API URL для получения изображения из ресурсов бэкенда
      const apiUrl = getApiUrl();
      return `${apiUrl}/api/chapters/${currentChapter}-${currentPart}.jpg`;
    }
    return null;
  };

  const chapterImagePath = getChapterImagePath();

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
    <div className="page" style={{ position: 'relative', overflow: 'hidden' }}>
      {error && (
        <div className="card mb-3" style={{ background: 'rgba(255, 51, 102, 0.2)', borderColor: 'var(--secondary)' }}>
          {error}
        </div>
      )}

      {/* Chapter Image - Full Screen Center Overlay */}
      {chapterImagePath && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          pointerEvents: 'none',
          backgroundColor: 'rgba(0, 0, 0, 0.3)' // Полупрозрачный фон для лучшей видимости
        }}>
          <img 
            src={chapterImagePath}
            alt={`Глава ${currentChapter}, Часть ${currentPart}`}
            style={{
              maxWidth: '90vw',
              maxHeight: '90vh',
              objectFit: 'contain',
              borderRadius: '8px',
              boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)'
            }}
            onError={(e) => {
              console.error('Failed to load chapter image:', chapterImagePath);
              e.target.style.display = 'none';
            }}
          />
        </div>
      )}

      {/* Room Code Display - Bottom of screen, transparent */}
      <div 
        className="text-center" 
        style={{ 
          position: 'fixed',
          bottom: '2rem',
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 1001,
          width: 'auto',
          maxWidth: '300px'
        }}
      >
        <p className="mb-1" style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Комната:</p>
        <div 
          className="room-code" 
          style={{
            background: 'transparent',
            border: 'none',
            padding: '0.5rem 1rem',
            fontSize: '2rem',
            display: 'inline-block',
            width: 'auto'
          }}
        >
          {roomCode}
        </div>
      </div>

      {/* Winner Display */}
      {winner && gameState === 'ROUND_ENDED' && (
        <div className="winner-display card mb-4" style={{ position: 'relative', zIndex: 1001 }}>
          <div className="winner-avatar">
            <AvatarDisplay avatar={winner.avatar} size="16rem" />
          </div>
          <div className="winner-name">{winner.name}</div>
        </div>
      )}

      {/* Game State */}
      <div className="text-center mb-4" style={{ position: 'relative', zIndex: 1001 }}>
        <span className={`game-state ${gameState.toLowerCase()}`}>
          {gameState === 'WAITING' && '⏳ Ожидание'}
          {gameState === 'ACTIVE' && '🔥 Раунд активен!'}
          {gameState === 'ROUND_ENDED' && '✅ Раунд завершён'}
        </span>
      </div>

      {/* Current Chapter Display */}
      {currentChapter !== null && currentPart !== null && (
        <div className="text-center mb-4" style={{ position: 'relative', zIndex: 1001 }}>
          <div className="card" style={{ 
            display: 'inline-block', 
            padding: '0.75rem 1.5rem',
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            color: 'var(--text)',
            fontSize: '3.6rem',
            fontWeight: 'bold'
          }}>
            {chapterNames[currentChapter] || `Глава ${currentChapter}`}, Часть {currentPart}
          </div>
        </div>
      )}

      {/* Players List by Teams - Full Screen Transparent Overlay */}
      {players.length > 0 && (
        <div 
          className="teams-corners" 
          style={{ 
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            zIndex: 1001,
            pointerEvents: 'none',
            padding: '2rem'
          }}
        >
          {orderedTeams.map(({ id, players: teamPlayers }, index) => {
            // Определяем позицию команды по углам: 0-верх-левый, 1-верх-правый, 2-низ-левый, 3-низ-правый
            const cornerClass = index === 0 ? 'corner-top-left' : 
                                index === 1 ? 'corner-top-right' : 
                                index === 2 ? 'corner-bottom-left' : 
                                'corner-bottom-right';
            
            return (
              <div key={id} className={`team-corner ${cornerClass}`}>
                <h3 
                  className="team-name" 
                  style={{ 
                    backgroundColor: 'rgba(0, 0, 0, 0.3)',
                    backdropFilter: 'blur(4px)'
                  }}
                >
                  {teamNames[id] || `Команда ${id}`}
                </h3>
                <div 
                  className="team-players"
                  style={{
                    background: 'transparent',
                    border: 'none',
                    padding: '0.5rem'
                  }}
                >
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
                          margin: '0.75rem',
                          backgroundColor: 'rgba(0, 0, 0, 0.2)',
                          backdropFilter: 'blur(2px)'
                        }}
                      >
                        <AvatarDisplay avatar={player.avatar} size="12rem" />
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
  );
}

export default ScreenPage;

