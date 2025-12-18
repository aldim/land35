import React from 'react';
import { useNavigate } from 'react-router-dom';

function HomePage() {
  const navigate = useNavigate();

  return (
    <div className="page">
      <div className="flex flex-col items-center justify-center flex-1">
        <h1 className="mb-4">Quiz Battle</h1>
        <p className="text-center mb-4" style={{ color: 'var(--text-muted)', maxWidth: '500px' }}>
          Интерактивная игра для квизов. Ведущий создаёт комнату, 
          добавляет игроков, и кто первый нажмёт кнопку — тот и отвечает!
        </p>
        
        <div className="flex gap-3 mt-4">
          <button 
            className="btn btn-primary"
            onClick={() => navigate('/host')}
          >
            🎮 Создать игру
          </button>
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


