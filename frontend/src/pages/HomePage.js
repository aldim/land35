import React from 'react';
import { useNavigate } from 'react-router-dom';
import AvatarDisplay from '../components/AvatarDisplay';

function HomePage() {
  const navigate = useNavigate();
  const userId = localStorage.getItem('userId');
  const userRole = localStorage.getItem('userRole');

  // ProtectedRoute уже проверил авторизацию, но на всякий случай
  if (!userId) {
    return null; // ProtectedRoute сделает редирект
  }

  const isAdmin = userRole === 'ADMIN';

  return (
    <div className="page">
      <div className="flex flex-col items-center justify-center flex-1">
        <h1 className="mb-4">Quiz Battle</h1>
        <p className="text-center mb-4" style={{ color: 'var(--text-muted)', maxWidth: '500px' }}>
          Интерактивная игра для квизов. Ведущий создаёт комнату, 
          добавляет игроков, и кто первый нажмёт кнопку — тот и отвечает!
        </p>
        
        <div className="card mb-3" style={{ maxWidth: '400px', width: '100%' }}>
          <div className="flex items-center gap-2">
            <AvatarDisplay avatar={localStorage.getItem('userAvatar')} size="2rem" />
            <div>
              <div style={{ fontWeight: '600' }}>
                {localStorage.getItem('userFullName')}
              </div>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                {isAdmin ? '👑 Администратор' : '👤 Игрок'}
              </div>
            </div>
            <button
              className="btn btn-secondary ml-auto"
              style={{ padding: '0.3rem 0.8rem', fontSize: '0.85rem' }}
              onClick={() => {
                localStorage.clear();
                navigate('/login');
              }}
            >
              Выйти
            </button>
          </div>
        </div>
        
        <div className="flex flex-col gap-3 mt-4" style={{ width: '100%', maxWidth: '400px' }}>
          {isAdmin && (
            <>
              <button 
                className="btn btn-primary"
                onClick={() => navigate('/host')}
              >
                🎮 Продолжить игру
              </button>
              <button 
                className="btn btn-secondary"
                onClick={() => navigate('/host?new=true')}
              >
                ➕ Создать новую комнату
              </button>
            </>
          )}
          {!isAdmin && (
            <button 
              className="btn btn-primary"
              onClick={() => navigate('/join')}
            >
              👤 Подключиться к игре
            </button>
          )}
        </div>

        <div className="card mt-4" style={{ maxWidth: '400px' }}>
          <h2 className="mb-2">Как играть?</h2>
          <ol style={{ color: 'var(--text-muted)', lineHeight: '1.8' }}>
            <li>Ведущий создаёт комнату</li>
            <li>Ведущий добавляет игроков и даёт им ссылки</li>
            <li>Игроки открывают ссылки на телефонах</li>
            <li>Ведущий задаёт вопрос и нажимает "Старт"</li>
            <li>Кто первый нажмёт кнопку — отвечает!</li>
          </ol>
        </div>
      </div>
    </div>
  );
}

export default HomePage;


