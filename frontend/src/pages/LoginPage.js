import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AvatarDisplay from '../components/AvatarDisplay';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

function LoginPage() {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedUser, setSelectedUser] = useState(null);
  const [password, setPassword] = useState('password123'); // Стандартный пароль
  const [loggingIn, setLoggingIn] = useState(false);

  useEffect(() => {
    // Если пользователь уже авторизован, перенаправляем на главную
    const userId = localStorage.getItem('userId');
    if (userId) {
      const userRole = localStorage.getItem('userRole');
      if (userRole === 'ADMIN') {
        navigate('/host', { replace: true });
      } else {
        navigate('/join', { replace: true });
      }
      return;
    }

    // Загружаем список пользователей
    fetch(`${API_URL}/api/users`)
      .then(res => {
        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }
        return res.json();
      })
      .then(data => {
        console.log('Users loaded:', data);
        const usersList = data.users || [];
        console.log('Users list:', usersList);
        setUsers(usersList);
        setLoading(false);
      })
      .catch(err => {
        console.error('Error loading users:', err);
        setError('Не удалось загрузить список пользователей. Проверьте, что сервер запущен.');
        setLoading(false);
      });
  }, [navigate]);

  const handleLogin = async (user) => {
    setLoggingIn(true);
    setError(null);

    try {
      const response = await fetch(`${API_URL}/api/users/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: user.username,
          password: password
        })
      });

      const data = await response.json();

      if (response.ok) {
        // Сохраняем информацию о пользователе
        localStorage.setItem('userId', data.id);
        localStorage.setItem('username', data.username);
        localStorage.setItem('userFullName', data.fullName);
        localStorage.setItem('userNickname', data.nickname || data.fullName);
        localStorage.setItem('userAvatar', data.avatar || '👤');
        localStorage.setItem('userRole', data.role || 'PLAYER');
        
        // Перенаправляем в зависимости от роли
        if (data.role === 'ADMIN') {
          navigate('/host');
        } else {
          navigate('/join');
        }
      } else {
        setError(data.error || 'Неверный пароль');
      }
    } catch (err) {
      console.error('Login error:', err);
      setError('Ошибка при входе в систему');
    } finally {
      setLoggingIn(false);
    }
  };

  if (loading) {
    return (
      <div className="page flex items-center justify-center">
        <div className="text-center">
          <div className="spinner mb-2" style={{ margin: '0 auto' }}></div>
          <p>Загрузка пользователей...</p>
        </div>
      </div>
    );
  }

  if (error && users.length === 0) {
    return (
      <div className="page flex items-center justify-center">
        <div className="card text-center" style={{ maxWidth: '500px' }}>
          <h2 style={{ color: 'var(--secondary)' }}>Ошибка</h2>
          <p className="mt-2">{error}</p>
          <button className="btn btn-secondary mt-3" onClick={() => window.location.reload()}>
            Обновить
          </button>
        </div>
      </div>
    );
  }

  if (users.length === 0 && !loading) {
    return (
      <div className="page flex items-center justify-center">
        <div className="card text-center" style={{ maxWidth: '500px' }}>
          <h2>Нет пользователей</h2>
          <p className="mt-2" style={{ color: 'var(--text-muted)' }}>
            Пользователи не найдены. Проверьте подключение к серверу.
          </p>
          <button className="btn btn-secondary mt-3" onClick={() => window.location.reload()}>
            Обновить
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="flex flex-col items-center justify-center flex-1">
        <h1 className="mb-4">Вход в игру</h1>
        
        {error && (
          <div className="card mb-4" style={{ background: 'rgba(255, 51, 102, 0.2)', borderColor: 'var(--secondary)', maxWidth: '500px', width: '100%' }}>
            {error}
          </div>
        )}

        <div className="card" style={{ maxWidth: '500px', width: '100%' }}>
          <p className="mb-3" style={{ color: 'var(--text-muted)' }}>
            Выберите вашего игрока:
          </p>
          
          <div className="form-group mb-3">
            <label>Пароль (для всех игроков)</label>
            <input
              type="password"
              className="input w-full"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Введите пароль"
            />
            <small style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
              Стандартный пароль: password123
            </small>
          </div>

          {users.length === 0 ? (
            <div className="text-center" style={{ padding: '2rem', color: 'var(--text-muted)' }}>
              <p>Загрузка пользователей...</p>
            </div>
          ) : (
            <div className="users-grid" style={{ 
              display: 'grid', 
              gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))',
              gap: '1rem',
              marginTop: '1rem'
            }}>
              {users.map(user => {
              const isAdmin = user.role === 'ADMIN';
              return (
                <button
                  key={user.id}
                  className={`user-card ${selectedUser?.id === user.id ? 'selected' : ''}`}
                  onClick={() => setSelectedUser(user)}
                  disabled={loggingIn}
                  style={{
                    padding: '1rem',
                    border: '2px solid',
                    borderColor: selectedUser?.id === user.id 
                      ? 'var(--primary)' 
                      : isAdmin 
                        ? '#ffd700' 
                        : 'var(--border)',
                    borderRadius: '8px',
                    background: selectedUser?.id === user.id 
                      ? 'rgba(0, 123, 255, 0.1)' 
                      : isAdmin 
                        ? 'rgba(255, 215, 0, 0.1)' 
                        : 'transparent',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <AvatarDisplay avatar={user.avatar} />
                  </div>
                  <div style={{ 
                    fontSize: '0.9rem', 
                    fontWeight: '600',
                    textAlign: 'center',
                    wordBreak: 'break-word'
                  }}>
                    {user.nickname || user.fullName}
                  </div>
                  {isAdmin && (
                    <div style={{ 
                      fontSize: '0.75rem', 
                      color: '#ffd700',
                      fontWeight: '600'
                    }}>
                      👑 Админ
                    </div>
                  )}
                </button>
              );
            })}
            </div>
          )}

          <button
            className="btn btn-primary w-full mt-4"
            onClick={() => selectedUser && handleLogin(selectedUser)}
            disabled={!selectedUser || loggingIn}
          >
            {loggingIn ? 'Вход...' : 'Войти'}
          </button>

          <button
            className="btn btn-secondary w-full mt-2"
            onClick={() => navigate('/')}
          >
            Назад
          </button>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;

