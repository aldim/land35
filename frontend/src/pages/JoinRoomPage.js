import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

function JoinRoomPage() {
  const navigate = useNavigate();
  const [roomCode, setRoomCode] = useState('');
  const [userId, setUserId] = useState(null);
  const [userName, setUserName] = useState('');
  const [error, setError] = useState(null);

  useEffect(() => {
    // Проверяем, вошел ли пользователь
    const storedUserId = localStorage.getItem('userId');
    const storedUserName = localStorage.getItem('userNickname') || localStorage.getItem('userFullName');
    
    if (!storedUserId) {
      // Если не вошел, перенаправляем на страницу входа
      navigate('/login');
      return;
    }
    
    setUserId(storedUserId);
    setUserName(storedUserName);
  }, [navigate]);

  const handleJoinRoom = () => {
    if (!roomCode.trim()) {
      setError('Введите код комнаты');
      return;
    }

    if (!userId) {
      setError('Ошибка: пользователь не найден');
      return;
    }

    // Перенаправляем на страницу игрока
    navigate(`/play/${roomCode.toUpperCase()}/${userId}`);
  };

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  if (!userId) {
    return (
      <div className="page flex items-center justify-center">
        <div className="text-center">
          <div className="spinner mb-2" style={{ margin: '0 auto' }}></div>
          <p>Загрузка...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="flex flex-col items-center justify-center flex-1">
        <h1 className="mb-2">Подключиться к игре</h1>
        
        <div className="card mb-3" style={{ maxWidth: '400px', width: '100%' }}>
          <div className="flex items-center gap-2 mb-3">
            <span style={{ fontSize: '2rem' }}>
              {localStorage.getItem('userAvatar') || '👤'}
            </span>
            <div>
              <div style={{ fontWeight: '600' }}>{userName}</div>
              <button
                className="btn btn-secondary"
                style={{ padding: '0.2rem 0.5rem', fontSize: '0.8rem', marginTop: '0.3rem' }}
                onClick={handleLogout}
              >
                Выйти
              </button>
            </div>
          </div>
        </div>

        {error && (
          <div className="card mb-3" style={{ 
            background: 'rgba(255, 51, 102, 0.2)', 
            borderColor: 'var(--secondary)',
            maxWidth: '400px',
            width: '100%'
          }}>
            {error}
          </div>
        )}

        <div className="card" style={{ maxWidth: '400px', width: '100%' }}>
          <div className="form-group">
            <label>Код комнаты</label>
            <input
              type="text"
              className="input w-full"
              value={roomCode}
              onChange={(e) => {
                setRoomCode(e.target.value.toUpperCase());
                setError(null);
              }}
              placeholder="Введите код комнаты"
              onKeyPress={(e) => e.key === 'Enter' && handleJoinRoom()}
              autoFocus
              style={{ 
                textTransform: 'uppercase',
                fontSize: '1.5rem',
                textAlign: 'center',
                letterSpacing: '0.2rem'
              }}
            />
          </div>

          <button
            className="btn btn-primary w-full mt-3"
            onClick={handleJoinRoom}
            disabled={!roomCode.trim()}
          >
            Подключиться
          </button>

          <button
            className="btn btn-secondary w-full mt-2"
            onClick={() => navigate('/')}
          >
            На главную
          </button>
        </div>

        <div className="card mt-4" style={{ maxWidth: '400px', width: '100%' }}>
          <h3 className="mb-2">Как подключиться?</h3>
          <ol style={{ color: 'var(--text-muted)', lineHeight: '1.8', textAlign: 'left' }}>
            <li>Попросите ведущего дать вам код комнаты</li>
            <li>Введите код в поле выше</li>
            <li>Нажмите "Подключиться"</li>
            <li>Дождитесь начала раунда и нажимайте кнопку!</li>
          </ol>
        </div>
      </div>
    </div>
  );
}

export default JoinRoomPage;

