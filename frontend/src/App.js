import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import HomePage from './pages/HomePage';
import HostPage from './pages/HostPage';
import PlayerPage from './pages/PlayerPage';
import LoginPage from './pages/LoginPage';
import JoinRoomPage from './pages/JoinRoomPage';
import './App.css';

// Компонент для редиректа на логин
function RequireAuth({ children }) {
  const userId = localStorage.getItem('userId');
  const location = useLocation();
  
  if (!userId) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  
  return children;
}

// Компонент для защиты маршрутов (обертка)
function ProtectedRoute({ children }) {
  return <RequireAuth>{children}</RequireAuth>;
}

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/play/:roomCode/:playerId" element={<PlayerPage />} />
          <Route path="/" element={
            <ProtectedRoute>
              <HomePage />
            </ProtectedRoute>
          } />
          <Route path="/join" element={
            <ProtectedRoute>
              <JoinRoomPage />
            </ProtectedRoute>
          } />
          <Route path="/host" element={
            <ProtectedRoute>
              <HostPage />
            </ProtectedRoute>
          } />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
