import React, { useState } from 'react';

// Динамически определяем URL API на основе текущего хоста
const getApiUrl = () => {
  const envUrl = process.env.REACT_APP_API_URL;
  if (envUrl && typeof envUrl === 'string' && envUrl.trim() !== '') {
    return envUrl;
  }
  // В Docker контейнере используем имя сервиса, иначе localhost
  const host = window.location.hostname;
  const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
  // Если фронтенд на порту 3000, бэкенд на 8080
  // Если фронтенд на порту 80 (nginx), бэкенд на 8080
  return `${protocol}//${host}:8080`;
};

// Функция для отображения аватара (изображение или эмодзи)
const AvatarDisplay = ({ avatar, size = '2.5rem' }) => {
  const [imageError, setImageError] = useState(false);
  
  if (!avatar) {
    return <span style={{ fontSize: size }}>👤</span>;
  }
  
  // Если аватар начинается с /avatars/, это путь к изображению
  if ((avatar.startsWith('/avatars/') || avatar.startsWith('avatars/')) && !imageError) {
    const imageUrl = avatar.startsWith('/') 
      ? `${getApiUrl()}${avatar}` 
      : `${getApiUrl()}/${avatar}`;
    
    const sizeNum = parseFloat(size) || 2.5;
    const sizePx = sizeNum * 16; // Конвертируем rem в px (1rem = 16px)
    
    return (
      <img 
        src={imageUrl} 
        alt="Avatar" 
        style={{ 
          width: `${sizePx}px`, 
          height: `${sizePx}px`, 
          borderRadius: '50%', 
          objectFit: 'cover',
          border: '2px solid var(--card-border)',
          display: 'block'
        }}
        onError={() => {
          // Если изображение не загрузилось, показываем эмодзи
          setImageError(true);
        }}
      />
    );
  }
  
  // Иначе показываем как эмодзи
  return <span style={{ fontSize: size }}>{avatar}</span>;
};

export default AvatarDisplay;

